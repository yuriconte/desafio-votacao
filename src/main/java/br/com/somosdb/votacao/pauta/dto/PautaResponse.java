package br.com.somosdb.votacao.pauta.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.somosdb.votacao.pauta.Pauta;

public record PautaResponse(UUID id, String titulo, String descricao, Instant criadaEm) {

    public static PautaResponse from(Pauta pauta) {
        return new PautaResponse(pauta.getId(), pauta.getTitulo(), pauta.getDescricao(), pauta.getCriadaEm());
    }
}

