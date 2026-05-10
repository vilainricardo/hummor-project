package com.rb.multi.agent.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * EN: Request body for password grant used by {@code POST /api/v1/auth/token}. PT-BR: Pedido de emissão de token.
 */
public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}
