package br.com.somosdb.votacao.voto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import br.com.somosdb.votacao.elegibilidade.ConsultaElegibilidade;
import br.com.somosdb.votacao.elegibilidade.Cpf;
import br.com.somosdb.votacao.elegibilidade.StatusElegibilidade;
import br.com.somosdb.votacao.shared.error.AssociadoNaoPodeVotarException;
import br.com.somosdb.votacao.shared.error.RecursoNaoEncontradoException;
import br.com.somosdb.votacao.voto.dto.RegistrarVotoRequest;
import br.com.somosdb.votacao.voto.dto.RegistrarVotoV2Request;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@ExtendWith(MockitoExtension.class)
class VotoV2ServiceTest {

    private static final UUID PAUTA_ID = UUID.fromString("e6e3dab1-1829-49e6-978e-cb26ff6f645c");
    private static final String CPF = "52998224725";

    @Mock
    private VotoService votoService;

    @Mock
    private ConsultaElegibilidade consultaElegibilidade;

    private VotoV2Service votoV2Service;

    @BeforeEach
    void setUp() {
        votoV2Service = new VotoV2Service(votoService, consultaElegibilidade);
    }

    @Test
    void deveRejeitarCpfInvalidoAntesDaConsulta() {
        RegistrarVotoV2Request request = new RegistrarVotoV2Request("11111111111", OpcaoVoto.SIM);

        assertThatThrownBy(() -> votoV2Service.registrar(PAUTA_ID, request))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .extracting("codigo")
                .isEqualTo("CPF_INVALIDO");
        verify(consultaElegibilidade, never()).consultar(org.mockito.ArgumentMatchers.any());
        verify(votoService, never()).registrar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveRejeitarAssociadoNaoHabilitado() {
        RegistrarVotoV2Request request = new RegistrarVotoV2Request(CPF, OpcaoVoto.SIM);
        when(consultaElegibilidade.consultar(org.mockito.ArgumentMatchers.any(Cpf.class)))
                .thenReturn(StatusElegibilidade.UNABLE_TO_VOTE);

        assertThatThrownBy(() -> votoV2Service.registrar(PAUTA_ID, request))
                .isInstanceOf(AssociadoNaoPodeVotarException.class);
        verify(votoService, never()).registrar(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deveNormalizarCpfERegistrarAssociadoHabilitado() {
        RegistrarVotoV2Request request = new RegistrarVotoV2Request("529.982.247-25", OpcaoVoto.NAO);
        when(consultaElegibilidade.consultar(org.mockito.ArgumentMatchers.any(Cpf.class)))
                .thenReturn(StatusElegibilidade.ABLE_TO_VOTE);
        VotoResponse esperado = new VotoResponse(UUID.randomUUID(), PAUTA_ID, CPF, OpcaoVoto.NAO, Instant.now());
        when(votoService.registrar(org.mockito.ArgumentMatchers.eq(PAUTA_ID), org.mockito.ArgumentMatchers.any()))
                .thenReturn(esperado);

        VotoResponse response = votoV2Service.registrar(PAUTA_ID, request);

        ArgumentCaptor<RegistrarVotoRequest> captor = ArgumentCaptor.forClass(RegistrarVotoRequest.class);
        verify(votoService).registrar(org.mockito.ArgumentMatchers.eq(PAUTA_ID), captor.capture());
        assertThat(captor.getValue().associadoId()).isEqualTo(CPF);
        assertThat(captor.getValue().opcao()).isEqualTo(OpcaoVoto.NAO);
        assertThat(response).isSameAs(esperado);
    }
}
