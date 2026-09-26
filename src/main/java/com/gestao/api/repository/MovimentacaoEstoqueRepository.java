package com.gestao.api.repository;

import com.gestao.api.model.MovimentacaoEstoque;
import com.gestao.api.model.TipoMovimentacao;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    Page<MovimentacaoEstoque> findByProdutoIdOrderByDataHoraDesc(Long produtoId, Pageable pageable);

    List<MovimentacaoEstoque> findByProdutoIdAndDataHoraBetweenOrderByDataHoraDesc(
            Long produtoId, LocalDateTime inicio, LocalDateTime fim);

    long countByProdutoId(Long produtoId);

    @Query("SELECT m FROM MovimentacaoEstoque m " +
            "JOIN FETCH m.produto p " +
            "WHERE m.tipo = :tipo " +
            "AND m.dataHora BETWEEN :inicio AND :fim " +
            "ORDER BY m.dataHora DESC")
    List<MovimentacaoEstoque> findByTipoAndPeriodo(
            @Param("tipo") TipoMovimentacao tipo,
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim);
}