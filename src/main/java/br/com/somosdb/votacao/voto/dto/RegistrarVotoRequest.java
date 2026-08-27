package br.com.somosdb.votacao.voto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import br.com.somosdb.votacao.voto.OpcaoVoto;

public record RegistrarVotoRequest(
        @NotBlank @Size(max = 100) String associadoId,
        @NotNull OpcaoVoto opcao) {
}

