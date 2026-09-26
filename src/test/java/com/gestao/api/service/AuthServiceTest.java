package com.gestao.api.service;

import com.gestao.api.dto.auth.LoginRequest;
import com.gestao.api.dto.auth.RegisterRequest;
import com.gestao.api.dto.auth.TokenResponse;
import com.gestao.api.exception.CredenciaisInvalidasException;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.model.Perfil;
import com.gestao.api.model.Usuario;
import com.gestao.api.repository.UsuarioRepository;
import com.gestao.api.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService service;

    private Usuario usuarioExistente;

    @BeforeEach
    void setUp() {
        usuarioExistente = new Usuario();
        usuarioExistente.setId(1L);
        usuarioExistente.setNome("João Silva");
        usuarioExistente.setEmail("joao@gestao.local");
        usuarioExistente.setSenha("$2a$10$hashBcryptSimulado");
        usuarioExistente.setPerfil(Perfil.OPERADOR);
        usuarioExistente.setAtivo(true);
    }

    // ============================================================
    // registrar()
    // ============================================================

    @Nested
    @DisplayName("registrar()")
    class Registrar {

        @Test
        @DisplayName("Deve registrar novo usuário e retornar token JWT")
        void deveRegistrarComSucesso() {
            RegisterRequest request = new RegisterRequest(
                    "Maria Souza",
                    "maria@gestao.local",
                    "senhaSegura123",
                    Perfil.OPERADOR
            );

            when(usuarioRepository.existsByEmail("maria@gestao.local")).thenReturn(false);
            when(passwordEncoder.encode("senhaSegura123")).thenReturn("$2a$10$hashGerado");
            when(jwtService.gerarToken(any(Usuario.class))).thenReturn("jwt.token.fake");
            when(jwtService.getExpirationMs()).thenReturn(900000L);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
                Usuario u = inv.getArgument(0);
                u.setId(2L);
                return u;
            });

            TokenResponse response = service.registrar(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt.token.fake");
            assertThat(response.tipo()).isEqualTo("Bearer");
            assertThat(response.expiraEm()).isEqualTo(900000L);

            verify(usuarioRepository).existsByEmail("maria@gestao.local");
            verify(passwordEncoder).encode("senhaSegura123");
            verify(usuarioRepository).save(any(Usuario.class));
            verify(jwtService).gerarToken(any(Usuario.class));
        }

        @Test
        @DisplayName("Deve forçar perfil OPERADOR no registro público")
        void deveForcarPerfilOperador() {
            RegisterRequest request = new RegisterRequest(
                    "Maria Souza",
                    "maria@gestao.local",
                    "senhaSegura123",
                    Perfil.OPERADOR
            );

            when(usuarioRepository.existsByEmail(anyString())).thenReturn(false);
            when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
            when(jwtService.gerarToken(any(Usuario.class))).thenReturn("jwt");
            when(jwtService.getExpirationMs()).thenReturn(900000L);
            when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

            service.registrar(request);

            verify(usuarioRepository).save(argThat(usuario ->
                    usuario.getPerfil() == Perfil.OPERADOR
            ));
        }

        @Test
        @DisplayName("Deve lançar IllegalArgumentException quando e-mail já existir")
        void deveLancarExcecaoQuandoEmailDuplicado() {
            RegisterRequest request = new RegisterRequest(
                    "Maria Souza",
                    "joao@gestao.local",
                    "senhaSegura123",
                    Perfil.OPERADOR
            );

            when(usuarioRepository.existsByEmail("joao@gestao.local")).thenReturn(true);

            assertThatThrownBy(() -> service.registrar(request))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("E-mail já cadastrado");

            verify(usuarioRepository, never()).save(any());
            verify(passwordEncoder, never()).encode(anyString());
        }
    }

    // ============================================================
    // login()
    // ============================================================

    @Nested
    @DisplayName("login()")
    class Login {

        @Test
        @DisplayName("Deve autenticar e retornar token JWT")
        void deveLoginComSucesso() {
            LoginRequest request = new LoginRequest("joao@gestao.local", "senhaCorreta");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null); // autenticação bem-sucedida (retorno ignorado)
            when(usuarioRepository.findByEmail("joao@gestao.local"))
                    .thenReturn(Optional.of(usuarioExistente));
            when(jwtService.gerarToken(usuarioExistente)).thenReturn("jwt.token.valido");
            when(jwtService.getExpirationMs()).thenReturn(900000L);

            TokenResponse response = service.login(request);

            assertThat(response).isNotNull();
            assertThat(response.token()).isEqualTo("jwt.token.valido");
            assertThat(response.tipo()).isEqualTo("Bearer");
            assertThat(response.expiraEm()).isEqualTo(900000L);

            verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
            verify(usuarioRepository).findByEmail("joao@gestao.local");
            verify(jwtService).gerarToken(usuarioExistente);
        }

        @Test
        @DisplayName("Deve lançar CredenciaisInvalidasException quando senha estiver errada")
        void deveLancarExcecaoQuandoSenhaInvalida() {
            LoginRequest request = new LoginRequest("joao@gestao.local", "senhaErrada");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(CredenciaisInvalidasException.class)
                    .hasMessageContaining("E-mail ou senha inválidos");

            verify(usuarioRepository, never()).findByEmail(anyString());
            verify(jwtService, never()).gerarToken(any());
        }

        @Test
        @DisplayName("Deve lançar CredenciaisInvalidasException quando e-mail não existir")
        void deveLancarExcecaoQuandoEmailNaoExistir() {
            LoginRequest request = new LoginRequest("inexistente@gestao.local", "qualquerSenha");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenThrow(new BadCredentialsException("Bad credentials"));

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(CredenciaisInvalidasException.class)
                    .hasMessageContaining("E-mail ou senha inválidos");
        }

        @Test
        @DisplayName("Deve lançar RecursoNaoEncontradoException quando autenticação OK mas usuário sumiu do banco")
        void deveLancarExcecaoQuandoUsuarioNaoEncontradoPosAutenticacao() {
            LoginRequest request = new LoginRequest("joao@gestao.local", "senhaCorreta");

            when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                    .thenReturn(null);
            when(usuarioRepository.findByEmail("joao@gestao.local"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.login(request))
                    .isInstanceOf(RecursoNaoEncontradoException.class)
                    .hasMessageContaining("Usuário não encontrado");

            verify(jwtService, never()).gerarToken(any());
        }
    }
}