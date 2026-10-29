INSERT INTO setores (nome, sigla) VALUES
    ('Coordenação de Tecnologia da Informação e Comunicação', 'COTIC'),
    ('Manutenção e Infraestrutura', 'MANUT');

INSERT INTO categorias (nome, descricao, setor_id) VALUES
    ('Rede e internet', 'Problemas com Wi-Fi, rede cabeada ou acesso à internet.', 1),
    ('Computadores e projetores', 'Suporte aos equipamentos das salas e dos laboratórios.', 1),
    ('Instalações elétricas', 'Problemas em tomadas, iluminação e instalações elétricas.', 2),
    ('Instalações hidráulicas', 'Vazamentos e manutenção de torneiras e bebedouros.', 2);

INSERT INTO usuarios (nome, email, matricula, tipo, setor_id) VALUES
    ('Ana Beatriz', 'ana.beatriz@escolar.ifrn.edu.br', '2026101001', 'SOLICITANTE', NULL),
    ('Lucas Ferreira', 'lucas.ferreira@escolar.ifrn.edu.br', '2026101002', 'SOLICITANTE', NULL),
    ('Marcos Silva', 'marcos.silva@ifrn.edu.br', '1234567', 'TECNICO', 1),
    ('Juliana Costa', 'juliana.costa@ifrn.edu.br', '7654321', 'TECNICO', 2);
