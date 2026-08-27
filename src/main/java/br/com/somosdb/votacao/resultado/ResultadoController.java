package br.com.somosdb.votacao.resultado;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.somosdb.votacao.resultado.dto.ResultadoResponse;

@RestController
@RequestMapping("/api/v1/pautas/{pautaId}/resultado")
public class ResultadoController {

    private final ResultadoService resultadoService;

    public ResultadoController(ResultadoService resultadoService) {
        this.resultadoService = resultadoService;
    }

    @GetMapping
    ResultadoResponse consultar(@PathVariable UUID pautaId) {
        return resultadoService.consultar(pautaId);
    }
}

