package br.com.somosdb.votacao.elegibilidade;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CpfTest {

    @Test
    void deveAceitarCpfSemFormatacao() {
        assertThat(Cpf.criar("52998224725"))
                .get()
                .extracting(Cpf::numero)
                .isEqualTo("52998224725");
    }

    @Test
    void deveNormalizarCpfFormatado() {
        assertThat(Cpf.criar("529.982.247-25"))
                .get()
                .extracting(Cpf::numero)
                .isEqualTo("52998224725");
    }

    @Test
    void deveRejeitarCpfComDigitoInvalido() {
        assertThat(Cpf.criar("52998224724")).isEmpty();
    }

    @Test
    void deveRejeitarCpfComTodosDigitosIguais() {
        assertThat(Cpf.criar("11111111111")).isEmpty();
    }

    @Test
    void deveRejeitarFormatoNaoReconhecido() {
        assertThat(Cpf.criar("cpf-52998224725")).isEmpty();
    }

    @Test
    void deveRejeitarCpfNulo() {
        assertThat(Cpf.criar(null)).isEmpty();
    }
}
