package br.com.somosdb.votacao;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import br.com.somosdb.votacao.pauta.PautaRepository;
import br.com.somosdb.votacao.pauta.dto.CriarPautaRequest;
import br.com.somosdb.votacao.pauta.dto.PautaResponse;
import br.com.somosdb.votacao.resultado.Resultado;
import br.com.somosdb.votacao.resultado.dto.ResultadoResponse;
import br.com.somosdb.votacao.sessao.SessaoRepository;
import br.com.somosdb.votacao.sessao.StatusSessao;
import br.com.somosdb.votacao.sessao.dto.AbrirSessaoRequest;
import br.com.somosdb.votacao.sessao.dto.SessaoResponse;
import br.com.somosdb.votacao.voto.OpcaoVoto;
import br.com.somosdb.votacao.voto.VotoRepository;
import br.com.somosdb.votacao.voto.dto.RegistrarVotoRequest;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@Testcontainers
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@Import(VotacaoApplicationIT.ClockTestConfiguration.class)
class VotacaoApplicationIT {

    private static final Instant INICIO = Instant.parse("2026-08-25T12:00:00Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17.11-alpine");

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private MutableClock clock;

    @Autowired
    private VotoRepository votoRepository;

    @Autowired
    private SessaoRepository sessaoRepository;

    @Autowired
    private PautaRepository pautaRepository;

    @BeforeEach
    void limparBanco() {
        votoRepository.deleteAll();
        sessaoRepository.deleteAll();
        pautaRepository.deleteAll();
        clock.setInstant(INICIO);
    }

    @Test
    void deveExecutarFluxoCompletoEManterResultado() {
        PautaResponse pauta = criarPauta("Investimento anual");
        SessaoResponse sessao = abrirSessao(pauta.id(), 120L);

        assertThat(sessao.duracaoSegundos()).isEqualTo(120);
        assertThat(sessao.status()).isEqualTo(StatusSessao.ABERTA);

        ResponseEntity<VotoResponse> voto = votar(pauta.id(), "associado-1", OpcaoVoto.SIM);
        assertThat(voto.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResultadoResponse parcial = consultarResultado(pauta.id());
        assertThat(parcial.resultado()).isEqualTo(Resultado.EM_ANDAMENTO);
        assertThat(parcial.votosSim()).isEqualTo(1);

        clock.advance(Duration.ofSeconds(120));

        ResponseEntity<String> votoAposEncerramento = restTemplate.postForEntity(
                "/api/v1/pautas/{id}/votos",
                new RegistrarVotoRequest("associado-2", OpcaoVoto.NAO),
                String.class,
                pauta.id());
        assertThat(votoAposEncerramento.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(votoAposEncerramento.getBody()).contains("SESSAO_ENCERRADA");

        ResultadoResponse finalizado = consultarResultado(pauta.id());
        assertThat(finalizado.status()).isEqualTo(StatusSessao.ENCERRADA);
        assertThat(finalizado.resultado()).isEqualTo(Resultado.APROVADA);
        assertThat(finalizado.totalVotos()).isEqualTo(1);
    }

    @Test
    void deveAplicarDuracaoPadraoSemCorpo() {
        PautaResponse pauta = criarPauta("Pauta padrão");

        ResponseEntity<SessaoResponse> response = restTemplate.postForEntity(
                "/api/v1/pautas/{id}/sessoes", null, SessaoResponse.class, pauta.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().duracaoSegundos()).isEqualTo(60);
    }

    @Test
    void deveImpedirVotoDuplicadoMesmoComRequisicoesConcorrentes() throws Exception {
        PautaResponse pauta = criarPauta("Concorrência");
        abrirSessao(pauta.id(), 60L);
        int quantidade = 8;
        CountDownLatch inicioSimultaneo = new CountDownLatch(1);
        List<Callable<Integer>> tarefas = new ArrayList<>();
        for (int indice = 0; indice < quantidade; indice++) {
            tarefas.add(() -> {
                inicioSimultaneo.await();
                return restTemplate.postForEntity(
                        "/api/v1/pautas/{id}/votos",
                        new RegistrarVotoRequest("mesmo-associado", OpcaoVoto.SIM),
                        String.class,
                        pauta.id()).getStatusCode().value();
            });
        }

        List<Integer> statusRecebidos;
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var resultados = tarefas.stream().map(executor::submit).toList();
            inicioSimultaneo.countDown();
            statusRecebidos = new ArrayList<>();
            for (var resultado : resultados) {
                statusRecebidos.add(resultado.get());
            }
        }

        assertThat(statusRecebidos).filteredOn(status -> status == HttpStatus.CREATED.value()).hasSize(1);
        assertThat(statusRecebidos).filteredOn(status -> status == HttpStatus.CONFLICT.value()).hasSize(quantidade - 1);
        assertThat(votoRepository.count()).isEqualTo(1);
    }

    @Test
    void deveRejeitarSegundaSessaoPersistida() {
        PautaResponse pauta = criarPauta("Sessão única");
        abrirSessao(pauta.id(), 60L);

        ResponseEntity<String> response = restTemplate.postForEntity(
                "/api/v1/pautas/{id}/sessoes",
                new AbrirSessaoRequest(60L),
                String.class,
                pauta.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).contains("SESSAO_JA_EXISTE");
    }

    private PautaResponse criarPauta(String titulo) {
        ResponseEntity<PautaResponse> response = restTemplate.postForEntity(
                "/api/v1/pautas",
                new CriarPautaRequest(titulo, null),
                PautaResponse.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private SessaoResponse abrirSessao(UUID pautaId, Long duracao) {
        ResponseEntity<SessaoResponse> response = restTemplate.postForEntity(
                "/api/v1/pautas/{id}/sessoes",
                new AbrirSessaoRequest(duracao),
                SessaoResponse.class,
                pautaId);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody();
    }

    private ResponseEntity<VotoResponse> votar(UUID pautaId, String associadoId, OpcaoVoto opcao) {
        return restTemplate.postForEntity(
                "/api/v1/pautas/{id}/votos",
                new RegistrarVotoRequest(associadoId, opcao),
                VotoResponse.class,
                pautaId);
    }

    private ResultadoResponse consultarResultado(UUID pautaId) {
        return restTemplate.getForObject(
                "/api/v1/pautas/{id}/resultado", ResultadoResponse.class, Map.of("id", pautaId));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ClockTestConfiguration {

        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(INICIO);
        }
    }

    static final class MutableClock extends Clock {

        private Instant instant;

        MutableClock(Instant instant) {
            this.instant = instant;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return zone.equals(ZoneOffset.UTC) ? this : Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
