package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {
    Page<MovimentacaoEstoque> findByEstoqueIdOrderByCriadoEmDesc(Long estoqueId, Pageable pageable);
}
