package com.rb.multi.agent.dto;

/**
 * <p><b>EN:</b> FR-019 style trend summary for one scale kind (mood or sleep): half-period averages, overall mean,
 * criticality code, and localized message.</p>
 * <p><b>PT-BR:</b> Resumo de tendência FR-019 por escala (humor/sono) com médias e criticidade.</p>
 */
public record TrendIndicatorResponse(
		String scaleKind,
		double firstHalfAverage,
		double secondHalfAverage,
		double periodWeightedAverage,
		String criticality,
		String message) {
}
