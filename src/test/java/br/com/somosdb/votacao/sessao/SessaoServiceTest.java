package br.com.somosdb.votacao.sessao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.somosdb.votacao.pauta.Pauta;
import br.com.somosdb.votacao.pauta.PautaService;
import br.com.somosdb.votacao.sessao.dto.AbrirSessaoRequest;
import br.com.somosdb.votacao.sessao.dto.SessaoResponse;
import br.com.somosdb.votacao.shared.error.RegraNegocioException;

@ExtendWith(MockitoExtension.class)
class SessaoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-08-25T12:00:00Z");
    private static final UUID PAUTA_ID = UUID.fromString("0132c84d-eec1-4df5-8511-4ce1fa937d72");

    @Mock
    private SessaoRepository sessaoRepository;

    @Mock
    private PautaService pautaService;

    private SessaoService sessaoService;

    @BeforeEach
    void setUp() {
        sessaoService = new SessaoService(
                sessaoRepository,
                pautaService,
                Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveAbrirSessaoComDuracaoPadrao() {
        prepararPautaDisponivel();

        SessaoResponse response = sessaoService.abrir(PAUTA_ID, new AbrirSessaoRequest(null));

        assertThat(response.pautaId()).isEqualTo(PAUTA_ID);
        assertThat(response.abertaEm()).isEqualTo(AGORA);
        assertThat(response.fechaEm()).isEqualTo(AGORA.plusSeconds(60));
        assertThat(response.duracaoSegundos()).isEqualTo(60);
        assertThat(response.status()).isEqualTo(StatusSessao.ABERTA);
        verify(sessaoRepository).saveAndFlush(any(SessaoVotacao.class));
    }

    @Test
    void deveAbrirSessaoComDuracaoInformada() {
        prepararPautaDisponivel();

        SessaoResponse response = sessaoService.abrir(PAUTA_ID, new AbrirSessaoRequest(300L));

        assertThat(response.fechaEm()).isEqualTo(AGORA.plusSeconds(300));
        assertThat(response.duracaoSegundos()).isEqualTo(300);
    }

    @Test
    void deveRejeitarSegundaSessaoNaMesmaPauta() {
        when(pautaService.buscar(PAUTA_ID)).thenReturn(new Pauta(PAUTA_ID, "Pauta", null, AGORA));
        when(sessaoRepository.existsByPautaId(PAUTA_ID)).thenReturn(true);
        AbrirSessaoRequest request = new AbrirSessaoRequest(null);

        assertThatThrownBy(() -> sessaoService.abrir(PAUTA_ID, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A pauta já possui uma sessão de votação")
                .extracting("codigo")
                .isEqualTo("SESSAO_JA_EXISTE");
    }

    @Test
    void deveRejeitarDuracaoQueUltrapassaInstant() {
        Pauta pauta = new Pauta(PAUTA_ID, "Pauta", null, AGORA);
        when(pautaService.buscar(PAUTA_ID)).thenReturn(pauta);
        when(sessaoRepository.existsByPautaId(PAUTA_ID)).thenReturn(false);
        AbrirSessaoRequest request = new AbrirSessaoRequest(Long.MAX_VALUE);

        assertThatThrownBy(() -> sessaoService.abrir(PAUTA_ID, request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A duração informada ultrapassa o limite temporal")
                .extracting("codigo")
                .isEqualTo("DURACAO_INVALIDA");
    }

    private void prepararPautaDisponivel() {
        Pauta pauta = new Pauta(PAUTA_ID, "Pauta", null, AGORA);
        when(pautaService.buscar(PAUTA_ID)).thenReturn(pauta);
        when(sessaoRepository.existsByPautaId(PAUTA_ID)).thenReturn(false);
        when(sessaoRepository.saveAndFlush(any(SessaoVotacao.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
