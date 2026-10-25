CREATE TABLE interacoes (
    id BIGSERIAL PRIMARY KEY,
    texto TEXT NOT NULL,
    data_hora TIMESTAMP NOT NULL,
    chamado_id BIGINT NOT NULL,
    autor_id BIGINT NOT NULL,
    CONSTRAINT fk_interacoes_chamado FOREIGN KEY (chamado_id) REFERENCES chamados (id),
    CONSTRAINT fk_interacoes_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id)
);
