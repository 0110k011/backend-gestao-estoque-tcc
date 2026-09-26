package com.gestao.api.exception;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller exclusivo para testes do GlobalExceptionHandler.
 * Cada endpoint dispara uma exceção específica para validar a resposta RFC 7807.
 */
@RestController
@RequestMapping("/test")
public class TestExceptionController {

    @GetMapping("/estoque-insuficiente")
    public void estoqueInsuficiente() {
        throw new EstoqueInsuficienteException("Saldo insuficiente: disponível 10, solicitado 100");
    }

    @GetMapping("/recurso-nao-encontrado")
    public void recursoNaoEncontrado() {
        throw new RecursoNaoEncontradoException("Produto não encontrado: 99");
    }

    @GetMapping("/sku-duplicado")
    public void skuDuplicado() {
        throw new SkuDuplicadoException("Já existe produto com SKU: BEB-001");
    }

    @GetMapping("/credenciais-invalidas")
    public void credenciaisInvalidas() {
        throw new CredenciaisInvalidasException("E-mail ou senha inválidos");
    }

    @GetMapping("/erro-generico")
    public void erroGenerico() {
        throw new RuntimeException("Erro inesperado de infraestrutura");
    }
}