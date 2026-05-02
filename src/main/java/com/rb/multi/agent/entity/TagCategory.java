package com.rb.multi.agent.entity;

/**
 * <p><b>EN:</b> Groups catalogue tags for API filtering/search only (not a clinical classification).</p>
 * <p><b>PT-BR:</b> Agrupa tags do catálogo apenas para filtros/pesquisa na API (não é classificação clínica).</p>
 * <p><b>EN:</b> Concrete labels live in {@link Tag#getName()}; categories narrow result sets.</p>
 * <p><b>PT-BR:</b> Rótulos concretos estão em {@link Tag#getName()}; categorias reduzem o conjunto de resultados.</p>
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
	 * <p><b>EN:</b> Items not belonging to another axis.</p>
	 * <p><b>PT-BR:</b> Etiquetas que não se encaixam nos outros eixos.</p>
	 */
	OTHER
}
