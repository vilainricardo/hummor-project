package com.rb.multi.agent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class TechAgent {

    @Value("${openai.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public String responder(String pergunta) {

        String prompt = """
            Você é um engenheiro backend especialista em Java e Spring Boot.
            Seja direto, técnico e inclua exemplos de código.

            Pergunta: """ + pergunta;

        Map<String, Object> request = Map.of(
                "model", "gpt-4.1-mini",
                "input", prompt
        );

        var headers = new org.springframework.http.HttpHeaders();
        headers.setBearerAuth(apiKey);
        headers.set("Content-Type", "application/json");

        var entity = new org.springframework.http.HttpEntity<>(request, headers);

        var response = restTemplate.postForObject(
                "https://api.openai.com/v1/responses",
                entity,
                Map.class
        );

        return response.toString();
    }
}