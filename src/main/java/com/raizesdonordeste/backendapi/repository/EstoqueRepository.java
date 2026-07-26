package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Estoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EstoqueRepository extends JpaRepository<Estoque, Long> {
	Optional<Estoque> findByUnidadeIdAndProdutoId(Long unidadeId, Long produtoId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select e from Estoque e where e.id = :id")
	Optional<Estoque> findByIdForUpdate(@Param("id") Long id);

	@Modifying(flushAutomatically = true)
	@Query("""
			UPDATE Estoque e
			SET e.quantidade = e.quantidade - :quantidade
			WHERE e.unidade.id = :unidadeId
			  AND e.produto.id = :produtoId
			  AND e.quantidade >= :quantidade
			""")
	int reservarEstoqueSeDisponivel(@Param("unidadeId") Long unidadeId,
			@Param("produtoId") Long produtoId, @Param("quantidade") Integer quantidade);

	Page<Estoque> findByUnidadeId(Long unidadeId, Pageable pageable);

	List<Estoque> findByUnidadeId(Long unidadeId);

	boolean existsByUnidadeId(Long unidadeId);
}
