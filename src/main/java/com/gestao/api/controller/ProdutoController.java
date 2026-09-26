package com.gestao.api.controller;

import com.gestao.api.dto.produto.ProdutoRequest;
import com.gestao.api.dto.produto.ProdutoResponse;
import com.gestao.api.service.ProdutoService;
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
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Produtos", description = "Gerenciamento do catálogo de produtos")
public class ProdutoController {

    private final ProdutoService produtoService;

    @GetMapping
    @Operation(summary = "Listar produtos ativos (paginado)")
    public ResponseEntity<Page<ProdutoResponse>> listar(
            @PageableDefault(size = 20, sort = "nome") Pageable pageable) {
        return ResponseEntity.ok(produtoService.listar(pageable));
    }

    @GetMapping("/buscar")
    @Operation(summary = "Buscar produtos por nome")
    public ResponseEntity<Page<ProdutoResponse>> buscarPorNome(
            @RequestParam String nome,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorNome(nome, pageable));
    }

    @GetMapping("/categoria/{categoriaId}")
    @Operation(summary = "Listar produtos por categoria")
    public ResponseEntity<Page<ProdutoResponse>> buscarPorCategoria(
            @PathVariable Long categoriaId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(produtoService.buscarPorCategoria(categoriaId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar produto por ID")
    public ResponseEntity<ProdutoResponse> buscarPorId(@PathVariable Long id) {
        return ResponseEntity.ok(produtoService.buscarPorId(id));
    }

    @PostMapping
    @Operation(summary = "Criar novo produto")
    public ResponseEntity<ProdutoResponse> criar(@Valid @RequestBody ProdutoRequest request) {
        ProdutoResponse response = produtoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar produto existente")
    public ResponseEntity<ProdutoResponse> atualizar(
            @PathVariable Long id,
            @Valid @RequestBody ProdutoRequest request) {
        return ResponseEntity.ok(produtoService.atualizar(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Inativar produto")
    public ResponseEntity<Void> inativar(@PathVariable Long id) {
        produtoService.inativar(id);
        return ResponseEntity.noContent().build();
    }
}