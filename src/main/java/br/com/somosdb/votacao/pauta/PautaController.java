package br.com.somosdb.votacao.pauta;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.somosdb.votacao.pauta.dto.CriarPautaRequest;
import br.com.somosdb.votacao.pauta.dto.PautaResponse;

@RestController
@RequestMapping("/api/v1/pautas")
public class PautaController {

    private final PautaService pautaService;

    public PautaController(PautaService pautaService) {
        this.pautaService = pautaService;
    }

    @PostMapping
    ResponseEntity<PautaResponse> criar(@Valid @RequestBody CriarPautaRequest request) {
        PautaResponse response = pautaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
