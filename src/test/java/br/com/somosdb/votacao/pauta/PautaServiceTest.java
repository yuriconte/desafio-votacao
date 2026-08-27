package br.com.somosdb.votacao.pauta;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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

import br.com.somosdb.votacao.pauta.dto.CriarPautaRequest;
import br.com.somosdb.votacao.pauta.dto.PautaResponse;
import br.com.somosdb.votacao.shared.error.RecursoNaoEncontradoException;

@ExtendWith(MockitoExtension.class)
class PautaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-08-25T12:00:00Z");

    @Mock
    private PautaRepository pautaRepository;

    private PautaService pautaService;

    @BeforeEach
    void setUp() {
        pautaService = new PautaService(pautaRepository, Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    @Test
    void deveCriarPautaNormalizandoTextos() {
        when(pautaRepository.save(any(Pauta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PautaResponse response = pautaService.criar(new CriarPautaRequest("  Nova pauta  ", "  Descrição  "));

        assertThat(response.id()).isNotNull();
        assertThat(response.titulo()).isEqualTo("Nova pauta");
        assertThat(response.descricao()).isEqualTo("Descrição");
        assertThat(response.criadaEm()).isEqualTo(AGORA);
        verify(pautaRepository).save(any(Pauta.class));
    }

    @Test
    void devePreservarDescricaoNula() {
        when(pautaRepository.save(any(Pauta.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PautaResponse response = pautaService.criar(new CriarPautaRequest("Pauta", null));

        assertThat(response.descricao()).isNull();
    }

    @Test
    void deveBuscarPautaExistente() {
        UUID id = UUID.randomUUID();
        Pauta pauta = new Pauta(id, "Pauta", null, AGORA);
        when(pautaRepository.findById(id)).thenReturn(Optional.of(pauta));

        assertThat(pautaService.buscar(id)).isSameAs(pauta);
    }

    @Test
    void deveRejeitarPautaInexistente() {
        UUID id = UUID.randomUUID();
        when(pautaRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> pautaService.buscar(id))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Pauta não encontrada")
                .extracting("codigo")
                .isEqualTo("PAUTA_NAO_ENCONTRADA");
    }
}

