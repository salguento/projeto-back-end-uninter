package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.UsuarioUnidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UsuarioUnidadeRepository extends JpaRepository<UsuarioUnidade, Long> {
    List<UsuarioUnidade> findByUsuarioId(Long usuarioId);
    List<UsuarioUnidade> findByUsuarioEmail(String email);
    boolean existsByUnidadeId(Long unidadeId);
}