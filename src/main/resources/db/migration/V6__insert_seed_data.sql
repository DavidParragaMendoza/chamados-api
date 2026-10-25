-- Datos semilla para pruebas locales

INSERT INTO setores (nome, sigla, ativo) VALUES
    ('Tecnologia da Informação', 'TI', TRUE),
    ('Recursos Humanos', 'RH', TRUE),
    ('Infraestrutura', 'INFRA', TRUE);

INSERT INTO categorias (nome, setor_id) VALUES
    ('Redes', (SELECT id FROM setores WHERE sigla = 'TI')),
    ('Hardware', (SELECT id FROM setores WHERE sigla = 'TI')),
    ('Sistemas', (SELECT id FROM setores WHERE sigla = 'TI')),
    ('Folha de Pagamento', (SELECT id FROM setores WHERE sigla = 'RH')),
    ('Elétrica', (SELECT id FROM setores WHERE sigla = 'INFRA'));

INSERT INTO usuarios (nome, email, matricula, tipo, setor_id) VALUES
    ('João Silva', 'joao@ifrn.edu.br', '2023101', 'SOLICITANTE', (SELECT id FROM setores WHERE sigla = 'TI')),
    ('Maria Souza', 'maria@ifrn.edu.br', '2023102', 'TECNICO', (SELECT id FROM setores WHERE sigla = 'TI')),
    ('Carlos Andrade', 'carlos@ifrn.edu.br', '2023103', 'TECNICO', (SELECT id FROM setores WHERE sigla = 'INFRA'));

INSERT INTO chamados (titulo, descricao, local, prioridade, status, data_abertura, data_fechamento, solucao, categoria_id, solicitante_id, tecnico_id) VALUES
    ('Sem acesso à rede Wi-Fi no Bloco B',
     'O notebook não consegue se conectar à rede sem fio institucional desde o início da manhã.',
     'Bloco B - Sala 12',
     'ALTA',
     'ABERTO',
     NOW() - INTERVAL '3 days',
     NULL,
     NULL,
     (SELECT id FROM categorias WHERE nome = 'Redes'),
     (SELECT id FROM usuarios WHERE email = 'joao@ifrn.edu.br'),
     NULL),
    ('Impressora do setor de TI não imprime',
     'A impressora HP LaserJet apresenta erro de comunicação ao enviar documentos para impressão.',
     'Bloco A - Sala 104',
     'MEDIA',
     'EM_ATENDIMENTO',
     NOW() - INTERVAL '2 days',
     NULL,
     NULL,
     (SELECT id FROM categorias WHERE nome = 'Hardware'),
     (SELECT id FROM usuarios WHERE email = 'joao@ifrn.edu.br'),
     (SELECT id FROM usuarios WHERE email = 'maria@ifrn.edu.br')),
    ('Erro ao acessar o sistema de ponto eletrônico',
     'Ao tentar registrar o ponto, o sistema exibe a mensagem de usuário não autorizado.',
     'Bloco C - Recepção',
     'BAIXA',
     'RESOLVIDO',
     NOW() - INTERVAL '5 days',
     NOW() - INTERVAL '3 days',
     'Foi realizada a sincronização do cadastro do usuário com o sistema de ponto eletrônico e o acesso foi restabelecido.',
     (SELECT id FROM categorias WHERE nome = 'Sistemas'),
     (SELECT id FROM usuarios WHERE email = 'joao@ifrn.edu.br'),
     (SELECT id FROM usuarios WHERE email = 'maria@ifrn.edu.br'));

INSERT INTO interacoes (texto, data_hora, chamado_id, autor_id) VALUES
    ('Bom dia, João. Já estou verificando a configuração da impressora no servidor de impressão.',
     NOW() - INTERVAL '2 days' + INTERVAL '30 minutes',
     (SELECT id FROM chamados WHERE titulo = 'Impressora do setor de TI não imprime'),
     (SELECT id FROM usuarios WHERE email = 'maria@ifrn.edu.br')),
    ('Obrigado, Maria. Fico no aguardo do retorno.',
     NOW() - INTERVAL '2 days' + INTERVAL '1 hour',
     (SELECT id FROM chamados WHERE titulo = 'Impressora do setor de TI não imprime'),
     (SELECT id FROM usuarios WHERE email = 'joao@ifrn.edu.br'));
