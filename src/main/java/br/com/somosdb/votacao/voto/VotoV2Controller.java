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

import br.com.somosdb.votacao.voto.dto.RegistrarVotoV2Request;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@RestController
@RequestMapping("/api/v2/pautas/{pautaId}/votos")
public class VotoV2Controller {

    private final VotoV2Service votoV2Service;

    public VotoV2Controller(VotoV2Service votoV2Service) {
        this.votoV2Service = votoV2Service;
    }

    @PostMapping
    ResponseEntity<VotoResponse> registrar(
            @PathVariable UUID pautaId,
            @Valid @RequestBody RegistrarVotoV2Request request) {
        VotoResponse response = votoV2Service.registrar(pautaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
