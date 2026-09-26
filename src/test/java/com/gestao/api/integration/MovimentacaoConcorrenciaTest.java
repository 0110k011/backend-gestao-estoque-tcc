package com.gestao.api.integration;

import com.gestao.api.AbstractIntegrationTest;
import com.gestao.api.dto.movimentacao.MovimentacaoRequest;
import com.gestao.api.exception.EstoqueInsuficienteException;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

class MovimentacaoConcorrenciaTest extends AbstractIntegrationTest {

    private static final int SALDO_INICIAL = 10;
    private static final int REQUISICOES_SIMULTANEAS = 20;
    private static final int QUANTIDADE_POR_SAIDA = 1;

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
        produto.setSku("PROD-CONC-001");
        produto.setNome("Produto Concorrência");
        produto.setPrecoCusto(new BigDecimal("10.00"));
        produto.setPrecoVenda(new BigDecimal("20.00"));
        produto.setQuantidadeEstoque(SALDO_INICIAL);
        produto.setEstoqueMinimo(0);
        produto.setAtivo(true);
        produto.setCategoria(categoria);
        produto = produtoRepository.save(produto);
    }

    @Test
    @DisplayName("Deve impedir lost update em 20 saídas simultâneas sobre saldo 10")
    void deveImpedirPerdaDeAtualizacaoEmSaidasConcorrentes() throws Exception {

        ExecutorService executor = Executors.newFixedThreadPool(REQUISICOES_SIMULTANEAS);
        CountDownLatch largada = new CountDownLatch(1);
        CountDownLatch termino = new CountDownLatch(REQUISICOES_SIMULTANEAS);

        List<Future<Boolean>> resultados = new ArrayList<>();
        Long produtoId = produto.getId();

        for (int i = 0; i < REQUISICOES_SIMULTANEAS; i++) {
            resultados.add(executor.submit(() -> {
                largada.await(); // todas aguardam o mesmo sinal
                try {
                    MovimentacaoRequest request = new MovimentacaoRequest(
                            produtoId,
                            TipoMovimentacao.SAIDA,
                            QUANTIDADE_POR_SAIDA,
                            new BigDecimal("20.00"),
                            "Saída concorrente"
                    );
                    service.registrar(request);
                    return true;
                } catch (EstoqueInsuficienteException e) {
                    return false;
                } finally {
                    termino.countDown();
                }
            }));
        }

        // dispara todas simultaneamente
        largada.countDown();
        termino.await(30, TimeUnit.SECONDS);
        executor.shutdown();

        // Contagem de sucessos e falhas
        long sucessos = 0;
        long falhas = 0;
        for (Future<Boolean> r : resultados) {
            if (r.get()) sucessos++;
            else falhas++;
        }

        // Asserções
        assertThat(sucessos)
                .as("Exatamente %d saídas devem ser bem-sucedidas", SALDO_INICIAL)
                .isEqualTo(SALDO_INICIAL);

        assertThat(falhas)
                .as("Exatamente %d saídas devem ser rejeitadas", REQUISICOES_SIMULTANEAS - SALDO_INICIAL)
                .isEqualTo(REQUISICOES_SIMULTANEAS - SALDO_INICIAL);

        Produto atualizado = produtoRepository.findById(produtoId).orElseThrow();
        assertThat(atualizado.getQuantidadeEstoque())
                .as("Saldo final deve ser exatamente zero")
                .isZero();

        long totalMovimentacoes = movimentacaoRepository.countByProdutoId(produtoId);
        assertThat(totalMovimentacoes)
                .as("Histórico deve conter exatamente %d movimentações", SALDO_INICIAL)
                .isEqualTo(SALDO_INICIAL);
    }
}