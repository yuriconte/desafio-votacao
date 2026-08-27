package br.com.somosdb.votacao.resultado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import br.com.somosdb.votacao.resultado.dto.ResultadoResponse;
import br.com.somosdb.votacao.sessao.SessaoRepository;
import br.com.somosdb.votacao.sessao.SessaoVotacao;
import br.com.somosdb.votacao.sessao.StatusSessao;
import br.com.somosdb.votacao.shared.error.RegraNegocioException;
import br.com.somosdb.votacao.voto.ResumoVotos;
import br.com.somosdb.votacao.voto.VotoRepository;

@ExtendWith(MockitoExtension.class)
class ResultadoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID PAUTA_ID = UUID.fromString("b03b5e00-600b-4aba-bade-2cf19839ddda");

    @Mock
    private PautaService pautaService;

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private VotoRepository votoRepository;

    @Mock
    private ResumoVotos resumo;

    private ResultadoService resultadoService;
    private Pauta pauta;

    @BeforeEach
    void setUp() {
        pauta = new Pauta(PAUTA_ID, "Pauta", null, AGORA.minusSeconds(100));
        resultadoService = new ResultadoService(
                pautaService,
                sessaoRepository,
                votoRepository,
                Clock.fixed(AGORA, ZoneOffset.UTC));
        when(pautaService.buscar(PAUTA_ID)).thenReturn(pauta);
    }

    @Test
    void deveRetornarResultadoEmAndamento() {
        prepararResultado(AGORA.plusSeconds(60), 2, 1);

        ResultadoResponse response = resultadoService.consultar(PAUTA_ID);

        assertThat(response.status()).isEqualTo(StatusSessao.ABERTA);
        assertThat(response.resultado()).isEqualTo(Resultado.EM_ANDAMENTO);
        assertThat(response.totalVotos()).isEqualTo(3);
    }

    @Test
    void deveRetornarPautaAprovada() {
        prepararResultado(AGORA, 3, 2);

        assertThat(resultadoService.consultar(PAUTA_ID).resultado()).isEqualTo(Resultado.APROVADA);
    }

    @Test
    void deveRetornarPautaRejeitada() {
        prepararResultado(AGORA.minusNanos(1), 1, 2);

        assertThat(resultadoService.consultar(PAUTA_ID).resultado()).isEqualTo(Resultado.REJEITADA);
    }

    @Test
    void deveRetornarEmpateInclusiveSemVotos() {
        prepararResultado(AGORA.minusSeconds(1), 0, 0);

        assertThat(resultadoService.consultar(PAUTA_ID).resultado()).isEqualTo(Resultado.EMPATE);
    }

    @Test
    void deveRejeitarConsultaSemSessao() {
        when(sessaoRepository.findByPautaId(PAUTA_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> resultadoService.consultar(PAUTA_ID))
                .isInstanceOf(RegraNegocioException.class)
                .extracting("codigo")
                .isEqualTo("SESSAO_NAO_ABERTA");
    }

    private void prepararResultado(Instant fechaEm, long votosSim, long votosNao) {
        SessaoVotacao sessao = new SessaoVotacao(
                UUID.randomUUID(), pauta, AGORA.minusSeconds(60), fechaEm);
        when(sessaoRepository.findByPautaId(PAUTA_ID)).thenReturn(Optional.of(sessao));
        when(votoRepository.resumirPorPauta(PAUTA_ID)).thenReturn(resumo);
        when(resumo.getVotosSim()).thenReturn(votosSim);
        when(resumo.getVotosNao()).thenReturn(votosNao);
        when(resumo.getTotal()).thenReturn(votosSim + votosNao);
    }
}

