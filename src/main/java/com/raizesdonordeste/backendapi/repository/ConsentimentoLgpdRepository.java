package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.ConsentimentoLgpd;
import com.raizesdonordeste.backendapi.model.FinalidadeConsentimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConsentimentoLgpdRepository extends JpaRepository<ConsentimentoLgpd, Long> {

    Optional<ConsentimentoLgpd> findTopByUsuarioIdAndFinalidadeOrderByRegistradoEmDescIdDesc(
            Long usuarioId, FinalidadeConsentimento finalidade);
}
