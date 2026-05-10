package com.rb.multi.agent.service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.rb.multi.agent.entity.MoodEntry;
import com.rb.multi.agent.entity.SleepEntry;

/**
 * <p><b>EN:</b> Splits the requested instant window into two halves, compares mean scale scores (FR-018-style trend),
 * and maps outcome to BOM / ALERTA / PERIGO per product rules (FR-019).</p>
 * <p><b>PT-BR:</b> Divide o intervalo ao meio; compara médias das metades; classifica criticidade (FR-019).</p>
 */
public final class ScalePeriodTrendCalculator {

	private static final double EPS = 1e-6;

	private ScalePeriodTrendCalculator() {
	}

	/** EN: Longitudinal mood entries. PT-BR: Registos de humor no tempo. */
	public static Optional<TrendComputation> computeMood(List<MoodEntry> entries, Instant fromInclusive, Instant toInclusive) {
		if (fromInclusive.isAfter(toInclusive)) {
			return Optional.empty();
		}
		Instant mid = midpoint(fromInclusive, toInclusive);
		List<Integer> first = new ArrayList<>();
		List<Integer> second = new ArrayList<>();
		for (MoodEntry e : entries) {
			Instant t = e.getCreatedAt();
			if (t.isBefore(fromInclusive) || t.isAfter(toInclusive)) {
				continue;
			}
			if (t.isBefore(mid)) {
				first.add(e.getValue());
			} else {
				second.add(e.getValue());
			}
		}
		return computeFromHalves(first, second);
	}

	/** EN: Sleep rows anchored by {@link SleepEntry#getRecordedOn()} start-of-day UTC. PT-BR: Sono por dia civil UTC. */
	public static Optional<TrendComputation> computeSleep(List<SleepEntry> entries, Instant fromInclusive, Instant toInclusive) {
		if (fromInclusive.isAfter(toInclusive)) {
			return Optional.empty();
		}
		Instant mid = midpoint(fromInclusive, toInclusive);
		List<Integer> first = new ArrayList<>();
		List<Integer> second = new ArrayList<>();
		for (SleepEntry e : entries) {
			Instant dayStart = e.getRecordedOn().atStartOfDay(ZoneOffset.UTC).toInstant();
			if (dayStart.isBefore(fromInclusive) || dayStart.isAfter(toInclusive)) {
				continue;
			}
			if (dayStart.isBefore(mid)) {
				first.add(e.getValue());
			} else {
				second.add(e.getValue());
			}
		}
		return computeFromHalves(first, second);
	}

	static Instant midpoint(Instant fromInclusive, Instant toInclusive) {
		Duration full = Duration.between(fromInclusive, toInclusive);
		return fromInclusive.plus(full.dividedBy(2));
	}

	static Optional<TrendComputation> computeFromHalves(List<Integer> firstHalfValues, List<Integer> secondHalfValues) {
		if (firstHalfValues.isEmpty() || secondHalfValues.isEmpty()) {
			return Optional.empty();
		}
		double avgFirst = averageInts(firstHalfValues);
		double avgSecond = averageInts(secondHalfValues);
		List<Integer> all = new ArrayList<>(firstHalfValues);
		all.addAll(secondHalfValues);
		double periodAvg = averageInts(all);
		TrendDirection dir = direction(avgFirst, avgSecond);
		TrendCriticality tier = classify(dir, avgSecond);
		return Optional.of(new TrendComputation(avgFirst, avgSecond, periodAvg, tier));
	}

	private static double averageInts(List<Integer> values) {
		return values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
	}

	static TrendDirection direction(double first, double second) {
		if (second < first - EPS) {
			return TrendDirection.FALLS;
		}
		if (second > first + EPS) {
			return TrendDirection.RISES;
		}
		return TrendDirection.STABLE;
	}

	/**
	 * EN: Recent half average ({@code avgSecond}) combined with trend. PT-BR: Média da segunda metade + tendência.
	 */
	static TrendCriticality classify(TrendDirection dir, double avgSecond) {
		return switch (dir) {
			case FALLS -> avgSecond > 6.0 ? TrendCriticality.ALERTA : TrendCriticality.PERIGO;
			case STABLE -> {
				if (avgSecond > 8.0) {
					yield TrendCriticality.BOM;
				}
				if (avgSecond >= 6.0 && avgSecond <= 8.0) {
					yield TrendCriticality.ALERTA;
				}
				yield TrendCriticality.PERIGO;
			}
			case RISES -> avgSecond >= 6.0 ? TrendCriticality.BOM : TrendCriticality.PERIGO;
		};
	}

	public enum TrendDirection {
		FALLS,
		STABLE,
		RISES
	}

	public enum TrendCriticality {
		BOM,
		ALERTA,
		PERIGO
	}

	public record TrendComputation(
			double firstHalfAverage,
			double secondHalfAverage,
			double periodWeightedAverage,
			TrendCriticality criticality) {
	}
}
