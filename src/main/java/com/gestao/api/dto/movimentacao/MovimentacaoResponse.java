package com.gestao.api.dto.movimentacao;

import com.gestao.api.model.TipoMovimentacao;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MovimentacaoResponse(
        Long id,
        Long produtoId,
        String produtoNome,
        TipoMovimentacao tipo,
        Integer quantidade,
        BigDecimal valorUnitario,
        LocalDateTime dataHora,
        String observacao
) {}