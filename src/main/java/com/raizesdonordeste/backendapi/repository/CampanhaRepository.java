package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Campanha;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CampanhaRepository extends JpaRepository<Campanha, Long> {

	@Query("""
			    SELECT c FROM Campanha c
			    WHERE c.produto.id = :produtoId
			      AND c.unidade.id = :unidadeId
			      AND c.ativa = true
			      AND c.dataInicio <= :agora
			      AND c.dataFim >= :agora
			    ORDER BY c.dataFim DESC
			    LIMIT 1
			""")
	Optional<Campanha> findCampanhaAtiva(@Param("produtoId") Long produtoId, @Param("unidadeId") Long unidadeId,
			@Param("agora") LocalDateTime agora);

	List<Campanha> findByProdutoIdAndUnidadeIdAndAtivaTrue(Long produtoId, Long unidadeId);

	Page<Campanha> findByUnidadeIdIn(List<Long> unidadeIds, Pageable pageable);

	boolean existsByUnidadeId(Long unidadeId);
}
