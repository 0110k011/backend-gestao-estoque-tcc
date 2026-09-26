package com.gestao.api.exception;

import com.gestao.api.config.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TestExceptionController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ============================================================
    // 400 Bad Request
    // ============================================================

    @Test
    @DisplayName("EstoqueInsuficienteException — deve retornar 400 com ProblemDetail")
    void deveTratarEstoqueInsuficiente() throws Exception {
        mockMvc.perform(get("/test/estoque-insuficiente"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.type").value("https://api.gestao.local/errors/estoque-insuficiente"))
                .andExpect(jsonPath("$.title").value("Estoque insuficiente"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value("Saldo insuficiente: disponível 10, solicitado 100"))
                .andExpect(jsonPath("$.instance").value("/test/estoque-insuficiente"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ============================================================
    // 404 Not Found
    // ============================================================

    @Test
    @DisplayName("RecursoNaoEncontradoException — deve retornar 404 com ProblemDetail")
    void deveTratarRecursoNaoEncontrado() throws Exception {
        mockMvc.perform(get("/test/recurso-nao-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.gestao.local/errors/recurso-nao-encontrado"))
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Produto não encontrado: 99"))
                .andExpect(jsonPath("$.instance").value("/test/recurso-nao-encontrado"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    // ============================================================
    // 409 Conflict
    // ============================================================

    @Test
    @DisplayName("SkuDuplicadoException — deve retornar 409 com ProblemDetail")
    void deveTratarSkuDuplicado() throws Exception {
        mockMvc.perform(get("/test/sku-duplicado"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://api.gestao.local/errors/sku-duplicado"))
                .andExpect(jsonPath("$.title").value("Conflito de dados"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.detail").value("Já existe produto com SKU: BEB-001"));
    }

    // ============================================================
    // 401 Unauthorized
    // ============================================================

    @Test
    @DisplayName("CredenciaisInvalidasException — deve retornar 401 com ProblemDetail")
    void deveTratarCredenciaisInvalidas() throws Exception {
        mockMvc.perform(get("/test/credenciais-invalidas"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.type").value("https://api.gestao.local/errors/credenciais-invalidas"))
                .andExpect(jsonPath("$.title").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.status").value(401));
    }

    // ============================================================
    // 500 Internal Server Error
    // ============================================================

    @Test
    @DisplayName("Exception genérica — deve retornar 500 sem expor stack trace")
    void deveTratarErroGenerico() throws Exception {
        mockMvc.perform(get("/test/erro-generico"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.type").value("https://api.gestao.local/errors/erro-interno"))
                .andExpect(jsonPath("$.title").value("Erro interno do servidor"))
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.detail").value(
                        "Ocorreu um erro inesperado. Contate o administrador do sistema."))
                // Garante que o stack trace NÃO vaza para o cliente
                .andExpect(jsonPath("$.stackTrace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }
}