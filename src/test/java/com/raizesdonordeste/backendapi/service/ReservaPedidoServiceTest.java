package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.PedidoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaPedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private EstoqueService estoqueService;
    @Mock private DescontoService descontoService;
    @InjectMocks private ReservaPedidoService reservaPedidoService;

    @Test
    void deveCancelarPedidoPendenteEDevolverEstoque() {
        Pedido pedido = criarPedidoPendente();

        boolean cancelado = reservaPedidoService.cancelarEDevolverEstoque(pedido, "PRAZO_EXPIRADO");

        assertTrue(cancelado);
        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        verify(pedidoRepository).save(pedido);
        verify(estoqueService).estornarEstoque(10L, 20L, 2);
        verify(descontoService).estornarBeneficios(pedido);
    }

    @Test
    void naoDeveDevolverEstoqueDuasVezes() {
        Pedido pedido = criarPedidoPendente();
        pedido.setStatus(StatusPedido.CANCELADO);

        assertFalse(reservaPedidoService.cancelarEDevolverEstoque(pedido, "PRAZO_EXPIRADO"));

        verifyNoInteractions(pedidoRepository, estoqueService, descontoService);
    }

    @Test
    void deveExpirarTodasAsReservasVencidas() {
        Pedido pedido = criarPedidoPendente();
        when(pedidoRepository.findByStatusAndExpiraEmBefore(eq(StatusPedido.AGUARDANDO_PAGAMENTO), any(LocalDateTime.class)))
                .thenReturn(List.of(pedido));

        reservaPedidoService.expirarReservasVencidas();

        assertEquals(StatusPedido.CANCELADO, pedido.getStatus());
        verify(estoqueService).estornarEstoque(10L, 20L, 2);
        verify(descontoService).estornarBeneficios(pedido);
    }

    private Pedido criarPedidoPendente() {
        Produto produto = new Produto();
        produto.setId(10L);
        Unidade unidade = new Unidade();
        unidade.setId(20L);
        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(2);
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUnidade(unidade);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setItens(List.of(item));
        return pedido;
    }
}
