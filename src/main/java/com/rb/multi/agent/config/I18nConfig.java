package com.rb.multi.agent.config;

import java.util.List;
import java.util.Locale;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * <p><b>EN:</b> Negotiates locales from {@code Accept-Language}; default pt-BR, supports pt/pt-BR/en.</p>
 * <p><b>PT-BR:</b> Negocia locale via {@code Accept-Language}; padrão pt-BR; suporta pt/pt-BR/en.</p>
 */
@Configuration
public class I18nConfig {

	/**
	 * <p><b>EN:</b> {@link AcceptHeaderLocaleResolver} bound to MVC request locale context.</p>
	 * <p><b>PT-BR:</b> {@link AcceptHeaderLocaleResolver} ligado ao contexto de locale dos pedidos MVC.</p>
	 */
	@Bean
	public LocaleResolver localeResolver() {
		AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
		resolver.setDefaultLocale(Locale.forLanguageTag("pt-BR"));
		resolver.setSupportedLocales(List.of(
				Locale.forLanguageTag("pt-BR"),
				Locale.forLanguageTag("pt"),
				Locale.ENGLISH));
		return resolver;
	}
}
