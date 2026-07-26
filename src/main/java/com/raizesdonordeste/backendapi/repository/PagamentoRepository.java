package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.Pagamento;
import com.raizesdonordeste.backendapi.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    List<Pagamento> findAllByPedidoOrderByCriadoEmAsc(Pedido pedido);
    boolean existsByPedidoAndStatusAndEstornadoEmIsNull(Pedido pedido, String status);
    Optional<Pagamento> findFirstByPedidoAndStatusAndEstornadoEmIsNullOrderByCriadoEmDesc(
            Pedido pedido, String status);
    long countByPedido(Pedido pedido);
}
