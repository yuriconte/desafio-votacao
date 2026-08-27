package br.com.somosdb.votacao.voto;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VotoRepository extends JpaRepository<Voto, UUID> {

    @Query(value = """
            SELECT COUNT(*) AS total,
                   COUNT(*) FILTER (WHERE opcao = 'SIM') AS "votosSim",
                   COUNT(*) FILTER (WHERE opcao = 'NAO') AS "votosNao"
              FROM voto
             WHERE pauta_id = :pautaId
            """, nativeQuery = true)
    ResumoVotos resumirPorPauta(@Param("pautaId") UUID pautaId);
}

