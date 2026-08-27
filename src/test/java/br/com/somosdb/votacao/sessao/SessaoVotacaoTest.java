package br.com.somosdb.votacao.sessao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import br.com.somosdb.votacao.pauta.Pauta;

class SessaoVotacaoTest {

    private static final Instant ABERTA_EM = Instant.parse("2026-08-25T12:00:00Z");
    private final Pauta pauta = new Pauta(UUID.randomUUID(), "Pauta", null, ABERTA_EM);
    private final SessaoVotacao sessao = new SessaoVotacao(
            UUID.randomUUID(), pauta, ABERTA_EM, ABERTA_EM.plusSeconds(60));

    @Test
    void deveEstarAbertaNoInstanteInicial() {
        assertThat(sessao.estaAbertaEm(ABERTA_EM)).isTrue();
        assertThat(sessao.statusEm(ABERTA_EM)).isEqualTo(StatusSessao.ABERTA);
    }

    @Test
    void deveEstarEncerradaAntesDaAbertura() {
        assertThat(sessao.estaAbertaEm(ABERTA_EM.minusNanos(1))).isFalse();
        assertThat(sessao.statusEm(ABERTA_EM.minusNanos(1))).isEqualTo(StatusSessao.ENCERRADA);
    }

    @Test
    void deveEstarEncerradaNoInstanteFinal() {
        assertThat(sessao.estaAbertaEm(ABERTA_EM.plusSeconds(60))).isFalse();
        assertThat(sessao.statusEm(ABERTA_EM.plusSeconds(60))).isEqualTo(StatusSessao.ENCERRADA);
    }
}

