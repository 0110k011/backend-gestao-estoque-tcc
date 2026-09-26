package com.gestao.api.controller;

import com.gestao.api.config.JwtAuthenticationFilter;
import com.gestao.api.dto.auth.LoginRequest;
import com.gestao.api.dto.auth.RegisterRequest;
import com.gestao.api.dto.auth.TokenResponse;
import com.gestao.api.exception.CredenciaisInvalidasException;
import com.gestao.api.model.Perfil;
import com.gestao.api.service.AuthService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ============================================================
    // POST /api/v1/auth/register
    // ============================================================

    @Test
    @DisplayName("POST /auth/register — deve retornar 201 com token JWT")
    void deveRegistrarUsuario() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Maria Souza", "maria@gestao.local", "senhaSegura123", Perfil.OPERADOR
        );

        TokenResponse response = new TokenResponse("jwt.token.fake", "Bearer", 900000L);

        when(authService.registrar(any(RegisterRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt.token.fake"))
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiraEm").value(900000));

        verify(authService).registrar(any(RegisterRequest.class));
    }

    @Test
    @DisplayName("POST /auth/register — deve retornar 400 quando e-mail for inválido")
    void deveRetornar400QuandoEmailInvalido() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Maria Souza", "email-invalido", "senhaSegura123", Perfil.OPERADOR
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams").isArray())
                .andExpect(jsonPath("$.invalidParams[0].campo").value("email"));

        verify(authService, never()).registrar(any());
    }

    @Test
    @DisplayName("POST /auth/register — deve retornar 400 quando senha for curta")
    void deveRetornar400QuandoSenhaCurta() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "Maria Souza", "maria@gestao.local", "123", Perfil.OPERADOR
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[0].campo").value("senha"));
    }

    // ============================================================
    // POST /api/v1/auth/login
    // ============================================================

    @Test
    @DisplayName("POST /auth/login — deve retornar 200 com token JWT")
    void deveFazerLogin() throws Exception {
        LoginRequest request = new LoginRequest("joao@gestao.local", "senhaCorreta");
        TokenResponse response = new TokenResponse("jwt.token.valido", "Bearer", 900000L);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.valido"))
                .andExpect(jsonPath("$.tipo").value("Bearer"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("POST /auth/login — deve retornar 401 quando credenciais inválidas")
    void deveRetornar401QuandoCredenciaisInvalidas() throws Exception {
        LoginRequest request = new LoginRequest("joao@gestao.local", "senhaErrada");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new CredenciaisInvalidasException("E-mail ou senha inválidos"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciais inválidas"))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("POST /auth/login — deve retornar 400 quando e-mail for inválido")
    void deveRetornar400QuandoEmailLoginInvalido() throws Exception {
        LoginRequest request = new LoginRequest("nao-e-email", "senha");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.invalidParams[0].campo").value("email"));
    }
}