package com.gestao.api.controller;

import com.gestao.api.config.JwtAuthenticationFilter;
import com.gestao.api.dto.categoria.CategoriaRequest;
import com.gestao.api.dto.categoria.CategoriaResponse;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.exception.SkuDuplicadoException;
import com.gestao.api.service.CategoriaService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoriaController.class)
@AutoConfigureMockMvc(addFilters = false)
class CategoriaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoriaService categoriaService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ============================================================
    // GET /api/v1/categorias
    // ============================================================

    @Test
    @DisplayName("GET /categorias — deve retornar 200 com lista de categorias")
    void deveListarCategorias() throws Exception {
        List<CategoriaResponse> categorias = List.of(
                new CategoriaResponse(1L, "Bebidas", "Bebidas em geral", true),
                new CategoriaResponse(2L, "Alimentos", "Alimentos em geral", true)
        );

        when(categoriaService.listarAtivas()).thenReturn(categorias);

        mockMvc.perform(get("/api/v1/categorias"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].nome").value("Bebidas"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].nome").value("Alimentos"));

        verify(categoriaService).listarAtivas();
    }

    // ============================================================
    // GET /api/v1/categorias/{id}
    // ============================================================

    @Test
    @DisplayName("GET /categorias/{id} — deve retornar 200 quando existir")
    void deveBuscarCategoriaPorId() throws Exception {
        CategoriaResponse response = new CategoriaResponse(1L, "Bebidas", "Bebidas em geral", true);

        when(categoriaService.buscarPorId(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/categorias/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Bebidas"))
                .andExpect(jsonPath("$.ativo").value(true));

        verify(categoriaService).buscarPorId(1L);
    }

    @Test
    @DisplayName("GET /categorias/{id} — deve retornar 404 quando não existir")
    void deveRetornar404QuandoCategoriaNaoExistir() throws Exception {
        when(categoriaService.buscarPorId(99L))
                .thenThrow(new RecursoNaoEncontradoException("Categoria não encontrada: 99"));

        mockMvc.perform(get("/api/v1/categorias/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.detail").value("Categoria não encontrada: 99"))
                .andExpect(jsonPath("$.status").value(404));
    }

    // ============================================================
    // POST /api/v1/categorias
    // ============================================================

    @Test
    @DisplayName("POST /categorias — deve retornar 201 com categoria criada")
    void deveCriarCategoria() throws Exception {
        CategoriaRequest request = new CategoriaRequest("Bebidas", "Bebidas em geral");
        CategoriaResponse response = new CategoriaResponse(1L, "Bebidas", "Bebidas em geral", true);

        when(categoriaService.criar(any(CategoriaRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.nome").value("Bebidas"));

        verify(categoriaService).criar(any(CategoriaRequest.class));
    }

    @Test
    @DisplayName("POST /categorias — deve retornar 400 quando nome for vazio")
    void deveRetornar400QuandoNomeVazio() throws Exception {
        CategoriaRequest request = new CategoriaRequest("", "Descrição qualquer");

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Erro de validação"))
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams[0].campo").value("nome"));

        verify(categoriaService, never()).criar(any());
    }

    @Test
    @DisplayName("POST /categorias — deve retornar 409 quando nome já existir")
    void deveRetornar409QuandoNomeDuplicado() throws Exception {
        CategoriaRequest request = new CategoriaRequest("Bebidas", "Descrição");

        when(categoriaService.criar(any(CategoriaRequest.class)))
                .thenThrow(new SkuDuplicadoException("Já existe categoria com o nome: Bebidas"));

        mockMvc.perform(post("/api/v1/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflito de dados"))
                .andExpect(jsonPath("$.status").value(409));
    }

    // ============================================================
    // PUT /api/v1/categorias/{id}
    // ============================================================

    @Test
    @DisplayName("PUT /categorias/{id} — deve retornar 200 quando atualizar")
    void deveAtualizarCategoria() throws Exception {
        CategoriaRequest request = new CategoriaRequest("Bebidas Alcoólicas", "Descrição atualizada");
        CategoriaResponse response = new CategoriaResponse(1L, "Bebidas Alcoólicas", "Descrição atualizada", true);

        when(categoriaService.atualizar(eq(1L), any(CategoriaRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/categorias/{id}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Bebidas Alcoólicas"));

        verify(categoriaService).atualizar(eq(1L), any(CategoriaRequest.class));
    }

    // ============================================================
    // DELETE /api/v1/categorias/{id}
    // ============================================================

    @Test
    @DisplayName("DELETE /categorias/{id} — deve retornar 204 quando inativar")
    void deveInativarCategoria() throws Exception {
        doNothing().when(categoriaService).inativar(1L);

        mockMvc.perform(delete("/api/v1/categorias/{id}", 1L))
                .andExpect(status().isNoContent());

        verify(categoriaService).inativar(1L);
    }

    @Test
    @DisplayName("DELETE /categorias/{id} — deve retornar 404 quando não existir")
    void deveRetornar404AoInativarCategoriaInexistente() throws Exception {
        doThrow(new RecursoNaoEncontradoException("Categoria não encontrada: 99"))
                .when(categoriaService).inativar(99L);

        mockMvc.perform(delete("/api/v1/categorias/{id}", 99L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }
}