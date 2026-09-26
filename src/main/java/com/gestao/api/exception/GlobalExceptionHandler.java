package com.gestao.api.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== 400 Bad Request =====

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://api.gestao.local/errors/validacao"));
        problem.setTitle("Erro de validação");
        problem.setDetail("Um ou mais campos da requisição estão inválidos");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        List<Map<String, String>> erros = ex.getBindingResult().getFieldErrors()
                .stream()
                .map(this::toErroCampo)
                .toList();
        problem.setProperty("invalidParams", erros);

        return ResponseEntity.badRequest().body(problem);
    }

    @ExceptionHandler(EstoqueInsuficienteException.class)
    public ResponseEntity<ProblemDetail> handleEstoqueInsuficiente(
            EstoqueInsuficienteException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setType(URI.create("https://api.gestao.local/errors/estoque-insuficiente"));
        problem.setTitle("Estoque insuficiente");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.badRequest().body(problem);
    }

    // ===== 404 Not Found =====

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ProblemDetail> handleNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.NOT_FOUND);
        problem.setType(URI.create("https://api.gestao.local/errors/recurso-nao-encontrado"));
        problem.setTitle("Recurso não encontrado");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    // ===== 409 Conflict =====

    @ExceptionHandler(SkuDuplicadoException.class)
    public ResponseEntity<ProblemDetail> handleSkuDuplicado(
            SkuDuplicadoException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.CONFLICT);
        problem.setType(URI.create("https://api.gestao.local/errors/sku-duplicado"));
        problem.setTitle("Conflito de dados");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    @ExceptionHandler(CredenciaisInvalidasException.class)
    public ResponseEntity<ProblemDetail> handleCredenciaisInvalidas(
            CredenciaisInvalidasException ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.UNAUTHORIZED);
        problem.setType(URI.create("https://api.gestao.local/errors/credenciais-invalidas"));
        problem.setTitle("Credenciais inválidas");
        problem.setDetail(ex.getMessage());
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    // ===== 500 Internal Server Error =====

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGenerico(
            Exception ex, HttpServletRequest request) {

        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        problem.setType(URI.create("https://api.gestao.local/errors/erro-interno"));
        problem.setTitle("Erro interno do servidor");
        problem.setDetail("Ocorreu um erro inesperado. Contate o administrador do sistema.");
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());

        // Log do stack trace para diagnóstico interno
        // log.error("Erro não tratado", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    // ===== Auxiliar =====

    private Map<String, String> toErroCampo(FieldError fieldError) {
        return Map.of(
                "campo", fieldError.getField(),
                "mensagem", fieldError.getDefaultMessage()
        );
    }
}