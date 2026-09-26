package com.gestao.api.security;

import com.gestao.api.model.Perfil;
import com.gestao.api.model.Usuario;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private JwtService jwtService;
    private Usuario usuario;

    // 256 bits mínimo para HMAC-SHA256
    private static final String SECRET =
            "chave-super-secreta-com-pelo-menos-256-bits-para-testes-hmac-sha256-ok";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expirationMs", 900000L); // 15 min

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNome("João Silva");
        usuario.setEmail("joao@gestao.local");
        usuario.setSenha("$2a$10$hash");
        usuario.setPerfil(Perfil.OPERADOR);
        usuario.setAtivo(true);
    }

    // ============================================================
    // gerarToken()
    // ============================================================

    @Nested
    @DisplayName("gerarToken()")
    class GerarToken {

        @Test
        @DisplayName("Deve gerar token JWT válido com subject = email do usuário")
        void deveGerarTokenValido() {
            String token = jwtService.gerarToken(usuario);

            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // header.payload.signature
        }

        @Test
        @DisplayName("Deve extrair o email do usuário como subject do token")
        void deveExtrairSubjectComoEmail() {
            String token = jwtService.gerarToken(usuario);

            String username = jwtService.extrairUsername(token);

            assertThat(username).isEqualTo("joao@gestao.local");
        }

        @Test
        @DisplayName("Deve incluir as authorities do usuário como claim")
        void deveIncluirAuthoritiesComoClaim() {
            String token = jwtService.gerarToken(usuario);

            var authorities = jwtService.extrairClaim(token, claims ->
                    claims.get("authorities"));

            assertThat(authorities).isNotNull();
            assertThat(authorities.toString()).contains("ROLE_OPERADOR");
        }
    }

    // ============================================================
    // extrairUsername() / extrairExpiracao()
    // ============================================================

    @Nested
    @DisplayName("extrair claims")
    class ExtrairClaims {

        @Test
        @DisplayName("Deve extrair a data de expiração do token")
        void deveExtrairExpiracao() {
            String token = jwtService.gerarToken(usuario);

            Date expiracao = jwtService.extrairExpiracao(token);

            assertThat(expiracao).isAfter(new Date());
        }

        @Test
        @DisplayName("Deve extrair o subject corretamente")
        void deveExtrairUsername() {
            String token = jwtService.gerarToken(usuario);

            assertThat(jwtService.extrairUsername(token)).isEqualTo("joao@gestao.local");
        }
    }

    // ============================================================
    // tokenValido()
    // ============================================================

    @Nested
    @DisplayName("tokenValido()")
    class TokenValido {

        @Test
        @DisplayName("Deve retornar true para token válido do mesmo usuário")
        void deveRetornarTrueParaTokenValido() {
            String token = jwtService.gerarToken(usuario);

            assertThat(jwtService.tokenValido(token, usuario)).isTrue();
        }

        @Test
        @DisplayName("Deve retornar false quando o username do token for diferente")
        void deveRetornarFalseParaUsernameDiferente() {
            String token = jwtService.gerarToken(usuario);

            Usuario outro = new Usuario();
            outro.setEmail("outro@gestao.local");

            assertThat(jwtService.tokenValido(token, outro)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para token expirado")
        void deveRetornarFalseParaTokenExpirado() {
            // Configura expiração para 1 ms no passado
            ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);

            String token = jwtService.gerarToken(usuario);

            assertThat(jwtService.tokenValido(token, usuario)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para token malformado")
        void deveRetornarFalseParaTokenMalformado() {
            assertThat(jwtService.tokenValido("token.invalido.aqui", usuario)).isFalse();
        }

        @Test
        @DisplayName("Deve retornar false para token assinado com outra chave")
        void deveRetornarFalseParaTokenComOutraChave() {
            JwtService outroServico = new JwtService();
            ReflectionTestUtils.setField(outroServico, "secret",
                    "outra-chave-completamente-diferente-com-256-bits-para-hmac-sha256-ok");
            ReflectionTestUtils.setField(outroServico, "expirationMs", 900000L);

            String tokenComOutraChave = outroServico.gerarToken(usuario);

            assertThat(jwtService.tokenValido(tokenComOutraChave, usuario)).isFalse();
        }
    }

    // ============================================================
    // getExpirationMs()
    // ============================================================

    @Nested
    @DisplayName("getExpirationMs()")
    class GetExpirationMs {

        @Test
        @DisplayName("Deve retornar o tempo de expiração configurado")
        void deveRetornarExpiracao() {
            assertThat(jwtService.getExpirationMs()).isEqualTo(900000L);
        }
    }

    // ============================================================
    // Exceções do JJWT
    // ============================================================

    @Nested
    @DisplayName("Exceções do JJWT")
    class ExcecoesJjwt {

        @Test
        @DisplayName("Deve lançar ExpiredJwtException ao extrair claims de token expirado")
        void deveLancarExcecaoParaTokenExpirado() {
            ReflectionTestUtils.setField(jwtService, "expirationMs", -1000L);
            String token = jwtService.gerarToken(usuario);

            assertThatThrownBy(() -> jwtService.extrairUsername(token))
                    .isInstanceOf(ExpiredJwtException.class);
        }

        @Test
        @DisplayName("Deve lançar JwtException ao extrair claims de token malformado")
        void deveLancarExcecaoParaTokenMalformado() {
            assertThatThrownBy(() -> jwtService.extrairUsername("nao.e.um.jwt"))
                    .isInstanceOf(JwtException.class);
        }
    }
}