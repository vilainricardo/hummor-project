package com.rb.multi.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * <p><b>EN:</b> Spring Boot entry point for this service.</p>
 * <p><b>PT-BR:</b> Ponto de entrada Spring Boot deste serviço.</p>
 */
@SpringBootApplication()
public class Application {

	/** EN: Bootstraps the application. PT-BR: Arranca a aplicação. */
	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
