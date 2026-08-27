package br.com.somosdb.votacao.voto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import br.com.somosdb.votacao.voto.OpcaoVoto;

public record RegistrarVotoV2Request(
        @NotBlank String associadoId,
        @NotNull OpcaoVoto opcao) {
}
