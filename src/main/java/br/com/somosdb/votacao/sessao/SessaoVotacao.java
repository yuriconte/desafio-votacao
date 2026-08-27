package br.com.somosdb.votacao.sessao;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import br.com.somosdb.votacao.pauta.Pauta;

@Entity
@Table(name = "sessao_votacao")
public class SessaoVotacao {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pauta_id", nullable = false, unique = true)
    private Pauta pauta;

    @Column(name = "aberta_em", nullable = false)
    private Instant abertaEm;

    @Column(name = "fecha_em", nullable = false)
    private Instant fechaEm;

    protected SessaoVotacao() {
        // Exigido pelo JPA.
    }

    public SessaoVotacao(UUID id, Pauta pauta, Instant abertaEm, Instant fechaEm) {
        this.id = id;
        this.pauta = pauta;
        this.abertaEm = abertaEm;
        this.fechaEm = fechaEm;
    }

    public boolean estaAbertaEm(Instant instante) {
        return !instante.isBefore(abertaEm) && instante.isBefore(fechaEm);
    }

    public StatusSessao statusEm(Instant instante) {
        return estaAbertaEm(instante) ? StatusSessao.ABERTA : StatusSessao.ENCERRADA;
    }

    public UUID getId() {
        return id;
    }

    public Pauta getPauta() {
        return pauta;
    }

    public Instant getAbertaEm() {
        return abertaEm;
    }

    public Instant getFechaEm() {
        return fechaEm;
    }
}

