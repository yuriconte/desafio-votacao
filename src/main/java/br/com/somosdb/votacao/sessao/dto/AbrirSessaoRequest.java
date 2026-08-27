package br.com.somosdb.votacao.sessao.dto;

import jakarta.validation.constraints.Positive;

public record AbrirSessaoRequest(@Positive Long duracaoSegundos) {
}

