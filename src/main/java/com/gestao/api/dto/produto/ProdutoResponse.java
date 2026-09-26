package com.gestao.api.dto.produto;

import java.math.BigDecimal;

public record ProdutoResponse(
        Long id,
        String sku,
        String nome,
        String descricao,
        BigDecimal precoCusto,
        BigDecimal precoVenda,
        Integer quantidadeEstoque,
        Integer estoqueMinimo,
        Boolean ativo,
        Long categoriaId,
        String categoriaNome,
        Boolean abaixoDoMinimo
) {}