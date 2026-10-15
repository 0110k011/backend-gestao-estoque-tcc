package com.gestao.api.service;

import com.gestao.api.AbstractIntegrationTest;
import com.gestao.api.model.Categoria;
import com.gestao.api.model.Produto;
import com.gestao.api.repository.CategoriaRepository;
import com.gestao.api.repository.ProdutoRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CategoriaIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private ProdutoRepository produtoRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Deve impedir a exclusão de categoria que possui produtos vinculados (ON DELETE RESTRICT)")
    void deveImpedirExclusaoDeCategoriaComProdutosVinculados() {
        // Arrange
        Categoria categoria = new Categoria();
        categoria.setNome("Categoria Protegida");
        categoria.setDescricao("Teste de integridade referencial");
        categoria.setAtivo(true);
        categoria = categoriaRepository.save(categoria);

        Produto produto = new Produto();
        produto.setSku("SKU-CAT-01");
        produto.setNome("Item Vinculado");
        produto.setDescricao("Item de teste");
        produto.setCategoria(categoria);
        produto.setPrecoCusto(new BigDecimal("50.00"));
        produto.setPrecoVenda(new BigDecimal("80.00"));
        produto.setQuantidadeEstoque(15);
        produto.setEstoqueMinimo(5);
        produto.setAtivo(true);
        produtoRepository.save(produto);

        // Sincroniza o contexto para garantir que os dados estão gravados no SGBD
        entityManager.flush();
        entityManager.clear();

        Long categoriaId = categoria.getId();

        // Act & Assert
        // Captura qualquer exceção de banco/runtime e valida que ocorreu o bloqueio de chave estrangeira
        Exception exception = assertThrows(Exception.class, () -> {
            categoriaRepository.deleteById(categoriaId);
            entityManager.flush(); // Força o envio imediato do SQL para o PostgreSQL
        });

        // Garante que a mensagem de erro veio da restrição de chave estrangeira do banco
        String mensagemErro = exception.getMessage() + (exception.getCause() != null ? " " + exception.getCause().getMessage() : "");
        assertTrue(mensagemErro.contains("violates foreign key constraint") || mensagemErro.contains("fk_produto_categoria"));
    }
}