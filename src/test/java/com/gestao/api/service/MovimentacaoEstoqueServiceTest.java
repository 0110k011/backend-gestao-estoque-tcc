package com.gestao.api.service;

import com.gestao.api.dto.movimentacao.MovimentacaoRequest;
import com.gestao.api.dto.movimentacao.MovimentacaoResponse;
import com.gestao.api.exception.EstoqueInsuficienteException;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.model.MovimentacaoEstoque;
import com.gestao.api.model.Produto;
import com.gestao.api.model.TipoMovimentacao;
import com.gestao.api.repository.MovimentacaoEstoqueRepository;
import com.gestao.api.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovimentacaoEstoqueServiceTest {

    @Mock
    private ProdutoRepository produtoRepository;

    @Mock
    private MovimentacaoEstoqueRepository movimentacaoRepository;

    @InjectMocks
    private MovimentacaoEstoqueService service;

    private Produto produto;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setSku("PROD-001");
        produto.setNome("Produto Teste");
        produto.setPrecoCusto(new BigDecimal("10.00"));
        produto.setPrecoVenda(new BigDecimal("20.00"));
        produto.setQuantidadeEstoque(10);
        produto.setEstoqueMinimo(2);
        produto.setAtivo(true);
    }

    @Test
    @DisplayName("Deve registrar ENTRADA e incrementar o saldo")
    void deveRegistrarEntrada() {
        when(produtoRepository.findByIdComBloqueio(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MovimentacaoRequest request = new MovimentacaoRequest(
                1L, TipoMovimentacao.ENTRADA, 5, new BigDecimal("10.00"), "Compra"
        );

        MovimentacaoResponse response = service.registrar(request);

        assertThat(response.tipo()).isEqualTo(TipoMovimentacao.ENTRADA);
        assertThat(response.quantidade()).isEqualTo(5);
        assertThat(produto.getQuantidadeEstoque()).isEqualTo(15);
        verify(produtoRepository).save(produto);
        verify(movimentacaoRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("Deve registrar SAIDA e decrementar o saldo")
    void deveRegistrarSaida() {
        when(produtoRepository.findByIdComBloqueio(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(any(Produto.class))).thenReturn(produto);
        when(movimentacaoRepository.save(any(MovimentacaoEstoque.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        MovimentacaoRequest request = new MovimentacaoRequest(
                1L, TipoMovimentacao.SAIDA, 3, new BigDecimal("20.00"), "Venda"
        );

        MovimentacaoResponse response = service.registrar(request);

        assertThat(response.tipo()).isEqualTo(TipoMovimentacao.SAIDA);
        assertThat(produto.getQuantidadeEstoque()).isEqualTo(7);
    }

    @Test
    @DisplayName("Deve lançar EstoqueInsuficienteException quando saldo for insuficiente")
    void deveLancarExcecaoQuandoSaldoInsuficiente() {
        when(produtoRepository.findByIdComBloqueio(1L)).thenReturn(Optional.of(produto));

        MovimentacaoRequest request = new MovimentacaoRequest(
                1L, TipoMovimentacao.SAIDA, 100, new BigDecimal("20.00"), "Venda"
        );

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(EstoqueInsuficienteException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(produtoRepository, never()).save(any());
        verify(movimentacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar RecursoNaoEncontradoException quando produto não existir")
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        when(produtoRepository.findByIdComBloqueio(99L)).thenReturn(Optional.empty());

        MovimentacaoRequest request = new MovimentacaoRequest(
                99L, TipoMovimentacao.ENTRADA, 5, new BigDecimal("10.00"), null
        );

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Produto não encontrado");
    }
}