package br.com.somosdb.votacao.sessao;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.somosdb.votacao.pauta.Pauta;
import br.com.somosdb.votacao.pauta.PautaService;
import br.com.somosdb.votacao.sessao.dto.AbrirSessaoRequest;
import br.com.somosdb.votacao.sessao.dto.SessaoResponse;
import br.com.somosdb.votacao.shared.error.RegraNegocioException;

@Service
public class SessaoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessaoService.class);
    static final long DURACAO_PADRAO_SEGUNDOS = 60;

    private final SessaoRepository sessaoRepository;
    private final PautaService pautaService;
    private final Clock clock;

    public SessaoService(SessaoRepository sessaoRepository, PautaService pautaService, Clock clock) {
        this.sessaoRepository = sessaoRepository;
        this.pautaService = pautaService;
        this.clock = clock;
    }

    @Transactional
    public SessaoResponse abrir(UUID pautaId, AbrirSessaoRequest request) {
        Pauta pauta = pautaService.buscar(pautaId);
        if (sessaoRepository.existsByPautaId(pautaId)) {
            throw new RegraNegocioException("SESSAO_JA_EXISTE", "A pauta já possui uma sessão de votação");
        }

        long duracaoSegundos = request.duracaoSegundos() == null
                ? DURACAO_PADRAO_SEGUNDOS
                : request.duracaoSegundos();
        Instant abertaEm = Instant.now(clock);
        Instant fechaEm;
        try {
            fechaEm = abertaEm.plusSeconds(duracaoSegundos);
        } catch (DateTimeException | ArithmeticException exception) {
            throw new RegraNegocioException("DURACAO_INVALIDA", "A duração informada ultrapassa o limite temporal");
        }

        SessaoVotacao sessao = new SessaoVotacao(UUID.randomUUID(), pauta, abertaEm, fechaEm);
        SessaoVotacao sessaoSalva = sessaoRepository.saveAndFlush(sessao);
        LOGGER.info("Sessao aberta: sessaoId={}, pautaId={}, fechaEm={}", sessaoSalva.getId(), pautaId, fechaEm);
        return SessaoResponse.from(sessaoSalva, abertaEm);
    }
}
