package com.gestao.api.model;

public enum Perfil {

    ADMIN("Administrador"),
    OPERADOR("Operador");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}