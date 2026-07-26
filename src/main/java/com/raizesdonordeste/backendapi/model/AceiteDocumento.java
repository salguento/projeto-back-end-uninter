package com.raizesdonordeste.backendapi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "aceites_documentos", uniqueConstraints = @UniqueConstraint(
        name = "uk_aceite_usuario_tipo_versao",
        columnNames = {"usuario_id", "tipo_documento", "versao"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AceiteDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 40)
    private TipoDocumentoLegal tipoDocumento;

    @Column(nullable = false, length = 30)
    private String versao;

    @Column(name = "hash_documento", nullable = false, length = 64)
    private String hashDocumento;

    @Column(name = "aceito_em", nullable = false)
    private LocalDateTime aceitoEm;

    @Column(nullable = false, length = 50)
    private String origem;
}
