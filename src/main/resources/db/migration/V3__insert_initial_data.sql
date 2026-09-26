INSERT INTO tb_categoria (nome, descricao) VALUES
    ('Geral', 'Categoria padrão para produtos sem classificação específica');

-- Usuário administrador padrão
-- Senha: admin123 (BCrypt hash)
INSERT INTO tb_usuario (nome, email, senha, perfil, ativo) VALUES
    ('Administrador', 'admin@gestao.local',
     '$2a$10$q1tAsx2Ru0NWXxt27oCHwO7LHK6XTOjMVU.0i8h7N20wV6.N364pm',
     'ADMIN', TRUE);