package com.raizesdonordeste.backendapi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@Entity
@Table(name = "unidades")
public class Unidade {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 100)
	private String nome;

	@Column(nullable = false, length = 200)
	private String endereco;

	@Column(nullable = false, length = 100)
	private String cidade;

	@Column(nullable = false, length = 2)
	private String estado;

	@Column(nullable = false, length = 10)
	private String cep;

	@Column(nullable = false, length = 15)
	private String telefone;

	@Column(nullable = false)
	private Boolean ativo = true;
}