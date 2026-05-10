package com.rb.multi.agent.constants;

/**
 * <p><b>EN:</b> Groups catalogue tags for API filtering/search only (not a clinical classification).</p>
 * <p><b>PT-BR:</b> Agrupa tags do catálogo apenas para filtros/pesquisa na API (não é classificação clínica).</p>
 * <p><b>EN:</b> Concrete labels live on each {@link com.rb.multi.agent.entity.Tag}; a tag may appear in several categories.</p>
 * <p><b>PT-BR:</b> Rótulos vivem em cada {@link com.rb.multi.agent.entity.Tag}; uma etiqueta pode ter várias categorias.</p>
 */
public enum TagCategory {

	/**
	 * <p><b>EN:</b> Anxiety, worry, subjective panic-like states.</p>
	 * <p><b>PT-BR:</b> Ansiedade, preocupação, estado semelhante a pânico subjetivo, etc.</p>
	 */
	ANXIETY,
	/**
	 * <p><b>EN:</b> Severe despair, acute emotional distress crises.</p>
	 * <p><b>PT-BR:</b> Desespero forte, crises agudas de angústia emocional.</p>
	 */
	DISTRESS_AND_DESPAIR,
	/**
	 * <p><b>EN:</b> Persistently low mood when catalogue vocabulary expresses it.</p>
	 * <p><b>PT-BR:</b> Humor prolongadamente baixo quando o catálogo o descreve assim.</p>
	 */
	LOW_MOOD,
	/**
	 * <p><b>EN:</b> Self-harm / suicide-related wording — always informational (SRS).</p>
	 * <p><b>PT-BR:</b> Textos sobre autolesão / ideação suicida — sempre informativos (SRS).</p>
	 */
	SUICIDE_AND_SELF_HARM_IDEATION,
	/**
	 * <p><b>EN:</b> Patient-reported somatic sensations (e.g. chest pain) — app does not diagnose.</p>
	 * <p><b>PT-BR:</b> Sintomas corporais relatados (ex.: dor no peito) — sem diagnóstico automático pela aplicação.</p>
	 */
	SOMATIC_CARDIOVASCULAR,
	/**
	 * <p><b>EN:</b> Sleep/rest patterns captured as tags.</p>
	 * <p><b>PT-BR:</b> Rotina sono/descanso modelada como etiquetas.</p>
	 */
	SLEEP,
	/**
	 * <p><b>EN:</b> Perceived stress, overload, and life transitions (adjustment).</p>
	 * <p><b>PT-BR:</b> Stress percebido, sobrecarga e transições de vida (ajuste).</p>
	 */
	STRESS_AND_ADJUSTMENT,
	/**
	 * <p><b>EN:</b> Trauma- or major-stressor-related wording (SRS; not diagnostic).</p>
	 * <p><b>PT-BR:</b> Relacionado com trauma ou grandes stressores (SRS; não diagnóstico).</p>
	 */
	TRAUMA_AND_STRESSOR_RELATED,
	/**
	 * <p><b>EN:</b> Non-cardiac somatic complaints (patient-reported; no diagnosis).</p>
	 * <p><b>PT-BR:</b> Queixas somáticas não cardíacas (autorrelato; sem diagnóstico).</p>
	 */
	SOMATIC_NON_CARDIAC,
	/**
	 * <p><b>EN:</b> Substance use / craving concerns (informational only).</p>
	 * <p><b>PT-BR:</b> Uso de substâncias / cravings (apenas informativo).</p>
	 */
	SUBSTANCE_USE,
	/**
	 * <p><b>EN:</b> Items not belonging to another axis.</p>
	 * <p><b>PT-BR:</b> Etiquetas que não se encaixam nos outros eixos.</p>
	 */
	OTHER
}
