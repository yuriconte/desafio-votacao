package br.com.somosdb.votacao.api;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.somosdb.votacao.pauta.PautaController;
import br.com.somosdb.votacao.pauta.PautaService;
import br.com.somosdb.votacao.pauta.dto.PautaResponse;
import br.com.somosdb.votacao.resultado.Resultado;
import br.com.somosdb.votacao.resultado.ResultadoController;
import br.com.somosdb.votacao.resultado.ResultadoService;
import br.com.somosdb.votacao.resultado.dto.ResultadoResponse;
import br.com.somosdb.votacao.sessao.SessaoController;
import br.com.somosdb.votacao.sessao.SessaoService;
import br.com.somosdb.votacao.sessao.StatusSessao;
import br.com.somosdb.votacao.sessao.dto.SessaoResponse;
import br.com.somosdb.votacao.shared.error.RecursoNaoEncontradoException;
import br.com.somosdb.votacao.shared.error.RegraNegocioException;
import br.com.somosdb.votacao.voto.OpcaoVoto;
import br.com.somosdb.votacao.voto.VotoController;
import br.com.somosdb.votacao.voto.VotoService;
import br.com.somosdb.votacao.voto.dto.VotoResponse;

@WebMvcTest({PautaController.class, SessaoController.class, VotoController.class, ResultadoController.class})
class ApiControllerTest {

    private static final UUID PAUTA_ID = UUID.fromString("c073f00d-a450-45d9-bc08-9a1713a35de3");
    private static final UUID RECURSO_ID = UUID.fromString("7b1a60f6-3c78-401b-a855-67cbd51f015e");
    private static final Instant AGORA = Instant.parse("2026-08-25T12:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PautaService pautaService;

    @MockitoBean
    private SessaoService sessaoService;

    @MockitoBean
    private VotoService votoService;

    @MockitoBean
    private ResultadoService resultadoService;

    @Test
    void deveCriarPauta() throws Exception {
        when(pautaService.criar(any())).thenReturn(new PautaResponse(RECURSO_ID, "Pauta", null, AGORA));

        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"Pauta\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(RECURSO_ID.toString()))
                .andExpect(jsonPath("$.titulo").value("Pauta"));
    }

    @Test
    void deveValidarPauta() throws Exception {
        mockMvc.perform(post("/api/v1/pautas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.violacoes", hasSize(1)))
                .andExpect(jsonPath("$.violacoes[0].campo").value("titulo"));
    }

    @Test
    void deveAbrirSessaoComCorpoAusente() throws Exception {
        SessaoResponse response = new SessaoResponse(
                RECURSO_ID, PAUTA_ID, AGORA, AGORA.plusSeconds(60), 60, StatusSessao.ABERTA);
        when(sessaoService.abrir(eq(PAUTA_ID), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/pautas/{id}/sessoes", PAUTA_ID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.duracaoSegundos").value(60));
    }

    @Test
    void deveValidarDuracaoDaSessao() throws Exception {
        mockMvc.perform(post("/api/v1/pautas/{id}/sessoes", PAUTA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"duracaoSegundos\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"));
    }

    @Test
    void deveRegistrarVoto() throws Exception {
        when(votoService.registrar(eq(PAUTA_ID), any())).thenReturn(new VotoResponse(
                RECURSO_ID, PAUTA_ID, "associado-1", OpcaoVoto.SIM, AGORA));

        mockMvc.perform(post("/api/v1/pautas/{id}/votos", PAUTA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"associado-1\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.opcao").value("SIM"));
    }

    @Test
    void deveRejeitarOpcaoDeVotoDesconhecida() throws Exception {
        mockMvc.perform(post("/api/v1/pautas/{id}/votos", PAUTA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"associado-1\",\"opcao\":\"TALVEZ\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("JSON_INVALIDO"));
    }

    @Test
    void deveConsultarResultado() throws Exception {
        when(resultadoService.consultar(PAUTA_ID)).thenReturn(new ResultadoResponse(
                PAUTA_ID, StatusSessao.ENCERRADA, 2, 1, 3, Resultado.APROVADA,
                AGORA.minusSeconds(60), AGORA));

        mockMvc.perform(get("/api/v1/pautas/{id}/resultado", PAUTA_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVotos").value(3))
                .andExpect(jsonPath("$.resultado").value("APROVADA"));
    }

    @Test
    void deveRetornarNotFoundPadronizado() throws Exception {
        when(resultadoService.consultar(PAUTA_ID)).thenThrow(
                new RecursoNaoEncontradoException("PAUTA_NAO_ENCONTRADA", "Pauta não encontrada"));

        mockMvc.perform(get("/api/v1/pautas/{id}/resultado", PAUTA_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("PAUTA_NAO_ENCONTRADA"))
                .andExpect(jsonPath("$.detail").value("Pauta não encontrada"))
                .andExpect(jsonPath("$.instance").value("/api/v1/pautas/" + PAUTA_ID + "/resultado"));
    }

    @Test
    void deveRetornarConflitoDeRegraPadronizado() throws Exception {
        when(resultadoService.consultar(PAUTA_ID)).thenThrow(
                new RegraNegocioException("SESSAO_NAO_ABERTA", "Sessão ausente"));

        mockMvc.perform(get("/api/v1/pautas/{id}/resultado", PAUTA_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("SESSAO_NAO_ABERTA"));
    }

    @Test
    void deveRetornarConflitoDeIntegridadePadronizado() throws Exception {
        when(votoService.registrar(eq(PAUTA_ID), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        mockMvc.perform(post("/api/v1/pautas/{id}/votos", PAUTA_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"associadoId\":\"associado-1\",\"opcao\":\"SIM\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CONFLITO_DE_INTEGRIDADE"));
    }

    @Test
    void deveTratarConstraintViolation() throws Exception {
        when(resultadoService.consultar(PAUTA_ID)).thenThrow(new ConstraintViolationException(Set.of()));

        mockMvc.perform(get("/api/v1/pautas/{id}/resultado", PAUTA_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("REQUISICAO_INVALIDA"))
                .andExpect(jsonPath("$.violacoes", hasSize(0)));
    }
}
