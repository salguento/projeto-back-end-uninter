package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.ItemPedido;
import com.raizesdonordeste.backendapi.model.Pedido;
import com.raizesdonordeste.backendapi.model.StatusPedido;
import com.raizesdonordeste.backendapi.repository.PedidoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReservaPedidoService {

    private final PedidoRepository pedidoRepository;
    private final EstoqueService estoqueService;
    private final DescontoService descontoService;

    @Transactional
    public boolean cancelarEDevolverEstoque(Pedido pedido, String motivo) {
        if (pedido.getStatus() != StatusPedido.AGUARDANDO_PAGAMENTO) {
            return false;
        }

        pedido.setStatus(StatusPedido.CANCELADO);
        pedidoRepository.save(pedido);

        if (pedido.getItens() != null) {
            for (ItemPedido item : pedido.getItens()) {
                try {
                    estoqueService.estornarEstoque(item.getProduto().getId(), pedido.getUnidade().getId(),
                            item.getQuantidade());
                } catch (RecursoNaoEncontradoException e) {
                    throw new RegraNegocioException("ESTORNO_FALHOU",
                            "Falha ao devolver estoque do produto ID " + item.getProduto().getId());
                }
            }
        }

        descontoService.estornarBeneficios(pedido);

        log.info("Reserva encerrada | Pedido: {} | Motivo: {}", pedido.getId(), motivo);
        return true;
    }

    @Scheduled(fixedDelayString = "${app.pedido.expiracao-intervalo-ms:60000}")
    @Transactional
    public void expirarReservasVencidas() {
        List<Pedido> vencidos = pedidoRepository.findByStatusAndExpiraEmBefore(
                StatusPedido.AGUARDANDO_PAGAMENTO, LocalDateTime.now());
        vencidos.forEach(pedido -> cancelarEDevolverEstoque(pedido, "PRAZO_EXPIRADO"));
    }
}
