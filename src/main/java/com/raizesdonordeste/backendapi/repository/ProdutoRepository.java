package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {
    
    Page<Produto> findByAtivoTrue(Pageable pageable);
    
    List<Produto> findByAtivoTrue();

    Optional<Produto> findByIdAndAtivoTrue(Long id);
}
