package com.gestao.api.controller;

import com.gestao.api.dto.movimentacao.MovimentacaoRequest;
import com.gestao.api.dto.movimentacao.MovimentacaoResponse;
import com.gestao.api.service.MovimentacaoEstoqueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/movimentacoes")
@RequiredArgsConstructor
@Tag(name = "Movimentações", description = "Registro de entradas e saídas de estoque")
public class MovimentacaoEstoqueController {

    private final MovimentacaoEstoqueService movimentacaoService;

    @PostMapping
    @Operation(summary = "Registrar movimentação (entrada ou saída)")
    public ResponseEntity<MovimentacaoResponse> registrar(
            @Valid @RequestBody MovimentacaoRequest request) {
        MovimentacaoResponse response = movimentacaoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/produto/{produtoId}")
    @Operation(summary = "Listar movimentações de um produto")
    public ResponseEntity<Page<MovimentacaoResponse>> listarPorProduto(
            @PathVariable Long produtoId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(movimentacaoService.listarPorProduto(produtoId, pageable));
    }
}