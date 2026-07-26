package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.AceiteDocumento;
import com.raizesdonordeste.backendapi.model.TipoDocumentoLegal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AceiteDocumentoRepository extends JpaRepository<AceiteDocumento, Long> {

    Optional<AceiteDocumento> findByUsuarioIdAndTipoDocumentoAndVersao(
            Long usuarioId, TipoDocumentoLegal tipoDocumento, String versao);

    boolean existsByUsuarioIdAndTipoDocumentoAndVersaoAndHashDocumento(
            Long usuarioId, TipoDocumentoLegal tipoDocumento, String versao, String hashDocumento);
}
