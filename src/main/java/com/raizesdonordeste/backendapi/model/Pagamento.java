package com.raizesdonordeste.backendapi.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamentos")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pagamento {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pedido_id", nullable = false)
    private Pedido pedido;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormaPagamento formaPagamento;

    @Column(nullable = false)
    private BigDecimal valor;

    @Column(nullable = false)
    private String status; 

    @Column
    private String codigoTransacao;

    @Column(name = "codigo_estorno")
    private String codigoEstorno;

    @Column(name = "estornado_em")
    private LocalDateTime estornadoEm;

    @Column(nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
