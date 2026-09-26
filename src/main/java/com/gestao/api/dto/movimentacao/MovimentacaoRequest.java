package com.gestao.api.dto.movimentacao;

import com.gestao.api.model.TipoMovimentacao;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record MovimentacaoRequest(

        @NotNull(message = "Produto é obrigatório")
        Long produtoId,

        @NotNull(message = "Tipo de movimentação é obrigatório")
        TipoMovimentacao tipo,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que zero")
        Integer quantidade,

        @NotNull(message = "Valor unitário é obrigatório")
        @PositiveOrZero(message = "Valor unitário não pode ser negativo")
        BigDecimal valorUnitario,

        @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres")
        String observacao
) {}