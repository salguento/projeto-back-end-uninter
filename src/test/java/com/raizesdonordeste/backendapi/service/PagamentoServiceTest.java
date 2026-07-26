package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.PagamentoRequestDTO;
import com.raizesdonordeste.backendapi.dto.PagamentoResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.exception.GatewayPagamentoIndisponivelException;
import com.raizesdonordeste.backendapi.gateway.GatewayPagamento;
import com.raizesdonordeste.backendapi.gateway.ResultadoGatewayEstorno;
import com.raizesdonordeste.backendapi.gateway.ResultadoGatewayPagamento;
import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.PagamentoRepository;
import com.raizesdonordeste.backendapi.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PagamentoService - Testes de Logica de Negocio")
class PagamentoServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;

    @Mock
    private PagamentoRepository pagamentoRepository;

    @Mock
    private AutorizacaoPedidoService autorizacaoPedidoService;

    @Mock
    private ReservaPedidoService reservaPedidoService;

    @Mock
    private GatewayPagamento gatewayPagamento;

    @InjectMocks
    private PagamentoService pagamentoService;

    private Pedido pedido;
    private PagamentoRequestDTO dto;

    @BeforeEach
    void setUp() {
        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setValorTotal(new BigDecimal("48.90"));
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.setItens(new java.util.ArrayList<>());

        dto = new PagamentoRequestDTO();
        dto.setFormaPagamento(FormaPagamento.PIX);
    }

    private void configurarMocksBasicos() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(any(Pedido.class), eq("APROVADO"))).thenReturn(false);
        when(pagamentoRepository.countByPedido(any(Pedido.class))).thenReturn(0L);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
            Pagamento p = invocation.getArgument(0);
            p.setId(10L);
            p.setCriadoEm(LocalDateTime.now());
            return p;
        });
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
    }

    // ============================================
    // TESTES DE SUCESSO
    // ============================================

    @Test
    @DisplayName("Deve processar pagamento com sucesso e atualizar status para RECEBIDO")
    void deveProcessarPagamentoComSucesso() {
        configurarMocksBasicos();
        when(gatewayPagamento.processar(any())).thenReturn(ResultadoGatewayPagamento.APROVADO);

        PagamentoResponseDTO response = pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com");

        assertNotNull(response);
        assertEquals(10L, response.getPagamentoId());
        assertEquals(1L, response.getPedidoId());
        assertEquals(FormaPagamento.PIX, response.getFormaPagamento());
        assertEquals(new BigDecimal("48.90"), response.getValor());
        assertEquals("APROVADO", response.getStatus());
        assertTrue(response.getCodigoTransacao().startsWith("TXN-"));
        assertNotNull(response.getCriadoEm());

        verify(pedidoRepository).findById(1L);
        verify(autorizacaoPedidoService).verificarPagamento(pedido, "cliente@raizes.com");
        verify(pagamentoRepository).existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, "APROVADO");
        verify(pagamentoRepository).save(any(Pagamento.class));
        verify(pedidoRepository).save(argThat(p -> p.getStatus() == StatusPedido.RECEBIDO));
    }

    @Test
    @DisplayName("Deve manter pedido aguardando pagamento quando tentativa e recusada")
    void deveManterPedidoAguardandoPagamentoQuandoRecusado() {
        configurarMocksBasicos();
        when(gatewayPagamento.processar(any())).thenReturn(ResultadoGatewayPagamento.RECUSADO);

        PagamentoResponseDTO response = pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com");

        assertEquals("RECUSADO", response.getStatus());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, response.getStatusPedido());
        assertEquals(2, response.getTentativasRestantes());
        verify(pedidoRepository).save(argThat(p -> p.getStatus() == StatusPedido.AGUARDANDO_PAGAMENTO));
    }

    @Test
    @DisplayName("Deve permitir nova tentativa quando pagamento anterior foi recusado")
    void devePermitirNovaTentativaQuandoPagamentoAnteriorRecusado() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, "APROVADO")).thenReturn(false);
        when(pagamentoRepository.countByPedido(pedido)).thenReturn(1L);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
            Pagamento p = invocation.getArgument(0);
            p.setId(10L);
            p.setCriadoEm(LocalDateTime.now());
            return p;
        });
        when(pedidoRepository.save(pedido)).thenReturn(pedido);
        when(gatewayPagamento.processar(any())).thenReturn(ResultadoGatewayPagamento.APROVADO);

        PagamentoResponseDTO response = pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com");

        assertNotNull(response);
        assertEquals("APROVADO", response.getStatus());
        verify(pagamentoRepository, never()).delete(any());
        verify(pagamentoRepository).save(any(Pagamento.class));
    }

    @Test
    @DisplayName("Deve preservar pedido e tentativas quando gateway esta indisponivel")
    void devePreservarPedidoQuandoGatewayIndisponivel() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, "APROVADO"))
                .thenReturn(false);
        when(pagamentoRepository.countByPedido(pedido)).thenReturn(1L);
        when(gatewayPagamento.processar(FormaPagamento.PIX))
                .thenThrow(new GatewayPagamentoIndisponivelException());

        GatewayPagamentoIndisponivelException exception = assertThrows(
                GatewayPagamentoIndisponivelException.class,
                () -> pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com"));

        assertEquals(GatewayPagamentoIndisponivelException.MESSAGE, exception.getMessage());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
        verify(pagamentoRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(reservaPedidoService);
    }

    @Test
    @DisplayName("Deve cancelar pedido e liberar reserva na terceira recusa")
    void deveCancelarPedidoNaTerceiraRecusa() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, "APROVADO")).thenReturn(false);
        when(pagamentoRepository.countByPedido(pedido)).thenReturn(2L);
        when(pagamentoRepository.save(any(Pagamento.class))).thenAnswer(invocation -> {
            Pagamento pagamento = invocation.getArgument(0);
            pagamento.setId(10L);
            return pagamento;
        });
        when(gatewayPagamento.processar(FormaPagamento.PIX)).thenReturn(ResultadoGatewayPagamento.RECUSADO);
        when(reservaPedidoService.cancelarEDevolverEstoque(pedido, "LIMITE_TENTATIVAS"))
                .thenAnswer(invocation -> {
                    pedido.setStatus(StatusPedido.CANCELADO);
                    return true;
                });

        PagamentoResponseDTO response = pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com");

        assertEquals("RECUSADO", response.getStatus());
        assertEquals(StatusPedido.CANCELADO, response.getStatusPedido());
        assertEquals(0, response.getTentativasRestantes());
        verify(reservaPedidoService).cancelarEDevolverEstoque(pedido, "LIMITE_TENTATIVAS");
    }

    @Test
    @DisplayName("Deve cancelar pedido quando a reserva estiver expirada")
    void deveCancelarPedidoQuandoReservaExpirada() {
        pedido.setExpiraEm(LocalDateTime.now().minusMinutes(1));
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com"));

        assertEquals("RESERVA_EXPIRADA", exception.getErrorCode());
        verify(reservaPedidoService).cancelarEDevolverEstoque(pedido, "PRAZO_EXPIRADO");
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve listar historico de tentativas sem apagar recusas")
    void deveListarHistoricoTentativas() {
        Pagamento primeira = new Pagamento();
        primeira.setId(1L);
        primeira.setPedido(pedido);
        primeira.setFormaPagamento(FormaPagamento.CARTAO);
        primeira.setValor(pedido.getValorTotal());
        primeira.setStatus("RECUSADO");
        primeira.setCriadoEm(LocalDateTime.now().minusMinutes(1));
        Pagamento segunda = new Pagamento();
        segunda.setId(2L);
        segunda.setPedido(pedido);
        segunda.setFormaPagamento(FormaPagamento.PIX);
        segunda.setValor(pedido.getValorTotal());
        segunda.setStatus("APROVADO");
        segunda.setCriadoEm(LocalDateTime.now());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.findAllByPedidoOrderByCriadoEmAsc(pedido))
                .thenReturn(List.of(primeira, segunda));

        List<PagamentoResponseDTO> response = pagamentoService.listarTentativas(1L, "cliente@raizes.com");

        assertEquals(2, response.size());
        assertEquals("RECUSADO", response.get(0).getStatus());
        assertEquals("APROVADO", response.get(1).getStatus());
        verify(autorizacaoPedidoService).verificarConsulta(pedido, "cliente@raizes.com");
    }

    @Test
    @DisplayName("Deve estornar pagamento aprovado preservando a transacao original")
    void deveEstornarPagamentoAprovado() {
        Pagamento pagamento = new Pagamento();
        pagamento.setId(10L);
        pagamento.setPedido(pedido);
        pagamento.setFormaPagamento(FormaPagamento.PIX);
        pagamento.setValor(pedido.getValorTotal());
        pagamento.setStatus("APROVADO");
        pagamento.setCodigoTransacao("TXN-12345678");
        when(pagamentoRepository.findFirstByPedidoAndStatusAndEstornadoEmIsNullOrderByCriadoEmDesc(
                pedido, "APROVADO")).thenReturn(Optional.of(pagamento));
        when(pagamentoRepository.save(pagamento)).thenReturn(pagamento);
        when(pagamentoRepository.countByPedido(pedido)).thenReturn(1L);
        when(gatewayPagamento.estornar("TXN-12345678"))
                .thenReturn(new ResultadoGatewayEstorno("EST-87654321"));

        PagamentoResponseDTO response = pagamentoService.estornarPagamento(pedido);

        assertEquals("ESTORNADO", response.getStatus());
        assertEquals("TXN-12345678", response.getCodigoTransacao());
        assertEquals("EST-87654321", response.getCodigoEstorno());
        assertNotNull(response.getEstornadoEm());
        assertEquals("APROVADO", pagamento.getStatus());
        verify(gatewayPagamento).estornar("TXN-12345678");
        verify(pagamentoRepository).save(pagamento);
    }

    @Test
    @DisplayName("Nao deve persistir estorno quando gateway esta indisponivel")
    void naoDevePersistirEstornoQuandoGatewayIndisponivel() {
        Pagamento pagamento = new Pagamento();
        pagamento.setId(10L);
        pagamento.setPedido(pedido);
        pagamento.setStatus("APROVADO");
        pagamento.setCodigoTransacao("TXN-12345678");
        when(pagamentoRepository.findFirstByPedidoAndStatusAndEstornadoEmIsNullOrderByCriadoEmDesc(
                pedido, "APROVADO")).thenReturn(Optional.of(pagamento));
        when(gatewayPagamento.estornar("TXN-12345678"))
                .thenThrow(new GatewayPagamentoIndisponivelException());

        assertThrows(GatewayPagamentoIndisponivelException.class,
                () -> pagamentoService.estornarPagamento(pedido));

        assertNull(pagamento.getCodigoEstorno());
        assertNull(pagamento.getEstornadoEm());
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir estorno duplicado")
    void deveImpedirEstornoDuplicado() {
        when(pagamentoRepository.findFirstByPedidoAndStatusAndEstornadoEmIsNullOrderByCriadoEmDesc(
                pedido, "APROVADO")).thenReturn(Optional.empty());

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pagamentoService.estornarPagamento(pedido));

        assertEquals("PAGAMENTO_NAO_ESTORNAVEL", exception.getErrorCode());
        verifyNoInteractions(gatewayPagamento);
        verify(pagamentoRepository, never()).save(any());
    }

    // ============================================
    // TESTES DE ERRO
    // ============================================

    @Test
    @DisplayName("Deve interromper pagamento quando usuario nao tem acesso ao pedido")
    void deveInterromperPagamentoQuandoUsuarioNaoTemAcesso() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(autorizacaoPedidoService)
                .verificarPagamento(pedido, "cliente@raizes.com");

        assertThrows(AccessDeniedException.class, () -> pagamentoService.processarPagamento(
                1L, dto, "cliente@raizes.com"));

        verify(pagamentoRepository, never()).existsByPedidoAndStatusAndEstornadoEmIsNull(any(), anyString());
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando pedido nao existe")
    void deveLancarExcecaoQuandoPedidoNaoExiste() {
        when(pedidoRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> pagamentoService.processarPagamento(999L, dto, "cliente@raizes.com"));

        assertEquals("PEDIDO_NAO_ENCONTRADO", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("999"));
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando pedido nao esta aguardando pagamento")
    void deveLancarExcecaoQuandoPedidoNaoEstaAguardandoPagamento() {
        pedido.setStatus(StatusPedido.RECEBIDO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com"));

        assertEquals("PEDIDO_INVALIDO", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("RECEBIDO"));
        verify(pagamentoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando pagamento anterior foi APROVADO")
    void deveLancarExcecaoQuandoPagamentoAnteriorAprovado() {
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, "APROVADO")).thenReturn(true);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com"));

        assertEquals("PAGAMENTO_JA_PROCESSADO", exception.getErrorCode());
        verify(pagamentoRepository, never()).save(any());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando DTO e nulo")
    void deveLancarExcecaoQuandoDTONulo() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> pagamentoService.processarPagamento(1L, null, "cliente@raizes.com"));

        assertTrue(exception.getMessage().contains("DTO"));
    }

    @Test
    @DisplayName("Deve lancar excecao quando forma de pagamento e nula")
    void deveLancarExcecaoQuandoFormaPagamentoNula() {
        dto.setFormaPagamento(null);

        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com"));

        assertTrue(exception.getMessage().contains("Forma de pagamento"));
    }

    @Test
    @DisplayName("Deve lancar excecao quando valor do pedido e zero")
    void deveLancarExcecaoQuandoValorPedidoZero() {
        pedido.setValorTotal(BigDecimal.ZERO);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, "APROVADO")).thenReturn(false);
        when(pagamentoRepository.countByPedido(pedido)).thenReturn(0L);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com"));

        assertEquals("VALOR_INVALIDO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando valor do pedido e nulo")
    void deveLancarExcecaoQuandoValorPedidoNulo() {
        pedido.setValorTotal(null);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoRepository.existsByPedidoAndStatusAndEstornadoEmIsNull(pedido, "APROVADO")).thenReturn(false);
        when(pagamentoRepository.countByPedido(pedido)).thenReturn(0L);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pagamentoService.processarPagamento(1L, dto, "cliente@raizes.com"));

        assertEquals("VALOR_INVALIDO", exception.getErrorCode());
    }
}
