package com.gestao.api.dto.auth;

import com.gestao.api.model.Perfil;
import jakarta.validation.constraints.*;

public record RegisterRequest(

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150)
        String nome,

        @NotBlank(message = "E-mail é obrigatório")
        @Email(message = "E-mail inválido")
        @Size(max = 150)
        String email,

        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String senha,

        @NotNull(message = "Perfil é obrigatório")
        Perfil perfil
) {}