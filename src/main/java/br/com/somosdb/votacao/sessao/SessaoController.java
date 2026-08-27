package br.com.somosdb.votacao.sessao;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.somosdb.votacao.sessao.dto.AbrirSessaoRequest;
import br.com.somosdb.votacao.sessao.dto.SessaoResponse;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/sessoes")
public class SessaoController {

    private final SessaoService sessaoService;

    public SessaoController(SessaoService sessaoService) {
        this.sessaoService = sessaoService;
    }

    @PostMapping
    ResponseEntity<SessaoResponse> abrir(
            @PathVariable UUID pautaId,
            @Valid @RequestBody(required = false) AbrirSessaoRequest request) {
        AbrirSessaoRequest requestEfetivo = request == null ? new AbrirSessaoRequest(null) : request;
        SessaoResponse response = sessaoService.abrir(pautaId, requestEfetivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
