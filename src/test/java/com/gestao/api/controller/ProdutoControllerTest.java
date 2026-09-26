package com.gestao.api.controller;

import com.gestao.api.config.JwtAuthenticationFilter;
import com.gestao.api.dto.produto.ProdutoRequest;
import com.gestao.api.dto.produto.ProdutoResponse;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.exception.SkuDuplicadoException;
import com.gestao.api.service.ProdutoService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProdutoController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProdutoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProdutoService produtoService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private ProdutoResponse produtoResponse() {
        return new ProdutoResponse(
                10L, "BEB-001", "Coca-Cola 350ml", "Refrigerante lata",
                new BigDecimal("3.00"), new BigDecimal("5.50"),
                100, 10, true, 1L, "Bebidas", false
        );
    }

    // ============================================================
    // GET /api/v1/produtos
    // ============================================================

    @Test
    @DisplayName("GET /produtos — deve retornar 200 com página de produtos")
    void deveListarProdutos() throws Exception {
        Page<ProdutoResponse> pagina = new PageImpl<>(
                List.of(produtoResponse()),
                PageRequest.of(0, 20),
                1
        );

        when(produtoService.listar(any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(10))
                .andExpect(jsonPath("$.content[0].sku").value("BEB-001"))
                .andExpect(jsonPath("$.content[0].categoriaNome").value("Bebidas"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // ============================================================
    // GET /api/v1/produtos/buscar?nome=...
    // ============================================================

    @Test
    @DisplayName("GET /produtos/buscar — deve retornar 200 com produtos filtrados")
    void deveBuscarProdutosPorNome() throws Exception {
        Page<ProdutoResponse> pagina = new PageImpl<>(
                List.of(produtoResponse()),
                PageRequest.of(0, 20),
                1
        );

        when(produtoService.buscarPorNome(eq("Coca"), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/produtos/buscar").param("nome", "Coca"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].nome").value("Coca-Cola 350ml"));
    }

    // ============================================================
    // GET /api/v1/produtos/categoria/{id}
    // ============================================================

    @Test
    @DisplayName("GET /produtos/categoria/{id} — deve retornar 200 com produtos da categoria")
    void deveBuscarProdutosPorCategoria() throws Exception {
        Page<ProdutoResponse> pagina = new PageImpl<>(
                List.of(produtoResponse()),
                PageRequest.of(0, 20),
                1
        );

        when(produtoService.buscarPorCategoria(eq(1L), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/produtos/categoria/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].categoriaId").value(1));
    }

    // ============================================================
    // GET /api/v1/produtos/{id}
    // ============================================================

    @Test
    @DisplayName("GET /produtos/{id} — deve retornar 200 quando existir")
    void deveBuscarProdutoPorId() throws Exception {
        when(produtoService.buscarPorId(10L)).thenReturn(produtoResponse());

        mockMvc.perform(get("/api/v1/produtos/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.sku").value("BEB-001"))
                .andExpect(jsonPath("$.abaixoDoMinimo").value(false));
    }

    @Test
    @DisplayName("GET /produtos/{id} — deve retornar 404 quando não existir")
    void deveRetornar404QuandoProdutoNaoExistir() throws Exception {
        when(produtoService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Produto não encontrado: 99"));

        mockMvc.perform(get("/api/v1/produtos/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    // ============================================================
    // POST /api/v1/produtos
    // ============================================================

    @Test
    @DisplayName("POST /produtos — deve retornar 201 com produto criado")
    void deveCriarProduto() throws Exception {
        ProdutoRequest request = new ProdutoRequest(
                "BEB-002", "Pepsi 350ml", "Refrigerante lata",
                new BigDecimal("3.00"), new BigDecimal("5.50"), 10, 1L
        );

        when(produtoService.criar(any(ProdutoRequest.class))).thenReturn(produtoResponse());

        mockMvc.perform(post("/api/v1/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    @DisplayName("POST /produtos — deve retornar 400 quando SKU for vazio")
    void deveRetornar400QuandoSkuVazio() throws Exception {
        ProdutoRequest request = new ProdutoRequest(
                "", "Pepsi 350ml", "Refrigerante lata",
                new BigDecimal("3.00"), new BigDecimal("5.50"), 10, 1L
        );

        mockMvc.perform(post("/api/v1/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams[0].campo").value("sku"));

        verify(produtoService, never()).criar(any());
    }

    @Test
    @DisplayName("POST /produtos — deve retornar 409 quando SKU já existir")
    void deveRetornar409QuandoSkuDuplicado() throws Exception {
        ProdutoRequest request = new ProdutoRequest(
                "BEB-001", "Coca-Cola", "Descrição",
                new BigDecimal("3.00"), new BigDecimal("5.50"), 10, 1L
        );

        when(produtoService.criar(any(ProdutoRequest.class)))
                .thenThrow(new SkuDuplicadoException("Já existe produto com SKU: BEB-001"));

        mockMvc.perform(post("/api/v1/produtos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de dados"));
    }

    // ============================================================
    // PUT /api/v1/produtos/{id}
    // ============================================================

    @Test
    @DisplayName("PUT /produtos/{id} — deve retornar 200 quando atualizar")
    void deveAtualizarProduto() throws Exception {
        ProdutoRequest request = new ProdutoRequest(
                "BEB-001", "Coca-Cola 600ml", "Refrigerante PET",
                new BigDecimal("4.00"), new BigDecimal("7.00"), 15, 1L
        );

        when(produtoService.atualizar(eq(10L), any(ProdutoRequest.class)))
                .thenReturn(produtoResponse());

        mockMvc.perform(put("/api/v1/produtos/{id}", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
    }

    // ============================================================
    // DELETE /api/v1/produtos/{id}
    // ============================================================

    @Test
    @DisplayName("DELETE /produtos/{id} — deve retornar 204 quando inativar")
    void deveInativarProduto() throws Exception {
        doNothing().when(produtoService).inativar(10L);

        mockMvc.perform(delete("/api/v1/produtos/{id}", 10L))
                .andExpect(status().isNoContent());

        verify(produtoService).inativar(10L);
    }
}