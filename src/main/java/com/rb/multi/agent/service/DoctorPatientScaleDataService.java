package com.rb.multi.agent.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.rb.multi.agent.dto.PatientScaleHistoryResponse;
import com.rb.multi.agent.dto.TrendIndicatorResponse;
import com.rb.multi.agent.dto.ScaleEntryResponse;
import com.rb.multi.agent.entity.Doctor;
import com.rb.multi.agent.entity.DoctorPatientAssociation;
import com.rb.multi.agent.entity.DoctorPatientKey;
import com.rb.multi.agent.constants.DoctorPatientLinkStatus;
import com.rb.multi.agent.entity.User;
import com.rb.multi.agent.exception.DoctorPatientLinkNotFoundException;
import com.rb.multi.agent.exception.NotDoctorProfileException;
import com.rb.multi.agent.exception.PatientDataAccessRevokedException;
import com.rb.multi.agent.exception.UserNotFoundException;
import com.rb.multi.agent.repository.DoctorPatientAssociationRepository;
import com.rb.multi.agent.entity.MoodEntry;
import com.rb.multi.agent.entity.SleepEntry;
import com.rb.multi.agent.repository.MoodEntryRepository;
import com.rb.multi.agent.repository.SleepEntryRepository;
import com.rb.multi.agent.repository.UserRepository;

import com.rb.multi.agent.service.ScalePeriodTrendCalculator.TrendComputation;
import com.rb.multi.agent.service.ScalePeriodTrendCalculator.TrendDirection;

/**
 * <p><b>EN:</b> Read-only access for doctors to patient mood/sleep series, clamped to FR-004 consent start and blocked when
 * FR-003 status is {@link DoctorPatientLinkStatus#UNLINKED_WITHOUT_ACCESS}.</p>
 * <p><b>PT-BR:</b> Leitura para médicos de séries humor/sono, limitadas ao consentimento (FR-004) e bloqueadas se sem acesso (FR-003).</p>
 */
@Service
public class DoctorPatientScaleDataService {

	private final UserRepository userRepository;
	private final DoctorPatientAssociationRepository associationRepository;
	private final MoodEntryRepository moodEntryRepository;
	private final SleepEntryRepository sleepEntryRepository;
	private final MessageSource messageSource;

	public DoctorPatientScaleDataService(
			UserRepository userRepository,
			DoctorPatientAssociationRepository associationRepository,
			MoodEntryRepository moodEntryRepository,
			SleepEntryRepository sleepEntryRepository,
			MessageSource messageSource) {
		this.userRepository = userRepository;
		this.associationRepository = associationRepository;
		this.moodEntryRepository = moodEntryRepository;
		this.sleepEntryRepository = sleepEntryRepository;
		this.messageSource = messageSource;
	}

	private DoctorPatientAssociation requireReadableAssociation(UUID doctorId, UUID patientId) {
		User doctorUser = userRepository.findById(doctorId).orElseThrow(() -> UserNotFoundException.byId(doctorId));
		if (!(doctorUser instanceof Doctor)) {
			throw new NotDoctorProfileException(doctorId);
		}
		DoctorPatientAssociation assoc =
				associationRepository
						.findById(new DoctorPatientKey(doctorId, patientId))
						.orElseThrow(() -> new DoctorPatientLinkNotFoundException(doctorId, patientId));
		if (assoc.getStatus() == DoctorPatientLinkStatus.UNLINKED_WITHOUT_ACCESS) {
			throw new PatientDataAccessRevokedException(doctorId, patientId);
		}
		return assoc;
	}

	/**
	 * EN: Effective window: never before patient’s {@link DoctorPatientAssociation#getAccessStartDate()} (UTC midnight); optional
	 * {@code from}/{@code to} narrow further; if {@code to} omitted, upper bound is current instant.
	 */
	static EffectiveWindow effectiveWindow(DoctorPatientAssociation assoc, Instant requestFrom, Instant requestTo) {
		Instant consentStart = assoc.getAccessStartDate().atStartOfDay(ZoneOffset.UTC).toInstant();
		Instant from = requestFrom != null ? requestFrom : consentStart;
		if (from.isBefore(consentStart)) {
			from = consentStart;
		}
		Instant to = requestTo != null ? requestTo : Instant.now();
		return new EffectiveWindow(from, to);
	}

	@Transactional(readOnly = true)
	public List<ScaleEntryResponse> listMoodEntries(UUID doctorId, UUID patientId, Instant from, Instant to) {
		DoctorPatientAssociation assoc = requireReadableAssociation(doctorId, patientId);
		EffectiveWindow w = effectiveWindow(assoc, from, to);
		if (w.fromInclusive().isAfter(w.toInclusive())) {
			return List.of();
		}
		return moodEntryRepository
				.findByPatient_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
						patientId, w.fromInclusive(), w.toInclusive())
				.stream()
				.map(ScaleEntryResponse::fromMood)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ScaleEntryResponse> listSleepEntries(UUID doctorId, UUID patientId, Instant from, Instant to) {
		DoctorPatientAssociation assoc = requireReadableAssociation(doctorId, patientId);
		EffectiveWindow w = effectiveWindow(assoc, from, to);
		if (w.fromInclusive().isAfter(w.toInclusive())) {
			return List.of();
		}
		LocalDate fromDay = w.fromInclusive().atZone(ZoneOffset.UTC).toLocalDate();
		LocalDate toDay = w.toInclusive().atZone(ZoneOffset.UTC).toLocalDate();
		if (fromDay.isAfter(toDay)) {
			return List.of();
		}
		return sleepEntryRepository
				.findByPatient_IdAndRecordedOnBetweenOrderByRecordedOnDesc(patientId, fromDay, toDay)
				.stream()
				.map(ScaleEntryResponse::fromSleep)
				.toList();
	}

	@Transactional(readOnly = true)
	public PatientScaleHistoryResponse getScaleHistory(UUID doctorId, UUID patientId, Instant from, Instant to) {
		DoctorPatientAssociation assoc = requireReadableAssociation(doctorId, patientId);
		EffectiveWindow w = effectiveWindow(assoc, from, to);
		if (w.fromInclusive().isAfter(w.toInclusive())) {
			return new PatientScaleHistoryResponse(
					assoc.getAccessStartDate(), w.fromInclusive(), w.toInclusive(), List.of(), List.of(), List.of());
		}
		List<MoodEntry> moodEntities =
				moodEntryRepository.findByPatient_IdAndCreatedAtGreaterThanEqualAndCreatedAtLessThanEqualOrderByCreatedAtDesc(
						patientId, w.fromInclusive(), w.toInclusive());
		List<ScaleEntryResponse> mood = moodEntities.stream().map(ScaleEntryResponse::fromMood).toList();
		LocalDate fromDay = w.fromInclusive().atZone(ZoneOffset.UTC).toLocalDate();
		LocalDate toDay = w.toInclusive().atZone(ZoneOffset.UTC).toLocalDate();
		List<SleepEntry> sleepEntities =
				fromDay.isAfter(toDay)
						? List.of()
						: sleepEntryRepository.findByPatient_IdAndRecordedOnBetweenOrderByRecordedOnDesc(patientId, fromDay, toDay);
		List<ScaleEntryResponse> sleep = sleepEntities.stream().map(ScaleEntryResponse::fromSleep).toList();
		List<TrendIndicatorResponse> trends = buildTrendIndicators(moodEntities, sleepEntities, w);
		return new PatientScaleHistoryResponse(
				assoc.getAccessStartDate(), w.fromInclusive(), w.toInclusive(), mood, sleep, trends);
	}

	private List<TrendIndicatorResponse> buildTrendIndicators(
			List<MoodEntry> moodEntries, List<SleepEntry> sleepEntries, EffectiveWindow w) {
		List<TrendIndicatorResponse> list = new ArrayList<>();
		Locale locale = LocaleContextHolder.getLocale();
		ScalePeriodTrendCalculator.computeMood(moodEntries, w.fromInclusive(), w.toInclusive())
				.ifPresent(c -> list.add(toTrendResponse("MOOD", c, locale)));
		ScalePeriodTrendCalculator.computeSleep(sleepEntries, w.fromInclusive(), w.toInclusive())
				.ifPresent(c -> list.add(toTrendResponse("SLEEP", c, locale)));
		return List.copyOf(list);
	}

	private TrendIndicatorResponse toTrendResponse(String scaleKind, TrendComputation comp, Locale locale) {
		TrendDirection dir =
				ScalePeriodTrendCalculator.direction(comp.firstHalfAverage(), comp.secondHalfAverage());
		String ruleKey =
				"trend.rule."
						+ dir.name()
						+ "."
						+ comp.criticality().name();
		String scaleLabel =
				messageSource.getMessage(
						"TrendIndicator.scale." + scaleKind, null, "TrendIndicator.scale." + scaleKind, locale);
		String message =
				messageSource.getMessage(ruleKey, new Object[] { scaleLabel, comp.secondHalfAverage() }, ruleKey, locale);
		return new TrendIndicatorResponse(
				scaleKind,
				round3(comp.firstHalfAverage()),
				round3(comp.secondHalfAverage()),
				round3(comp.periodWeightedAverage()),
				comp.criticality().name(),
				message);
	}

	private static double round3(double v) {
		return Math.round(v * 1000.0) / 1000.0;
	}

	/** EN: Internal result of consent + request clamping. PT-BR: Resultado interno do clamp consentimento + pedido. */
	record EffectiveWindow(Instant fromInclusive, Instant toInclusive) {
	}
}
