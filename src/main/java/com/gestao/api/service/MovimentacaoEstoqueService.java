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
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueService {

    private final ProdutoRepository produtoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;

    /**
     * Registra uma movimentação (entrada ou saída) no estoque.
     *
     * Fluxo:
     * 1. Bloqueia o produto com PESSIMISTIC_WRITE (impede concorrência)
     * 2. Valida a operação conforme o tipo
     * 3. Atualiza o saldo do produto
     * 4. Insere o registro de movimentação
     *
     * @Transactional garante atomicidade: se qualquer etapa falhar, tudo sofre rollback.
     */
    @Transactional
    public MovimentacaoResponse registrar(MovimentacaoRequest request) {

        // 1. Bloqueio pessimista: SELECT ... FOR UPDATE
        //    Impede que duas transações simultâneas alterem o mesmo produto.
        Produto produto = produtoRepository.findByIdComBloqueio(request.produtoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Produto não encontrado: " + request.produtoId()));

        // 2. Validações conforme o tipo de movimentação
        if (request.tipo() == TipoMovimentacao.SAIDA) {
            validarSaida(produto, request.quantidade());
        }

        // 3. Atualização do saldo lógico
        int novoSaldo = calcularNovoSaldo(produto, request.tipo(), request.quantidade());
        produto.setQuantidadeEstoque(novoSaldo);
        produtoRepository.save(produto);

        // 4. Registro imutável da movimentação
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setProduto(produto);
        movimentacao.setTipo(request.tipo());
        movimentacao.setQuantidade(request.quantidade());
        movimentacao.setValorUnitario(request.valorUnitario());
        movimentacao.setDataHora(LocalDateTime.now());
        movimentacao.setObservacao(request.observacao());

        MovimentacaoEstoque salva = movimentacaoRepository.save(movimentacao);

        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public Page<MovimentacaoResponse> listarPorProduto(Long produtoId, Pageable pageable) {
        return movimentacaoRepository.findByProdutoIdOrderByDataHoraDesc(produtoId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public long contarPorProduto(Long produtoId) {
        return movimentacaoRepository.countByProdutoId(produtoId);
    }

    // ===== Métodos auxiliares =====

    private void validarSaida(Produto produto, Integer quantidade) {
        if (produto.getQuantidadeEstoque() < quantidade) {
            throw new EstoqueInsuficienteException(
                    "Saldo insuficiente: disponível " + produto.getQuantidadeEstoque()
                            + ", solicitado " + quantidade);
        }
    }

    private int calcularNovoSaldo(Produto produto, TipoMovimentacao tipo, Integer quantidade) {
        return switch (tipo) {
            case ENTRADA -> produto.getQuantidadeEstoque() + quantidade;
            case SAIDA -> produto.getQuantidadeEstoque() - quantidade;
        };
    }

    private MovimentacaoResponse toResponse(MovimentacaoEstoque movimentacao) {
        return new MovimentacaoResponse(
                movimentacao.getId(),
                movimentacao.getProduto().getId(),
                movimentacao.getProduto().getNome(),
                movimentacao.getTipo(),
                movimentacao.getQuantidade(),
                movimentacao.getValorUnitario(),
                movimentacao.getDataHora(),
                movimentacao.getObservacao()
        );
    }
}