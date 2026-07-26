package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Cupom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface CupomRepository extends JpaRepository<Cupom, Long> {
	Optional<Cupom> findByCodigo(String codigo);

	Page<Cupom> findByUnidadeIdIn(List<Long> unidadeIds, Pageable pageable);

	@Modifying(flushAutomatically = true)
	@Query("""
			UPDATE Cupom c
			SET c.usoAtual = c.usoAtual + 1
			WHERE c.id = :cupomId
			  AND c.ativo = true
			  AND (c.usoMaximo IS NULL OR c.usoAtual < c.usoMaximo)
			""")
	int reservarUsoSeDisponivel(@Param("cupomId") Long cupomId);

	@Modifying(flushAutomatically = true)
	@Query("""
			UPDATE Cupom c
			SET c.usoAtual = c.usoAtual - 1
			WHERE c.id = :cupomId
			  AND c.usoAtual > 0
			""")
	int liberarUso(@Param("cupomId") Long cupomId);

	boolean existsByUnidadeId(Long unidadeId);
}
