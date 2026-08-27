package br.com.somosdb.votacao.sessao.dto;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import br.com.somosdb.votacao.sessao.SessaoVotacao;
import br.com.somosdb.votacao.sessao.StatusSessao;

public record SessaoResponse(
        UUID id,
        UUID pautaId,
        Instant abertaEm,
        Instant fechaEm,
        long duracaoSegundos,
        StatusSessao status) {

    public static SessaoResponse from(SessaoVotacao sessao, Instant agora) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getPauta().getId(),
                sessao.getAbertaEm(),
                sessao.getFechaEm(),
                Duration.between(sessao.getAbertaEm(), sessao.getFechaEm()).toSeconds(),
                sessao.statusEm(agora));
    }
}

