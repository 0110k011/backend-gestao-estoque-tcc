CREATE TABLE tb_categoria (
    id          BIGSERIAL       PRIMARY KEY,
    nome        VARCHAR(100)    NOT NULL,
    descricao   VARCHAR(255),
    ativo       BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_categoria_nome UNIQUE (nome)
);

CREATE INDEX idx_categoria_ativo ON tb_categoria (ativo);

CREATE TABLE tb_produto (
    id                  BIGSERIAL       PRIMARY KEY,
    categoria_id        BIGINT          NOT NULL,
    sku                 VARCHAR(50)     NOT NULL,
    nome                VARCHAR(150)    NOT NULL,
    descricao           TEXT,
    preco_custo         NUMERIC(10,2)   NOT NULL,
    preco_venda         NUMERIC(10,2)   NOT NULL,
    quantidade_estoque  INTEGER         NOT NULL DEFAULT 0,
    estoque_minimo      INTEGER         NOT NULL DEFAULT 0,
    ativo               BOOLEAN         NOT NULL DEFAULT TRUE,

    CONSTRAINT uq_produto_sku UNIQUE (sku),
    CONSTRAINT fk_produto_categoria
        FOREIGN KEY (categoria_id)
        REFERENCES tb_categoria (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_produto_quantidade_estoque
        CHECK (quantidade_estoque >= 0),
    CONSTRAINT ck_produto_preco_venda
        CHECK (preco_venda >= preco_custo),
    CONSTRAINT ck_produto_estoque_minimo
        CHECK (estoque_minimo >= 0)
);

CREATE INDEX idx_produto_categoria ON tb_produto (categoria_id);
CREATE INDEX idx_produto_ativo ON tb_produto (ativo);
CREATE INDEX idx_produto_sku ON tb_produto (sku);

CREATE TABLE tb_movimentacao_estoque (
    id              BIGSERIAL       PRIMARY KEY,
    produto_id      BIGINT          NOT NULL,
    tipo            VARCHAR(20)     NOT NULL,
    quantidade      INTEGER         NOT NULL,
    valor_unitario  NUMERIC(10,2)   NOT NULL,
    data_hora       TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    observacao      VARCHAR(255),

    CONSTRAINT fk_movimentacao_produto
        FOREIGN KEY (produto_id)
        REFERENCES tb_produto (id)
        ON DELETE RESTRICT,
    CONSTRAINT ck_movimentacao_quantidade
        CHECK (quantidade > 0),
    CONSTRAINT ck_movimentacao_tipo
        CHECK (tipo IN ('ENTRADA', 'SAIDA'))
);

CREATE INDEX idx_movimentacao_produto ON tb_movimentacao_estoque (produto_id);
CREATE INDEX idx_movimentacao_data ON tb_movimentacao_estoque (data_hora);
CREATE INDEX idx_movimentacao_tipo ON tb_movimentacao_estoque (tipo);