package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Unidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadeRepository extends JpaRepository<Unidade, Long> {
	Optional<Unidade> findByNome(String nome);

	Optional<Unidade> findByNomeAndAtivoTrue(String nome);

	boolean existsByNomeAndAtivoTrue(String nome);

	List<Unidade> findByAtivoTrue();

	Page<Unidade> findByAtivoTrue(Pageable pageable);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select u from Unidade u where u.id = :id")
	Optional<Unidade> findByIdForUpdate(@Param("id") Long id);
}
