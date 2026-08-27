package br.com.somosdb.votacao.sessao;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SessaoRepository extends JpaRepository<SessaoVotacao, UUID> {

    Optional<SessaoVotacao> findByPautaId(UUID pautaId);

    boolean existsByPautaId(UUID pautaId);
}

