package br.com.somosdb.votacao.voto;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.somosdb.votacao.voto.dto.RegistrarVotoRequest;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/votos")
public class VotoController {

    private final VotoService votoService;

    public VotoController(VotoService votoService) {
        this.votoService = votoService;
    }

    @PostMapping
    ResponseEntity<VotoResponse> registrar(
            @PathVariable UUID pautaId,
            @Valid @RequestBody RegistrarVotoRequest request) {
        VotoResponse response = votoService.registrar(pautaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
