CREATE TABLE pauta (
    id UUID PRIMARY KEY,
    titulo VARCHAR(200) NOT NULL,
    descricao VARCHAR(2000),
    criada_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE sessao_votacao (
    id UUID PRIMARY KEY,
    pauta_id UUID NOT NULL UNIQUE,
    aberta_em TIMESTAMP WITH TIME ZONE NOT NULL,
    fecha_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_sessao_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT ck_sessao_periodo CHECK (fecha_em > aberta_em)
);

CREATE TABLE voto (
    id UUID PRIMARY KEY,
    pauta_id UUID NOT NULL,
    associado_id VARCHAR(100) NOT NULL,
    opcao VARCHAR(3) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_voto_pauta FOREIGN KEY (pauta_id) REFERENCES pauta (id),
    CONSTRAINT uq_voto_pauta_associado UNIQUE (pauta_id, associado_id),
    CONSTRAINT ck_voto_opcao CHECK (opcao IN ('SIM', 'NAO'))
);

CREATE INDEX idx_voto_pauta_opcao ON voto (pauta_id, opcao);

