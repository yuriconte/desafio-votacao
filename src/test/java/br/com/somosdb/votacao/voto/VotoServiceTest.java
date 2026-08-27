package br.com.somosdb.votacao.voto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.somosdb.votacao.pauta.Pauta;
import br.com.somosdb.votacao.pauta.PautaService;
import br.com.somosdb.votacao.sessao.SessaoRepository;
import br.com.somosdb.votacao.sessao.SessaoVotacao;
import br.com.somosdb.votacao.shared.error.RegraNegocioException;
import br.com.somosdb.votacao.voto.dto.RegistrarVotoRequest;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@ExtendWith(MockitoExtension.class)
class VotoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID PAUTA_ID = UUID.fromString("e6e3dab1-1829-49e6-978e-cb26ff6f645c");

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    private VotoService votoService;
    private Pauta pauta;

    @BeforeEach
    void setUp() {
        pauta = new Pauta(PAUTA_ID, "Pauta", null, AGORA.minusSeconds(10));
        votoService = new VotoService(
                pautaService,
                sessaoRepository,
                votoRepository,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        when(pautaService.buscar(PAUTA_ID)).thenReturn(pauta);
    }

    @Test
    void deveRegistrarVotoDuranteSessao() {
        SessaoVotacao sessao = sessao(AGORA.minusSeconds(10), AGORA.plusSeconds(10));
        when(sessaoRepository.findByPautaId(PAUTA_ID)).thenReturn(Optional.of(sessao));
        when(votoRepository.saveAndFlush(any(Voto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        VotoResponse response = votoService.registrar(
                PAUTA_ID,
                new RegistrarVotoRequest("  associado-1  ", OpcaoVoto.SIM));

        assertThat(response.id()).isNotNull();
        assertThat(response.pautaId()).isEqualTo(PAUTA_ID);
        assertThat(response.associadoId()).isEqualTo("associado-1");
        assertThat(response.opcao()).isEqualTo(OpcaoVoto.SIM);
        assertThat(response.criadoEm()).isEqualTo(AGORA);
    }

    @Test
    void deveRejeitarVotoSemSessao() {
        when(sessaoRepository.findByPautaId(PAUTA_ID)).thenReturn(Optional.empty());
        RegistrarVotoRequest request = new RegistrarVotoRequest("associado", OpcaoVoto.NAO);

        assertThatThrownBy(() -> votoService.registrar(PAUTA_ID, request))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo")
                .isEqualTo("SESSAO_NAO_ABERTA");
        verify(votoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarVotoDepoisDoEncerramento() {
        when(sessaoRepository.findByPautaId(PAUTA_ID))
                .thenReturn(Optional.of(sessao(AGORA.minusSeconds(20), AGORA)));
        RegistrarVotoRequest request = new RegistrarVotoRequest("associado", OpcaoVoto.NAO);

        assertThatThrownBy(() -> votoService.registrar(PAUTA_ID, request))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo")
                .isEqualTo("SESSAO_ENCERRADA");
        verify(votoRepository, never()).saveAndFlush(any());
    }

    @Test
    void deveRejeitarVotoAntesDaAbertura() {
        when(sessaoRepository.findByPautaId(PAUTA_ID))
                .thenReturn(Optional.of(sessao(AGORA.plusSeconds(1), AGORA.plusSeconds(20))));
        RegistrarVotoRequest request = new RegistrarVotoRequest("associado", OpcaoVoto.SIM);

        assertThatThrownBy(() -> votoService.registrar(PAUTA_ID, request))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo")
                .isEqualTo("SESSAO_ENCERRADA");
    }

    private SessaoVotacao sessao(Instant abertaEm, Instant fechaEm) {
        return new SessaoVotacao(UUID.randomUUID(), pauta, abertaEm, fechaEm);
    }
}
