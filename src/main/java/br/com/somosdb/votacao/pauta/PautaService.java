package br.com.somosdb.votacao.pauta;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.somosdb.votacao.pauta.dto.CriarPautaRequest;
import br.com.somosdb.votacao.pauta.dto.PautaResponse;
import br.com.somosdb.votacao.shared.error.RecursoNaoEncontradoException;

@Service
public class PautaService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PautaService.class);

    private final PautaRepository pautaRepository;
    private final Clock clock;

    public PautaService(PautaRepository pautaRepository, Clock clock) {
        this.pautaRepository = pautaRepository;
        this.clock = clock;
    }

    @Transactional
    public PautaResponse criar(CriarPautaRequest request) {
        String descricao = request.descricao() == null ? null : request.descricao().trim();
        Pauta pauta = new Pauta(UUID.randomUUID(), request.titulo().trim(), descricao, Instant.now(clock));
        Pauta pautaSalva = pautaRepository.save(pauta);
        LOGGER.info("Pauta criada: pautaId={}", pautaSalva.getId());
        return PautaResponse.from(pautaSalva);
    }

    @Transactional(readOnly = true)
    public Pauta buscar(UUID pautaId) {
        return pautaRepository.findById(pautaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("PAUTA_NAO_ENCONTRADA", "Pauta não encontrada"));
    }
}
