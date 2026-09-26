package com.gestao.api.dto.categoria;

public record CategoriaResponse(
        Long id,
        String nome,
        String descricao,
        Boolean ativo
) {}