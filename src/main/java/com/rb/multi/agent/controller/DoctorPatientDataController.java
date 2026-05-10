package com.rb.multi.agent.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.rb.multi.agent.dto.PatientScaleHistoryResponse;
import com.rb.multi.agent.dto.ScaleEntryResponse;
import com.rb.multi.agent.service.DoctorPatientScaleDataService;

/**
 * <p><b>EN:</b> Clinician read API for patient mood/sleep; constrained by roster membership, FR-004 consent start, and FR-003 status.</p>
 * <p><b>PT-BR:</b> API de leitura médico–paciente para humor/sono; limitada por lista, início de partilha (FR-004) e estado (FR-003).</p>
 */
@RestController
@RequestMapping("/api/v1/doctors/{doctorId}/patients/{patientId}")
public class DoctorPatientDataController {

	private final DoctorPatientScaleDataService doctorPatientScaleDataService;

	public DoctorPatientDataController(DoctorPatientScaleDataService doctorPatientScaleDataService) {
		this.doctorPatientScaleDataService = doctorPatientScaleDataService;
	}

	/**
	 * EN: {@code from}/{@code to} optional ISO-8601 instants; omitted → full permitted history from
	 * {@link com.rb.multi.agent.entity.DoctorPatientAssociation#getAccessStartDate()} through “now”, clamped to consent.
	 */
	@GetMapping("/mood-entries")
	public List<ScaleEntryResponse> listMood(
			@PathVariable UUID doctorId,
			@PathVariable UUID patientId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		return doctorPatientScaleDataService.listMoodEntries(doctorId, patientId, from, to);
	}

	/** EN: Same window rules as {@link #listMood}. PT-BR: Mesmas regras de janela que {@link #listMood}. */
	@GetMapping("/sleep-entries")
	public List<ScaleEntryResponse> listSleep(
			@PathVariable UUID doctorId,
			@PathVariable UUID patientId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		return doctorPatientScaleDataService.listSleepEntries(doctorId, patientId, from, to);
	}

	/** EN: Mood and sleep together plus the effective query bounds in the JSON body. PT-BR: Humor e sono com limites efectivos no JSON. */
	@GetMapping("/scale-history")
	public PatientScaleHistoryResponse getScaleHistory(
			@PathVariable UUID doctorId,
			@PathVariable UUID patientId,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
		return doctorPatientScaleDataService.getScaleHistory(doctorId, patientId, from, to);
	}
}
