package com.gestao.api.repository;

import com.gestao.api.model.Produto;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {


    Page<Produto> findByAtivoTrue(Pageable pageable);

    Page<Produto> findByAtivoTrueAndNomeContainingIgnoreCase(String nome, Pageable pageable);

    Page<Produto> findByAtivoTrueAndCategoriaId(Long categoriaId, Pageable pageable);

    Optional<Produto> findBySku(String sku);

    boolean existsBySku(String sku);

    boolean existsBySkuAndIdNot(String sku, Long id);

    List<Produto> findByAtivoTrueAndQuantidadeEstoqueLessThanEqual(Integer quantidade);


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Produto p WHERE p.id = :id")
    Optional<Produto> findByIdComBloqueio(@Param("id") Long id);
}