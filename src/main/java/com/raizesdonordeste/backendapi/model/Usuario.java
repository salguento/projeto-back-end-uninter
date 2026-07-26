package com.raizesdonordeste.backendapi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "usuarios")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Perfil perfil;

    @Column(nullable = false)
    private Integer pontos = 0;

    @Column
    private String telefone;

    @Column
    private String cpf;

    // Flag LGPD - indica se a conta foi anonimizada
    @Column(name = "anonimizado", nullable = false)
    private Boolean anonimizado = false;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "usuario_unidade",
        joinColumns = @JoinColumn(name = "usuario_id"),
        inverseJoinColumns = @JoinColumn(name = "unidade_id")
    )
    private Set<Unidade> unidades = new HashSet<>();
}