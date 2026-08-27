package br.com.somosdb.votacao.voto;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.somosdb.votacao.pauta.Pauta;
import br.com.somosdb.votacao.pauta.PautaService;
import br.com.somosdb.votacao.sessao.SessaoRepository;
import br.com.somosdb.votacao.sessao.SessaoVotacao;
import br.com.somosdb.votacao.shared.error.RegraNegocioException;
import br.com.somosdb.votacao.voto.dto.RegistrarVotoRequest;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@Service
public class VotoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VotoService.class);

    private final PautaService pautaService;
    private final SessaoRepository sessaoRepository;
    private final VotoRepository votoRepository;
    private final Clock clock;

    public VotoService(
            PautaService pautaService,
            SessaoRepository sessaoRepository,
            VotoRepository votoRepository,
            Clock clock) {
        this.pautaService = pautaService;
        this.sessaoRepository = sessaoRepository;
        this.votoRepository = votoRepository;
        this.clock = clock;
    }

    @Transactional
    public VotoResponse registrar(UUID pautaId, RegistrarVotoRequest request) {
        Pauta pauta = pautaService.buscar(pautaId);
        SessaoVotacao sessao = sessaoRepository.findByPautaId(pautaId)
                .orElseThrow(() -> new RegraNegocioException(
                        "SESSAO_NAO_ABERTA", "A pauta ainda não possui uma sessão de votação"));
        Instant agora = Instant.now(clock);
        if (!sessao.estaAbertaEm(agora)) {
            throw new RegraNegocioException("SESSAO_ENCERRADA", "A sessão de votação não está aberta");
        }

        Voto voto = new Voto(
                UUID.randomUUID(),
                pauta,
                request.associadoId().trim(),
                request.opcao(),
                agora);
        Voto votoSalvo = votoRepository.saveAndFlush(voto);
        LOGGER.info("Voto registrado: votoId={}, pautaId={}", votoSalvo.getId(), pautaId);
        return VotoResponse.from(votoSalvo);
    }
}
