package br.com.somosdb.votacao.voto;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import br.com.somosdb.votacao.pauta.Pauta;

@Entity
@Table(name = "voto")
public class Voto {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false)
    private Pauta pauta;

    @Column(name = "associado_id", nullable = false, length = 100)
    private String associadoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 3)
    private OpcaoVoto opcao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Voto() {
        // Exigido pelo JPA.
    }

    public Voto(UUID id, Pauta pauta, String associadoId, OpcaoVoto opcao, Instant criadoEm) {
        this.id = id;
        this.pauta = pauta;
        this.associadoId = associadoId;
        this.opcao = opcao;
        this.criadoEm = criadoEm;
    }

    public UUID getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public String getAssociadoId() {
        return associadoId;
    }

    public OpcaoVoto getOpcao() {
        return opcao;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}

