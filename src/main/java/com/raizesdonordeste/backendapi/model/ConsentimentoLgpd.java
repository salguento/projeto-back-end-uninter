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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "consentimentos_lgpd")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConsentimentoLgpd {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private FinalidadeConsentimento finalidade;

    @Column(name = "versao_termo", nullable = false, length = 30)
    private String versaoTermo;

    @Column(nullable = false)
    private Boolean concedido;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;

    @Column(nullable = false, length = 50)
    private String origem;
}
