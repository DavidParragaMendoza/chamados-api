-- Usuario adicional para cumplir el requisito de al menos 4 usuarios iniciales
-- sin modificar la migración V6 ya aplicada (respetando la inmutabilidad de Flyway y la regla de no alterar migraciones ejecutadas).

INSERT INTO usuarios (nome, email, matricula, tipo, setor_id) VALUES
    ('Ana Lima', 'ana@ifrn.edu.br', '2023104', 'SOLICITANTE', (SELECT id FROM setores WHERE sigla = 'RH'));
