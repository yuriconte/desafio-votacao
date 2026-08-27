package br.com.somosdb.votacao.voto.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.somosdb.votacao.voto.OpcaoVoto;
import br.com.somosdb.votacao.voto.Voto;

public record VotoResponse(UUID id, UUID pautaId, String associadoId, OpcaoVoto opcao, Instant criadoEm) {

    public static VotoResponse from(Voto voto) {
        return new VotoResponse(
                voto.getId(),
                voto.getPauta().getId(),
                voto.getAssociadoId(),
                voto.getOpcao(),
                voto.getCriadoEm());
    }
}

