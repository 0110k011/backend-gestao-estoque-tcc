package com.gestao.api.service;

import com.gestao.api.dto.auth.LoginRequest;
import com.gestao.api.dto.auth.RegisterRequest;
import com.gestao.api.dto.auth.TokenResponse;
import com.gestao.api.exception.CredenciaisInvalidasException;
import com.gestao.api.exception.RecursoNaoEncontradoException;
import com.gestao.api.model.Usuario;
import com.gestao.api.repository.UsuarioRepository;
import com.gestao.api.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    @Transactional
    public TokenResponse registrar(RegisterRequest request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setPerfil(request.perfil());
        usuario.setAtivo(true);

        usuarioRepository.save(usuario);

        String token = jwtService.gerarToken(usuario);
        return new TokenResponse(token, jwtService.getExpirationMs());
    }

    public TokenResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.senha())
            );
        } catch (BadCredentialsException e) {
            throw new CredenciaisInvalidasException("E-mail ou senha inválidos");
        }

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado"));

        String token = jwtService.gerarToken(usuario);
        return new TokenResponse(token, jwtService.getExpirationMs());
    }
}