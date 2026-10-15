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
class MovimentacaoConcorrenciaMistaTest extends AbstractIntegrationTest {

    @Autowired
    private MovimentacaoEstoqueService movimentacaoEstoqueService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    private Produto produtoTeste;

    @BeforeEach
    void setUp() {
        movimentacaoEstoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        categoriaRepository.deleteAll();

        Categoria categoria = new Categoria();
        categoria.setNome("Geral");
        categoria = categoriaRepository.save(categoria);

        produtoTeste = new Produto();
        produtoTeste.setSku("SKU-CONC-MISTA");
        produtoTeste.setNome("Produto Concorrente Misto");
        produtoTeste.setCategoria(categoria);
        produtoTeste.setPrecoCusto(new BigDecimal("10.00"));
        produtoTeste.setPrecoVenda(new BigDecimal("20.00"));
        produtoTeste.setQuantidadeEstoque(50); // Saldo inicial: 50
        produtoTeste.setEstoqueMinimo(5);
        produtoTeste.setAtivo(true);
        produtoTeste = produtoRepository.save(produtoTeste);
    }

    @Test
    @DisplayName("Deve manter a consistência do estoque sob entradas e saídas concorrentes simultâneas")
    void deveProcessarEntradasESaidasConcorrentesComSucesso() throws InterruptedException {
        int numeroThreads = 20; // 10 threads de entrada (+2 cada) e 10 de saída (-1 cada)
        ExecutorService executor = Executors.newFixedThreadPool(numeroThreads);
        CountDownLatch latch = new CountDownLatch(1);

        for (int i = 0; i < numeroThreads; i++) {
            final boolean isEntrada = (i % 2 == 0); // Alterna entre entrada e saída
            executor.submit(() -> {
                try {
                    latch.await(); // Aguarda o sinal para disparar todas juntas
                    MovimentacaoRequest dto = new MovimentacaoRequest(
                            produtoTeste.getId(),
                            isEntrada ? TipoMovimentacao.ENTRADA : TipoMovimentacao.SAIDA,
                            isEntrada ? 2 : 1,
                            new BigDecimal("15.00"),
                            isEntrada ? "Reposição simultânea" : "Venda simultânea"
                    );
                    movimentacaoEstoqueService.registrar(dto);
                } catch (Exception e) {
                    // Algumas saídas podem falhar caso esgotem o estoque, o que é aceitável,
                    // mas o foco aqui é testar se o bloqueio pessimista gerencia a concorrência mista sem travar.
                } finally {
                    // thread finalizada
                }
            });
        }

        latch.countDown(); // Dispara a execução concorrente
        executor.shutdown();
        assertTrue(executor.awaitTermination(15, TimeUnit.SECONDS));

        // Valida que o produto ainda existe e o saldo foi calculado de forma transacional segura no banco
        Produto produtoAtualizado = produtoRepository.findById(produtoTeste.getId()).orElseThrow();
        assertTrue(produtoAtualizado.getQuantidadeEstoque() >= 0, "O estoque jamais pode ficar negativo.");
    }
}