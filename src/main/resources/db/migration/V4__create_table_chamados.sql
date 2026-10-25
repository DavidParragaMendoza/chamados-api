CREATE TABLE chamados (
    id BIGSERIAL PRIMARY KEY,
    titulo VARCHAR(255) NOT NULL,
    descricao TEXT NOT NULL,
    local VARCHAR(255) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    data_abertura TIMESTAMP NOT NULL,
    data_fechamento TIMESTAMP NULL,
    solucao TEXT NULL,
    categoria_id BIGINT NOT NULL,
    solicitante_id BIGINT NOT NULL,
    tecnico_id BIGINT NULL,
    CONSTRAINT fk_chamados_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_chamados_solicitante FOREIGN KEY (solicitante_id) REFERENCES usuarios (id),
    CONSTRAINT fk_chamados_tecnico FOREIGN KEY (tecnico_id) REFERENCES usuarios (id)
);
