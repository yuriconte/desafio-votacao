package br.com.somosdb.votacao.elegibilidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.random.RandomGenerator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsultaElegibilidadeFakeTest {

    @Mock
    private RandomGenerator randomGenerator;

    @Test
    void deveRetornarHabilitadoQuandoSorteioForVerdadeiro() {
        when(randomGenerator.nextBoolean()).thenReturn(true);
        ConsultaElegibilidadeFake consulta = new ConsultaElegibilidadeFake(randomGenerator);
        Cpf cpf = Cpf.criar("52998224725").orElseThrow();

        assertThat(consulta.consultar(cpf)).isEqualTo(StatusElegibilidade.ABLE_TO_VOTE);
    }

    @Test
    void deveRetornarNaoHabilitadoQuandoSorteioForFalso() {
        when(randomGenerator.nextBoolean()).thenReturn(false);
        ConsultaElegibilidadeFake consulta = new ConsultaElegibilidadeFake(randomGenerator);
        Cpf cpf = Cpf.criar("52998224725").orElseThrow();

        assertThat(consulta.consultar(cpf)).isEqualTo(StatusElegibilidade.UNABLE_TO_VOTE);
    }
}
