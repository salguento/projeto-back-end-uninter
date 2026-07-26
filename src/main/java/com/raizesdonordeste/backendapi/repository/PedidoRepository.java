package com.raizesdonordeste.backendapi.repository;

import com.raizesdonordeste.backendapi.model.CanalPedido;
import com.raizesdonordeste.backendapi.model.Pedido;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;
import com.raizesdonordeste.backendapi.model.StatusPedido;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
	Page<Pedido> findByUsuarioId(Long usuarioId, Pageable pageable);

	Page<Pedido> findByUsuarioIdAndCanalPedido(Long usuarioId, CanalPedido canalPedido, Pageable pageable);

	Page<Pedido> findByUsuarioIdAndUnidadeId(Long usuarioId, Long unidadeId, Pageable pageable);

	Page<Pedido> findByUsuarioIdAndCanalPedidoAndUnidadeId(Long usuarioId, CanalPedido canalPedido,
			Long unidadeId, Pageable pageable);

	Page<Pedido> findByUsuarioIdAndStatus(Long usuarioId, StatusPedido status, Pageable pageable);

	Page<Pedido> findByUsuarioIdAndCanalPedidoAndStatus(Long usuarioId, CanalPedido canalPedido,
			StatusPedido status, Pageable pageable);

	Page<Pedido> findByUsuarioIdAndStatusAndUnidadeId(Long usuarioId, StatusPedido status,
			Long unidadeId, Pageable pageable);

	Page<Pedido> findByUsuarioIdAndCanalPedidoAndStatusAndUnidadeId(Long usuarioId, CanalPedido canalPedido,
			StatusPedido status, Long unidadeId, Pageable pageable);

	Page<Pedido> findByCanalPedido(CanalPedido canalPedido, Pageable pageable);

	Page<Pedido> findByStatus(StatusPedido status, Pageable pageable);

	Page<Pedido> findByCanalPedidoAndStatus(CanalPedido canalPedido, StatusPedido status, Pageable pageable);

	Page<Pedido> findByUnidadeId(Long unidadeId, Pageable pageable);

	Page<Pedido> findByCanalPedidoAndUnidadeId(CanalPedido canalPedido, Long unidadeId, Pageable pageable);

	Page<Pedido> findByStatusAndUnidadeId(StatusPedido status, Long unidadeId, Pageable pageable);

	Page<Pedido> findByCanalPedidoAndStatusAndUnidadeId(CanalPedido canalPedido, StatusPedido status,
			Long unidadeId, Pageable pageable);

	Page<Pedido> findByUnidadeIdIn(List<Long> unidadeIds, Pageable pageable);

	Page<Pedido> findByCanalPedidoAndUnidadeIdIn(CanalPedido canalPedido, List<Long> unidadeIds, Pageable pageable);

	Page<Pedido> findByStatusAndUnidadeIdIn(StatusPedido status, List<Long> unidadeIds, Pageable pageable);

	Page<Pedido> findByCanalPedidoAndStatusAndUnidadeIdIn(CanalPedido canalPedido, StatusPedido status,
			List<Long> unidadeIds, Pageable pageable);

	boolean existsByUnidadeId(Long unidadeId);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	List<Pedido> findByStatusAndExpiraEmBefore(StatusPedido status, LocalDateTime limite);

	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select p from Pedido p where p.id = :id")
	Optional<Pedido> findByIdForUpdate(@Param("id") Long id);

	Optional<Pedido> findByUsuarioRegistroIdAndChaveIdempotencia(Long usuarioRegistroId, String chaveIdempotencia);
}
