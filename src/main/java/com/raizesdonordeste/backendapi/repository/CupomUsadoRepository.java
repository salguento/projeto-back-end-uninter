package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.CupomUsado;
import com.raizesdonordeste.backendapi.model.Pedido;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CupomUsadoRepository extends JpaRepository<CupomUsado, Long> {
    Optional<CupomUsado> findByPedidoAndEstornadoEmIsNull(Pedido pedido);
}
