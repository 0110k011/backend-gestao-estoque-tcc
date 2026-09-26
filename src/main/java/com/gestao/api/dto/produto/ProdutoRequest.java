package com.gestao.api.dto.produto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProdutoRequest(

        @NotBlank(message = "SKU é obrigatório")
        @Size(max = 50, message = "SKU deve ter no máximo 50 caracteres")
        @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU deve conter apenas letras maiúsculas, números e hífen")
        String sku,

        @NotBlank(message = "Nome é obrigatório")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nome,

        String descricao,

        @NotNull(message = "Preço de custo é obrigatório")
        @PositiveOrZero(message = "Preço de custo não pode ser negativo")
        BigDecimal precoCusto,

        @NotNull(message = "Preço de venda é obrigatório")
        @Positive(message = "Preço de venda deve ser maior que zero")
        BigDecimal precoVenda,

        @NotNull(message = "Estoque mínimo é obrigatório")
        @PositiveOrZero(message = "Estoque mínimo não pode ser negativo")
        Integer estoqueMinimo,

        @NotNull(message = "Categoria é obrigatória")
        Long categoriaId
) {}