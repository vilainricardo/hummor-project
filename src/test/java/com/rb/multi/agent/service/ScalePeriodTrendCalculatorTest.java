package com.rb.multi.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.rb.multi.agent.service.ScalePeriodTrendCalculator.TrendCriticality;
import com.rb.multi.agent.service.ScalePeriodTrendCalculator.TrendDirection;

class ScalePeriodTrendCalculatorTest {

	@Nested
	@DisplayName("direction")
	class DirectionTests {

		@Test
		void fallsWhenSecondIsLower() {
			assertThat(ScalePeriodTrendCalculator.direction(8.0, 6.0)).isEqualTo(TrendDirection.FALLS);
		}

		@Test
		void risesWhenSecondIsHigher() {
			assertThat(ScalePeriodTrendCalculator.direction(5.0, 9.0)).isEqualTo(TrendDirection.RISES);
		}

		@Test
		void stableWithinEpsilon() {
			assertThat(ScalePeriodTrendCalculator.direction(7.0, 7.0)).isEqualTo(TrendDirection.STABLE);
		}
	}

	@Nested
	@DisplayName("classify FR-019")
	class ClassifyTests {

		@Test
		void fallsAboveSix_alert() {
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.FALLS, 6.5)).isEqualTo(TrendCriticality.ALERTA);
		}

		@Test
		void fallsAtOrBelowSix_danger() {
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.FALLS, 6.0)).isEqualTo(TrendCriticality.PERIGO);
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.FALLS, 4.0)).isEqualTo(TrendCriticality.PERIGO);
		}

		@Test
		void stable_aboveEight_good() {
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.STABLE, 8.1)).isEqualTo(TrendCriticality.BOM);
		}

		@Test
		void stable_betweenSixAndEight_alert() {
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.STABLE, 6.0)).isEqualTo(TrendCriticality.ALERTA);
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.STABLE, 7.5)).isEqualTo(TrendCriticality.ALERTA);
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.STABLE, 8.0)).isEqualTo(TrendCriticality.ALERTA);
		}

		@Test
		void stable_belowSix_danger() {
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.STABLE, 5.9)).isEqualTo(TrendCriticality.PERIGO);
		}

		@Test
		void rises_atOrAboveSix_good() {
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.RISES, 6.0)).isEqualTo(TrendCriticality.BOM);
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.RISES, 9.0)).isEqualTo(TrendCriticality.BOM);
		}

		@Test
		void rises_belowSix_danger() {
			assertThat(ScalePeriodTrendCalculator.classify(TrendDirection.RISES, 5.0)).isEqualTo(TrendCriticality.PERIGO);
		}
	}

	@Nested
	@DisplayName("computeFromHalves")
	class HalvesTests {

		@Test
		void emptyHalves_yieldEmpty() {
			assertThat(ScalePeriodTrendCalculator.computeFromHalves(List.of(), List.of(1))).isEmpty();
			assertThat(ScalePeriodTrendCalculator.computeFromHalves(List.of(1), List.of())).isEmpty();
		}

		@Test
		void combinesPeriodAverage() {
			var r =
					ScalePeriodTrendCalculator.computeFromHalves(List.of(10, 10), List.of(0, 0))
							.orElseThrow();
			assertThat(r.firstHalfAverage()).isEqualTo(10.0);
			assertThat(r.secondHalfAverage()).isEqualTo(0.0);
			assertThat(r.periodWeightedAverage()).isEqualTo(5.0);
			assertThat(r.criticality()).isEqualTo(TrendCriticality.PERIGO);
		}
	}
}
