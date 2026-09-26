package com.gestao.api.service;

import com.gestao.api.AbstractIntegrationTest;
import com.gestao.api.dto.movimentacao.MovimentacaoRequest;
import com.gestao.api.dto.movimentacao.MovimentacaoResponse;
import com.gestao.api.model.Categoria;
import com.gestao.api.model.Produto;
import com.gestao.api.model.TipoMovimentacao;
import com.gestao.api.repository.CategoriaRepository;
import com.gestao.api.repository.MovimentacaoEstoqueRepository;
import com.gestao.api.repository.ProdutoRepository;
import com.gestao.api.service.MovimentacaoEstoqueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MovimentacaoEstoqueIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MovimentacaoEstoqueService service;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    private Produto produto;

    @BeforeEach
    void setUp() {
        movimentacaoRepository.deleteAll();
        produtoRepository.deleteAll();
        categoriaRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNome("Geral");
        categoria.setAtivo(true);
        categoria = categoriaRepository.save(categoria);

        produto = new Produto();
        produto.setSku("PROD-INT-001");
        produto.setNome("Produto Integração");
        produto.setPrecoCusto(new BigDecimal("10.00"));
        produto.setPrecoVenda(new BigDecimal("20.00"));
        produto.setQuantidadeEstoque(10);
        produto.setEstoqueMinimo(2);
        produto.setAtivo(true);
        produto.setCategoria(categoria);
        produto = produtoRepository.save(produto);
    }

    @Test
    @DisplayName("Deve registrar saída e persistir saldo + histórico corretamente")
    void deveRegistrarSaidaComSucesso() {
        MovimentacaoRequest request = new MovimentacaoRequest(
                produto.getId(), TipoMovimentacao.SAIDA, 3,
                new BigDecimal("20.00"), "Venda teste"
        );

        MovimentacaoResponse response = service.registrar(request);

        assertThat(response.id()).isNotNull();
        assertThat(response.tipo()).isEqualTo(TipoMovimentacao.SAIDA);

        Produto atualizado = produtoRepository.findById(produto.getId()).orElseThrow();
        assertThat(atualizado.getQuantidadeEstoque()).isEqualTo(7);

        assertThat(movimentacaoRepository.countByProdutoId(produto.getId())).isEqualTo(1);
    }
}