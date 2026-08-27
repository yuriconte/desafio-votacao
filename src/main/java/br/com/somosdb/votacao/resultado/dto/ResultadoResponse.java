package br.com.somosdb.votacao.resultado.dto;

import java.time.Instant;
import java.util.UUID;

import br.com.somosdb.votacao.resultado.Resultado;
import br.com.somosdb.votacao.sessao.StatusSessao;

public record ResultadoResponse(
        UUID pautaId,
        StatusSessao status,
        long votosSim,
        long votosNao,
        long totalVotos,
        Resultado resultado,
        Instant abertaEm,
        Instant fechaEm) {
}

