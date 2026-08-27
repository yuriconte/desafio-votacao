package br.com.somosdb.votacao.resultado;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.somosdb.votacao.pauta.PautaService;
import br.com.somosdb.votacao.resultado.dto.ResultadoResponse;
import br.com.somosdb.votacao.sessao.SessaoRepository;
import br.com.somosdb.votacao.sessao.SessaoVotacao;
import br.com.somosdb.votacao.sessao.StatusSessao;
import br.com.somosdb.votacao.shared.error.RegraNegocioException;
import br.com.somosdb.votacao.voto.ResumoVotos;
import br.com.somosdb.votacao.voto.VotoRepository;

@Service
public class ResultadoService {

    private final PautaService pautaService;
    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;

    public ResultadoService(
            PautaService pautaService,
            SessaoRepository sessaoRepository,
            VotoRepository votoRepository,
            Clock clock) {
        this.pautaService = pautaService;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ResultadoResponse consultar(UUID pautaId) {
        pautaService.buscar(pautaId);
        SessaoVotacao sessao = sessaoRepository.findByPautaId(pautaId)
                .orElseThrow(() -> new RegraNegocioException(
                        "SESSAO_NAO_ABERTA", "A pauta ainda não possui uma sessão de votação"));
        ResumoVotos resumo = votoRepository.resumirPorPauta(pautaId);
        Instant agora = Instant.now(clock);
        StatusSessao status = sessao.statusEm(agora);
        Resultado resultado = calcularResultado(status, resumo.getVotosSim(), resumo.getVotosNao());
        return new ResultadoResponse(
                pautaId,
                status,
                resumo.getVotosSim(),
                resumo.getVotosNao(),
                resumo.getTotal(),
                resultado,
                sessao.getAbertaEm(),
                sessao.getFechaEm());
    }

    static Resultado calcularResultado(StatusSessao status, long votosSim, long votosNao) {
        if (status == StatusSessao.ABERTA) {
            return Resultado.EM_ANDAMENTO;
        }
        if (votosSim > votosNao) {
            return Resultado.APROVADA;
        }
        if (votosNao > votosSim) {
            return Resultado.REJEITADA;
        }
        return Resultado.EMPATE;
    }
}

