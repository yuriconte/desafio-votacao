package br.com.somosdb.votacao.pauta;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pauta")
public class Pauta {

    @Id
    private UUID id;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    protected Pauta() {
        // Exigido pelo JPA.
    }

    public Pauta(UUID id, String titulo, String descricao, Instant criadaEm) {
        this.id = id;
        this.titulo = titulo;
        this.descricao = descricao;
        this.criadaEm = criadaEm;
    }

    public UUID getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }

    public Instant getCriadaEm() {
        return criadaEm;
    }
}

