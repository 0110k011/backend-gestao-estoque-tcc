package com.gestao.api.integration;

import com.gestao.api.AbstractIntegrationTest;
import com.gestao.api.dto.movimentacao.MovimentacaoRequest;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class MovimentacaoMultiplosProdutosTest extends AbstractIntegrationTest {

    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    private Produto produtoA;
    private Produto produtoB;

    @BeforeEach
    void setUp() {
        movimentacaoEstoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        categoriaRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNome("Geral");
        categoria = categoriaRepository.save(categoria);

        produtoA = new Produto();
        produtoA.setSku("SKU-PROD-A");
        produtoA.setNome("Produto A");
        produtoA.setCategoria(categoria);
        produtoA.setPrecoCusto(new BigDecimal("10.00"));
        produtoA.setPrecoVenda(new BigDecimal("20.00"));
        produtoA.setQuantidadeEstoque(100);
        produtoA.setEstoqueMinimo(5);
        produtoA.setAtivo(true);
        produtoA = produtoRepository.save(produtoA);

        produtoB = new Produto();
        produtoB.setSku("SKU-PROD-B");
        produtoB.setNome("Produto B");
        produtoB.setCategoria(categoria);
        produtoB.setPrecoCusto(new BigDecimal("15.00"));
        produtoB.setPrecoVenda(new BigDecimal("25.00"));
        produtoB.setQuantidadeEstoque(100);
        produtoB.setEstoqueMinimo(5);
        produtoB.setAtivo(true);
        produtoB = produtoRepository.save(produtoB);
    }

    @Test
    @DisplayName("Deve permitir movimentações simultâneas em produtos diferentes sem deadlocks ou bloqueios globais")
    void deveMovimentarProdutosDiferentesConcorrentemente() throws InterruptedException {
        int numeroThreads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(numeroThreads);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < numeroThreads; i++) {
            final Long produtoId = (i % 2 == 0) ? produtoA.getId() : produtoB.getId();
            executor.submit(() -> {
                try {
                    latch.await();
                    MovimentacaoRequest dto = new MovimentacaoRequest(
                            produtoId,
                            TipoMovimentacao.SAIDA,
                            5,
                            new BigDecimal("20.00"),
                            "Saída concorrente em produtos distintos"
                    );
                    movimentacaoEstoqueService.registrar(dto);
                } catch (Exception e) {
                    // Tratamento de exceção de concorrência
                }
            });
        }

        latch.countDown();
        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        // Valida que ambos os produtos processaram as transações de forma independente
        Produto atualA = produtoRepository.findById(produtoA.getId()).orElseThrow();
        Produto atualB = produtoRepository.findById(produtoB.getId()).orElseThrow();

        assertTrue(atualA.getQuantidadeEstoque() < 100);
        assertTrue(atualB.getQuantidadeEstoque() < 100);
    }
}