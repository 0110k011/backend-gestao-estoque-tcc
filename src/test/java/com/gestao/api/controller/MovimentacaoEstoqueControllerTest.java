package com.gestao.api.controller;

import com.gestao.api.config.JwtAuthenticationFilter;
import com.gestao.api.dto.movimentacao.MovimentacaoRequest;
import com.gestao.api.dto.movimentacao.MovimentacaoResponse;
import com.gestao.api.exception.EstoqueInsuficienteException;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.model.TipoMovimentacao;
import com.gestao.api.service.MovimentacaoEstoqueService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MovimentacaoEstoqueController.class)
@AutoConfigureMockMvc(addFilters = false)
class MovimentacaoEstoqueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MovimentacaoEstoqueService movimentacaoService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MovimentacaoResponse movimentacaoResponse() {
        return new MovimentacaoResponse(
                1L, 10L, "Coca-Cola 350ml",
                TipoMovimentacao.SAIDA, 3,
                new BigDecimal("20.00"),
                LocalDateTime.of(2026, 9, 26, 14, 30),
                "Venda"
        );
    }

    // ============================================================
    // POST /api/v1/movimentacoes
    // ============================================================

    @Test
    @DisplayName("POST /movimentacoes — deve retornar 201 com movimentação registrada")
    void deveRegistrarMovimentacao() throws Exception {
        MovimentacaoRequest request = new MovimentacaoRequest(
                10L, TipoMovimentacao.SAIDA, 3,
                new BigDecimal("20.00"), "Venda"
        );

        when(movimentacaoService.registrar(any(MovimentacaoRequest.class)))
                .thenReturn(movimentacaoResponse());

        mockMvc.perform(post("/api/v1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.tipo").value("SAIDA"))
                .andExpect(jsonPath("$.quantidade").value(3))
                .andExpect(jsonPath("$.produtoNome").value("Coca-Cola 350ml"));

        verify(movimentacaoService).registrar(any(MovimentacaoRequest.class));
    }

    @Test
    @DisplayName("POST /movimentacoes — deve retornar 400 quando quantidade for negativa")
    void deveRetornar400QuandoQuantidadeNegativa() throws Exception {
        MovimentacaoRequest request = new MovimentacaoRequest(
                10L, TipoMovimentacao.SAIDA, -5,
                new BigDecimal("20.00"), "Venda"
        );

        mockMvc.perform(post("/api/v1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams[0].campo").value("quantidade"));

        verify(movimentacaoService, never()).registrar(any());
    }

    @Test
    @DisplayName("POST /movimentacoes — deve retornar 400 quando estoque insuficiente")
    void deveRetornar400QuandoEstoqueInsuficiente() throws Exception {
        MovimentacaoRequest request = new MovimentacaoRequest(
                10L, TipoMovimentacao.SAIDA, 100,
                new BigDecimal("20.00"), "Venda"
        );

        when(movimentacaoService.registrar(any(MovimentacaoRequest.class)))
                .thenThrow(new EstoqueInsuficienteException(
                        "Saldo insuficiente: disponível 10, solicitado 100"));

        mockMvc.perform(post("/api/v1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Estoque insuficiente"))
                .andExpect(jsonPath("$.detail").value("Saldo insuficiente: disponível 10, solicitado 100"));
    }

    @Test
    @DisplayName("POST /movimentacoes — deve retornar 404 quando produto não existir")
    void deveRetornar404QuandoProdutoNaoExistir() throws Exception {
        MovimentacaoRequest request = new MovimentacaoRequest(
                99L, TipoMovimentacao.ENTRADA, 5,
                new BigDecimal("10.00"), "Compra"
        );

        when(movimentacaoService.registrar(any(MovimentacaoRequest.class)))
                .thenThrow(new RecursoNaoEncontradoException("Produto não encontrado: 99"));

        mockMvc.perform(post("/api/v1/movimentacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    // ============================================================
    // GET /api/v1/movimentacoes/produto/{id}
    // ============================================================

    @Test
    @DisplayName("GET /movimentacoes/produto/{id} — deve retornar 200 com página de movimentações")
    void deveListarMovimentacoesPorProduto() throws Exception {
        Page<MovimentacaoResponse> pagina = new PageImpl<>(
                List.of(movimentacaoResponse()),
                PageRequest.of(0, 20),
                1
        );

        when(movimentacaoService.listarPorProduto(eq(10L), any())).thenReturn(pagina);

        mockMvc.perform(get("/api/v1/movimentacoes/produto/{id}", 10L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].tipo").value("SAIDA"));
    }
}