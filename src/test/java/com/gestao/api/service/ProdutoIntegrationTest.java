package com.gestao.api.service;

import com.gestao.api.AbstractIntegrationTest;
import com.gestao.api.dto.produto.ProdutoRequest;
import com.gestao.api.exception.SkuDuplicadoException;
import com.gestao.api.model.Categoria;
import com.gestao.api.repository.CategoriaRepository;
import com.gestao.api.repository.MovimentacaoEstoqueRepository;
import com.gestao.api.repository.ProdutoRepository;
import com.gestao.api.service.ProdutoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProdutoIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProdutoService produtoService;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;

    private Categoria categoriaPadrao;

    @BeforeEach
    void setUp() {
        // IMPORTANTE: Limpar as dependências filhas (movimentações) antes dos pais (produtos)
        movimentacaoEstoqueRepository.deleteAll();
        produtoRepository.deleteAll();
        categoriaRepository.deleteAll();

        categoriaPadrao = new Categoria();
        categoriaPadrao.setNome("Eletrônicos de Teste");
        categoriaPadrao.setDescricao("Categoria para testes de integração");
        categoriaPadrao.setAtivo(true);
        categoriaPadrao = categoriaRepository.save(categoriaPadrao);
    }

    @Test
    @DisplayName("Deve persistir um novo produto com sucesso no PostgreSQL real")
    void devePersistirProdutoComSucesso() {
        ProdutoRequest dto = new ProdutoRequest(
                "SKU-TESTE-001",
                "Smartphone Teste",
                "Descrição do smartphone de teste",
                new BigDecimal("1200.00"),
                new BigDecimal("1500.00"),
                10,
                categoriaPadrao.getId()
        );

        var produtoCriado = produtoService.criar(dto);

        assertNotNull(produtoCriado.id());
        assertEquals("SKU-TESTE-001", produtoCriado.sku());
        assertEquals(0, produtoCriado.quantidadeEstoque());
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar persistir produto com SKU duplicado (UQ_produto_sku)")
    void deveLancarExcecaoParaSkuDuplicado() {
        ProdutoRequest dto1 = new ProdutoRequest(
                "SKU-DUPLICADO",
                "Produto A",
                "Descrição A",
                new BigDecimal("100.00"),
                new BigDecimal("150.00"),
                5,
                categoriaPadrao.getId()
        );
        produtoService.criar(dto1);

        ProdutoRequest dto2 = new ProdutoRequest(
                "SKU-DUPLICADO",
                "Produto B",
                "Descrição B",
                new BigDecimal("200.00"),
                new BigDecimal("250.00"),
                5,
                categoriaPadrao.getId()
        );

        assertThrows(SkuDuplicadoException.class, () -> {
            produtoService.criar(dto2);
        });
    }

}