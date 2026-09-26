CREATE TABLE tb_usuario (
    id          BIGSERIAL       PRIMARY KEY,
    nome        VARCHAR(150)    NOT NULL,
    email       VARCHAR(150)    NOT NULL,
    senha       VARCHAR(255)    NOT NULL,
    perfil      VARCHAR(20)     NOT NULL,
    ativo       BOOLEAN         NOT NULL DEFAULT TRUE,
    criado_em   TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_perfil
        CHECK (perfil IN ('ADMIN', 'OPERADOR'))
);

CREATE INDEX idx_usuario_email ON tb_usuario (email);
CREATE INDEX idx_usuario_ativo ON tb_usuario (ativo);