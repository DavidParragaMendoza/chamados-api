CREATE TABLE usuarios (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    matricula VARCHAR(100) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    setor_id BIGINT NULL,
    CONSTRAINT fk_usuarios_setor FOREIGN KEY (setor_id) REFERENCES setores (id)
);
