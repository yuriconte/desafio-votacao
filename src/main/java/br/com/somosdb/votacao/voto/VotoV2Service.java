package br.com.somosdb.votacao.voto;

import java.util.UUID;

import org.springframework.stereotype.Service;

import br.com.somosdb.votacao.elegibilidade.ConsultaElegibilidade;
import br.com.somosdb.votacao.elegibilidade.Cpf;
import br.com.somosdb.votacao.elegibilidade.StatusElegibilidade;
import br.com.somosdb.votacao.shared.error.AssociadoNaoPodeVotarException;
import br.com.somosdb.votacao.shared.error.RecursoNaoEncontradoException;
import br.com.somosdb.votacao.voto.dto.RegistrarVotoRequest;
import br.com.somosdb.votacao.voto.dto.RegistrarVotoV2Request;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@Service
public class VotoV2Service {

    private final VotoService votoService;
    private final ConsultaElegibilidade consultaElegibilidade;

    public VotoV2Service(VotoService votoService, ConsultaElegibilidade consultaElegibilidade) {
        this.votoService = votoService;
        this.consultaElegibilidade = consultaElegibilidade;
    }

    public VotoResponse registrar(UUID pautaId, RegistrarVotoV2Request request) {
        Cpf cpf = Cpf.criar(request.associadoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("CPF_INVALIDO", "CPF inválido"));
        StatusElegibilidade status = consultaElegibilidade.consultar(cpf);
        if (status == StatusElegibilidade.UNABLE_TO_VOTE) {
            throw new AssociadoNaoPodeVotarException();
        }
        return votoService.registrar(
                pautaId,
                new RegistrarVotoRequest(cpf.numero(), request.opcao()));
    }
}
