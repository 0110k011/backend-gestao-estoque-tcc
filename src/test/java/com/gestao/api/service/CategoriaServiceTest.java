package com.gestao.api.service;

import com.gestao.api.dto.categoria.CategoriaRequest;
import com.gestao.api.dto.categoria.CategoriaResponse;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.exception.SkuDuplicadoException;
import com.gestao.api.model.Categoria;
import com.gestao.api.repository.CategoriaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository categoriaRepository;

    @InjectMocks
    private CategoriaService service;

    private Categoria categoriaExistente;

    @BeforeEach
    void setUp() {
        categoriaExistente = new Categoria();
        categoriaExistente.setId(1L);
        categoriaExistente.setNome("Bebidas");
        categoriaExistente.setDescricao("Bebidas em geral");
        categoriaExistente.setAtivo(true);
    }

    // ============================================================
    // listarAtivas()
    // ============================================================

    @Nested
    @DisplayName("listarAtivas()")
    class ListarAtivas {

        @Test
        @DisplayName("Deve retornar lista vazia quando não houver categorias ativas")
        void deveRetornarListaVazia() {
            when(categoriaRepository.findByAtivoTrueOrderByNomeAsc())
                    .thenReturn(Collections.emptyList());

            List<CategoriaResponse> resultado = service.listarAtivas();

            assertThat(resultado).isEmpty();
            verify(categoriaRepository).findByAtivoTrueOrderByNomeAsc();
        }

        @Test
        @DisplayName("Deve retornar lista de categorias ativas convertidas em DTO")
        void deveRetornarListaDeCategorias() {
            Categoria segunda = new Categoria();
            segunda.setId(2L);
            segunda.setNome("Alimentos");
            segunda.setDescricao("Alimentos em geral");
            segunda.setAtivo(true);

            when(categoriaRepository.findByAtivoTrueOrderByNomeAsc())
                    .thenReturn(List.of(categoriaExistente, segunda));

            List<CategoriaResponse> resultado = service.listarAtivas();

            assertThat(resultado).hasSize(2);
            assertThat(resultado.get(0).id()).isEqualTo(1L);
            assertThat(resultado.get(0).nome()).isEqualTo("Bebidas");
            assertThat(resultado.get(0).ativo()).isTrue();
            assertThat(resultado.get(1).id()).isEqualTo(2L);
            assertThat(resultado.get(1).nome()).isEqualTo("Alimentos");
        }
    }

    // ============================================================
    // buscarPorId()
    // ============================================================

    @Nested
    @DisplayName("buscarPorId()")
    class BuscarPorId {

        @Test
        @DisplayName("Deve retornar categoria quando ID existir")
        void deveRetornarCategoriaQuandoIdExistir() {
            when(categoriaRepository.findById(1L))
                    .thenReturn(Optional.of(categoriaExistente));

            CategoriaResponse resultado = service.buscarPorId(1L);

            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(1L);
            assertThat(resultado.nome()).isEqualTo("Bebidas");
            assertThat(resultado.descricao()).isEqualTo("Bebidas em geral");
            assertThat(resultado.ativo()).isTrue();
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando ID não existir")
        void deveLancarExcecaoQuandoIdNaoExistir() {
            when(categoriaRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.buscarPorId(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Categoria não encontrada")
                    .hasMessageContaining("99");
        }
    }

    // ============================================================
    // criar()
    // ============================================================

    @Nested
    @DisplayName("criar()")
    class Criar {

        @Test
        @DisplayName("Deve criar categoria com sucesso")
        void deveCriarCategoriaComSucesso() {
            CategoriaRequest request = new CategoriaRequest("Bebidas", "Bebidas em geral");

            when(categoriaRepository.existsByNomeIgnoreCase("Bebidas"))
                    .thenReturn(false);
            when(categoriaRepository.save(any(Categoria.class)))
                    .thenAnswer(inv -> {
                        Categoria c = inv.getArgument(0);
                        c.setId(1L);
                        return c;
                    });

            CategoriaResponse resultado = service.criar(request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.id()).isEqualTo(1L);
            assertThat(resultado.nome()).isEqualTo("Bebidas");
            assertThat(resultado.descricao()).isEqualTo("Bebidas em geral");
            assertThat(resultado.ativo()).isTrue();

            verify(categoriaRepository).existsByNomeIgnoreCase("Bebidas");
            verify(categoriaRepository).save(any(Categoria.class));
        }

        @Test
        @DisplayName("Deve lançar SkuDuplicadoException quando nome já existir")
        void deveLancarExcecaoQuandoNomeDuplicado() {
            CategoriaRequest request = new CategoriaRequest("Bebidas", "Bebidas em geral");

            when(categoriaRepository.existsByNomeIgnoreCase("Bebidas"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.criar(request))
                    .isInstanceOf(SkuDuplicadoException.class)
                    .hasMessageContaining("Bebidas");

            verify(categoriaRepository, never()).save(any());
        }
    }

    // ============================================================
    // atualizar()
    // ============================================================

    @Nested
    @DisplayName("atualizar()")
    class Atualizar {

        @Test
        @DisplayName("Deve atualizar categoria com sucesso (mesmo nome)")
        void deveAtualizarComMesmoNome() {
            CategoriaRequest request = new CategoriaRequest("Bebidas", "Nova descrição");

            when(categoriaRepository.findById(1L))
                    .thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.save(any(Categoria.class)))
                    .thenReturn(categoriaExistente);

            CategoriaResponse resultado = service.atualizar(1L, request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.descricao()).isEqualTo("Nova descrição");

            // Não deve verificar duplicidade, pois o nome não mudou
            verify(categoriaRepository, never()).existsByNomeIgnoreCase(anyString());
        }

        @Test
        @DisplayName("Deve atualizar categoria com sucesso (nome diferente e único)")
        void deveAtualizarComNomeDiferente() {
            CategoriaRequest request = new CategoriaRequest("Alimentos", "Alimentos em geral");

            when(categoriaRepository.findById(1L))
                    .thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeIgnoreCase("Alimentos"))
                    .thenReturn(false);
            when(categoriaRepository.save(any(Categoria.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            CategoriaResponse resultado = service.atualizar(1L, request);

            assertThat(resultado).isNotNull();
            assertThat(resultado.nome()).isEqualTo("Alimentos");

            verify(categoriaRepository).existsByNomeIgnoreCase("Alimentos");
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando ID não existir")
        void deveLancarExcecaoQuandoIdNaoExistir() {
            CategoriaRequest request = new CategoriaRequest("Bebidas", "Descrição");

            when(categoriaRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.atualizar(99L, request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(categoriaRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve lançar SkuDuplicadoException quando novo nome já existir em outra categoria")
        void deveLancarExcecaoQuandoNomeDuplicado() {
            CategoriaRequest request = new CategoriaRequest("Alimentos", "Alimentos em geral");

            when(categoriaRepository.findById(1L))
                    .thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.existsByNomeIgnoreCase("Alimentos"))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.atualizar(1L, request))
                    .isInstanceOf(SkuDuplicadoException.class)
                    .hasMessageContaining("Alimentos");

            verify(categoriaRepository, never()).save(any());
        }
    }

    // ============================================================
    // inativar()
    // ============================================================

    @Nested
    @DisplayName("inativar()")
    class Inativar {

        @Test
        @DisplayName("Deve inativar categoria com sucesso")
        void deveInativarComSucesso() {
            when(categoriaRepository.findById(1L))
                    .thenReturn(Optional.of(categoriaExistente));
            when(categoriaRepository.save(any(Categoria.class)))
                    .thenReturn(categoriaExistente);

            service.inativar(1L);

            assertThat(categoriaExistente.getAtivo()).isFalse();
            verify(categoriaRepository).save(categoriaExistente);
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando ID não existir")
        void deveLancarExcecaoQuandoIdNaoExistir() {
            when(categoriaRepository.findById(99L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.inativar(99L))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("99");

            verify(categoriaRepository, never()).save(any());
        }
    }
}
