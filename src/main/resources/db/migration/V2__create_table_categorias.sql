CREATE TABLE categorias (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    setor_id BIGINT NOT NULL,
    CONSTRAINT fk_categorias_setor FOREIGN KEY (setor_id) REFERENCES setores (id)
);
