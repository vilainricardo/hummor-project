package com.rb.multi.agent.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * <p>Negociação de idioma via cabeçalho HTTP {@code Accept-Language}. Se ausente ou vazio → <b>inglês Estados Unidos</b>
 * ({@code en-US}).</p>
 *
 * <p><b>Padrão uniforme: idioma + região</b> (ficheiros {@code messages_&lt;lang&gt;_&lt;region&gt;.properties}):</p>
 * <ul>
 *   <li><b>Inglês</b> — {@code en-US} (omissão), {@code en-GB}</li>
 *   <li><b>Português</b> — {@code pt-BR}, {@code pt-PT}</li>
 *   <li><b>Espanhol</b> — {@code es-ES}, {@code es-MX}</li>
 * </ul>
 *
 * <p>Ficheiros: {@code messages_&lt;lang&gt;_&lt;region&gt;.properties} (ex.: {@code messages_en_US}, {@code messages_pt_BR},
 * {@code messages_pt_PT}, {@code messages_es_ES}, {@code messages_es_MX}, {@code messages_en_GB}) e
 * {@code ValidationMessages_*} com o mesmo sufixo.</p>
 */
@Configuration
public class I18nConfig {

	@Bean
	public LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setDefaultLocale(Locale.forLanguageTag("en-US"));
		resolver.setSupportedLocales(
				List.of(
						Locale.forLanguageTag("en-US"),
						Locale.forLanguageTag("en-GB"),
						Locale.forLanguageTag("pt-BR"),
						Locale.forLanguageTag("pt-PT"),
						Locale.forLanguageTag("es-ES"),
						Locale.forLanguageTag("es-MX")));
		return resolver;
	}
}
