package com.gestao.api.service;

import com.gestao.api.dto.produto.ProdutoRequest;
import com.gestao.api.dto.produto.ProdutoResponse;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.exception.SkuDuplicadoException;
import com.gestao.api.model.Categoria;
import com.gestao.api.model.Produto;
import com.gestao.api.repository.CategoriaRepository;
import com.gestao.api.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProdutoServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private ProdutoService service;

    private Categoria categoria;
    private Produto produtoExistente;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        categoria = new Categoria();
        categoria.setId(1L);
        categoria.setNome("Bebidas");
        categoria.setDescricao("Bebidas em geral");
        categoria.setAtivo(true);

        produtoExistente = new Produto();
        produtoExistente.setId(10L);
        produtoExistente.setSku("BEB-001");
        produtoExistente.setNome("Coca-Cola 350ml");
        produtoExistente.setDescricao("Refrigerante lata");
        produtoExistente.setPrecoCusto(new BigDecimal("3.00"));
        produtoExistente.setPrecoVenda(new BigDecimal("5.50"));
        produtoExistente.setQuantidadeEstoque(100);
        produtoExistente.setEstoqueMinimo(10);
        produtoExistente.setAtivo(true);
        produtoExistente.setCategoria(categoria);

        pageable = PageRequest.of(0, 20);
    }

    // ============================================================
    // listar()
    // ============================================================

    @Nested
    @DisplayName("listar()")
    class Listar {

        @Test
        @DisplayName("Deve retornar página vazia quando não houver produtos ativos")
        void deveRetornarPaginaVazia() {
            when(produtoRepository.findByAtivoTrue(pageable))
                    .thenReturn(Page.empty());

            Page<ProdutoResponse> resultado = service.listar(pageable);

            assertThat(resultado).isEmpty();
            verify(produtoRepository).findByAtivoTrue(pageable);
        }

        @Test
        @DisplayName("Deve retornar página com produtos convertidos em DTO")
        void deveRetornarPaginaDeProdutos() {
            Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente), pageable, 1);

            when(produtoRepository.findByAtivoTrue(pageable))
                    .thenReturn(pagina);

            Page<ProdutoResponse> resultado = service.listar(pageable);

            assertThat(resultado.getTotalElements()).isEqualTo(1);
            assertThat(resultado.getContent()).hasSize(1);

            ProdutoResponse response = resultado.getContent().get(0);
            assertThat(response.id()).isEqualTo(10L);
            assertThat(response.sku()).isEqualTo("BEB-001");
            assertThat(response.nome()).isEqualTo("Coca-Cola 350ml");
            assertThat(response.categoriaId()).isEqualTo(1L);
            assertThat(response.categoriaNome()).isEqualTo("Bebidas");
            assertThat(response.abaixoDoMinimo()).isFalse();
        }

        @Test
        @DisplayName("Deve marcar abaixoDoMinimo=true quando estoque <= mínimo")
        void deveMarcarAbaixoDoMinimo() {
            produtoExistente.setQuantidadeEstoque(5);
            produtoExistente.setEstoqueMinimo(10);

            Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente), pageable, 1);
            when(produtoRepository.findByAtivoTrue(pageable)).thenReturn(pagina);

            Page<ProdutoResponse> resultado = service.listar(pageable);

            assertThat(resultado.getContent().get(0).abaixoDoMinimo()).isTrue();
        }
    }

    // ============================================================
    // buscarPorNome()
    // ============================================================

    @Nested
    @DisplayName("buscarPorNome()")
    class BuscarPorNome {

        @Test
        @DisplayName("Deve retornar produtos filtrados por nome")
        void deveRetornarProdutosFiltrados() {
            Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente), pageable, 1);

            when(produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase("Coca", pageable))
                    .thenReturn(pagina);

            Page<ProdutoResponse> resultado = service.buscarPorNome("Coca", pageable);

            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).nome()).isEqualTo("Coca-Cola 350ml");
        }

        @Test
        @DisplayName("Deve retornar página vazia quando não houver correspondências")
        void deveRetornarPaginaVazia() {
            when(produtoRepository.findByAtivoTrueAndNomeContainingIgnoreCase("Inexistente", pageable))
                    .thenReturn(Page.empty());

            Page<ProdutoResponse> resultado = service.buscarPorNome("Inexistente", pageable);

            assertThat(resultado).isEmpty();
        }
    }

    // ============================================================
    // buscarPorCategoria()
    // ============================================================

    @Nested
    @DisplayName("buscarPorCategoria()")
    class BuscarPorCategoria {

        @Test
        @DisplayName("Deve retornar produtos de uma categoria específica")
        void deveRetornarProdutosDaCategoria() {
            Page<Produto> pagina = new PageImpl<>(List.of(produtoExistente), pageable, 1);

            when(produtoRepository.findByAtivoTrueAndCategoriaId(1L, pageable))
                    .thenReturn(pagina);

            Page<ProdutoResponse> resultado = service.buscarPorCategoria(1L, pageable);

            assertThat(resultado.getContent()).hasSize(1);
            assertThat(resultado.getContent().get(0).categoriaId()).isEqualTo(1L);
        }
    }

    // ============================================================
    // buscarPorId()
    // ============================================================

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar produto quando ID existir")
        void deveRetornarProduto() {
            when(produtoRepository.findById(10L))
                    .thenReturn(Optional.of(produtoExistente));

            ProdutoResponse resultado = service.buscarPorId(10L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(10L);
            assertThat(resultado.sku()).isEqualTo("BEB-001");
            assertThat(resultado.nome()).isEqualTo("Coca-Cola 350ml");
            assertThat(resultado.precoCusto()).isEqualByComparingTo("3.00");
            assertThat(resultado.precoVenda()).isEqualByComparingTo("5.50");
            assertThat(resultado.quantidadeEstoque()).isEqualTo(100);
            assertThat(resultado.estoqueMinimo()).isEqualTo(10);
            assertThat(resultado.ativo()).isTrue();
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando ID não existir")
        void deveLancarExcecaoQuandoIdNaoExistir() {
            when(produtoRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");
        }
    }

    // ============================================================
    // criar()
    // ============================================================

    @Nested
    @DisplayName("criar()")
    class Criar {

        private ProdutoRequest requestValido() {
            return new ProdutoRequest(
                    "BEB-002",
                    "Pepsi 350ml",
                    "Refrigerante lata",
                    new BigDecimal("3.00"),
                    new BigDecimal("5.50"),
                    10,
                    1L
            );
        }

        @Test
        @DisplayName("Deve criar produto com sucesso")
        void deveCriarComSucesso() {
            ProdutoRequest request = requestValido();

            when(produtoRepository.existsBySku("BEB-002")).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> {
                Produto p = inv.getArgument(0);
                p.setId(20L);
                return p;
            });

            ProdutoResponse resultado = service.criar(request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(20L);
            assertThat(resultado.sku()).isEqualTo("BEB-002");
            assertThat(resultado.nome()).isEqualTo("Pepsi 350ml");
            assertThat(resultado.quantidadeEstoque()).isZero();
            assertThat(resultado.ativo()).isTrue();
            assertThat(resultado.categoriaId()).isEqualTo(1L);

            verify(produtoRepository).save(any(Produto.class));
        }

        @Test
        @DisplayName("Deve lançar SkuDuplicadoException quando SKU já existir")
        void deveLancarExcecaoQuandoSkuDuplicado() {
            ProdutoRequest request = requestValido();

            when(produtoRepository.existsBySku("BEB-002")).thenReturn(true);

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOf(SkuDuplicadoException.class)
                    .hasMessageContaining("BEB-002");

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando categoria não existir")
        void deveLancarExcecaoQuandoCategoriaNaoExistir() {
            ProdutoRequest request = requestValido();

            when(produtoRepository.existsBySku("BEB-002")).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Categoria não encontrada");

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando preço de venda < preço de custo")
        void deveLancarExcecaoQuandoPrecoVendaMenorQueCusto() {
            ProdutoRequest request = new ProdutoRequest(
                    "BEB-002",
                    "Pepsi 350ml",
                    "Refrigerante lata",
                    new BigDecimal("10.00"),
                    new BigDecimal("5.00"),
                    10,
                    1L
            );

            when(produtoRepository.existsBySku("BEB-002")).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Preço de venda");

            verify(produtoRepository, never()).save(any());
        }
    }

    // ============================================================
    // atualizar()
    // ============================================================

    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        private ProdutoRequest requestValido() {
            return new ProdutoRequest(
                    "BEB-001",
                    "Coca-Cola 600ml",
                    "Refrigerante PET",
                    new BigDecimal("4.00"),
                    new BigDecimal("7.00"),
                    15,
                    1L
            );
        }

        @Test
        @DisplayName("Deve atualizar produto com sucesso")
        void deveAtualizarComSucesso() {
            ProdutoRequest request = requestValido();

            when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoExistente));
            when(produtoRepository.existsBySkuAndIdNot("BEB-001", 10L)).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
            when(produtoRepository.save(any(Produto.class))).thenAnswer(inv -> inv.getArgument(0));

            ProdutoResponse resultado = service.atualizar(10L, request);

            assertThat(resultado.nome()).isEqualTo("Coca-Cola 600ml");
            assertThat(resultado.descricao()).isEqualTo("Refrigerante PET");
            assertThat(resultado.precoCusto()).isEqualByComparingTo("4.00");
            assertThat(resultado.precoVenda()).isEqualByComparingTo("7.00");
            assertThat(resultado.estoqueMinimo()).isEqualTo(15);

            verify(produtoRepository).save(produtoExistente);
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando ID não existir")
        void deveLancarExcecaoQuandoIdNaoExistir() {
            ProdutoRequest request = requestValido();

            when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizar(99L, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar SkuDuplicadoException quando SKU já existir em outro produto")
        void deveLancarExcecaoQuandoSkuDuplicado() {
            ProdutoRequest request = requestValido();

            when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoExistente));
            when(produtoRepository.existsBySkuAndIdNot("BEB-001", 10L)).thenReturn(true);

            assertThatThrownBy(() -> service.atualizar(10L, request))
                    .isInstanceOf(SkuDuplicadoException.class)
                    .hasMessageContaining("BEB-001");

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando categoria não existir")
        void deveLancarExcecaoQuandoCategoriaNaoExistir() {
            ProdutoRequest request = requestValido();

            when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoExistente));
            when(produtoRepository.existsBySkuAndIdNot("BEB-001", 10L)).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizar(10L, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Categoria não encontrada");

            verify(produtoRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando preço de venda < preço de custo")
        void deveLancarExcecaoQuandoPrecoInvalido() {
            ProdutoRequest request = new ProdutoRequest(
                    "BEB-001",
                    "Coca-Cola 600ml",
                    "Refrigerante PET",
                    new BigDecimal("10.00"),
                    new BigDecimal("5.00"),
                    15,
                    1L
            );

            when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoExistente));
            when(produtoRepository.existsBySkuAndIdNot("BEB-001", 10L)).thenReturn(false);
            when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));

            assertThatThrownBy(() -> service.atualizar(10L, request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Preço de venda");

            verify(produtoRepository, never()).save(any());
        }
    }

    // ============================================================
    // inativar()
    // ============================================================

    @Nested
    @DisplayName("inativar()")
    class Inativar {

        @Test
        @DisplayName("Deve inativar produto com sucesso")
        void deveInativarComSucesso() {
            when(produtoRepository.findById(10L)).thenReturn(Optional.of(produtoExistente));
            when(produtoRepository.save(any(Produto.class))).thenReturn(produtoExistente);

            service.inativar(10L);

            assertThat(produtoExistente.getAtivo()).isFalse();
            verify(produtoRepository).save(produtoExistente);
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando ID não existir")
        void deveLancarExcecaoQuandoIdNaoExistir() {
            when(produtoRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inativar(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(produtoRepository, never()).save(any());
        }
    }
}