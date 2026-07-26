package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.ItemPedidoRequestDTO;
import com.raizesdonordeste.backendapi.dto.PedidoDTO;
import com.raizesdonordeste.backendapi.dto.PedidoResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.exception.GatewayPagamentoIndisponivelException;
import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PedidoService - Testes de Logica de Negocio")
class PedidoServiceTest {

    @Mock private PedidoRepository pedidoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private EstoqueRepository estoqueRepository;
    @Mock private AcessoUnidadeService acessoUnidadeService;
    @Mock private DescontoService descontoService;
    @Mock private EstoqueService estoqueService;
    @Mock private AutorizacaoPedidoService autorizacaoPedidoService;
    @Mock private ReservaPedidoService reservaPedidoService;
    @Mock private PagamentoService pagamentoService;
    @Mock private FidelidadeService fidelidadeService;

    @InjectMocks
    private PedidoService pedidoService;

    private Usuario cliente;
    private Usuario atendente;
    private Usuario admin;
    private Unidade unidade;
    private Produto produto;
    private Estoque estoque;

    @BeforeEach
    void setUp() {
        cliente = new Usuario();
        cliente.setId(1L);
        cliente.setEmail("cliente@raizes.com");
        cliente.setNome("Cliente Teste");
        cliente.setPerfil(Perfil.CLIENTE);
        cliente.setPontos(100);

        atendente = new Usuario();
        atendente.setId(2L);
        atendente.setEmail("atendente@raizes.com");
        atendente.setNome("Atendente Teste");
        atendente.setPerfil(Perfil.ATENDENTE);
        atendente.setPontos(0);

        admin = new Usuario();
        admin.setId(3L);
        admin.setEmail("admin@raizes.com");
        admin.setNome("Admin Teste");
        admin.setPerfil(Perfil.ADMIN);
        admin.setPontos(0);

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Unidade Centro");
        unidade.setAtivo(true);

        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Tapioca Tradicional");
        produto.setPrecoVigente(new BigDecimal("14.50"));
        produto.setAtivo(true);

        estoque = new Estoque();
        estoque.setId(1L);
        estoque.setProduto(produto);
        estoque.setUnidade(unidade);
        estoque.setQuantidade(50);
    }

    private PedidoDTO criarPedidoDTO(Long produtoId, Integer quantidade) {
        ItemPedidoRequestDTO itemDTO = new ItemPedidoRequestDTO();
        itemDTO.setProdutoId(produtoId);
        itemDTO.setQuantidade(quantidade);

        PedidoDTO dto = new PedidoDTO();
        dto.setUnidadeId(1L);
        dto.setCanalPedido(CanalPedido.APP);
        dto.setFormaPagamento(FormaPagamento.PIX);
        dto.setUsarPontos(false);
        dto.setItens(List.of(itemDTO));
        return dto;
    }

    private void configurarMocksBasicos() {
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 2)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(produto.getPrecoVigente());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);
    }

    @Test
    @DisplayName("Deve criar pedido com sucesso quando dados sao validos")
    void deveCriarPedidoComSucesso() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        configurarMocksBasicos();

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "cliente@raizes.com");

        assertNotNull(response);
        assertEquals(100L, response.getPedidoId());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, response.getStatus());
        assertEquals(new BigDecimal("29.00"), response.getTotal());
        assertEquals(BigDecimal.ZERO, response.getDesconto());
        assertEquals(1, response.getItens().size());
        assertNull(response.getCupomAplicado());
        assertNotNull(response.getExpiraEm());
        assertTrue(response.getExpiraEm().isAfter(response.getDataCriacao()));

        verify(pedidoRepository).save(any(Pedido.class));
        verify(estoqueRepository).reservarEstoqueSeDisponivel(1L, 1L, 2);
    }

    @Test
    @DisplayName("Deve devolver o pedido original ao repetir a mesma chave idempotente")
    void deveRepetirCriacaoDeFormaIdempotente() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        AtomicReference<Pedido> pedidoPersistido = new AtomicReference<>();

        when(usuarioRepository.findByEmailForUpdate("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByUsuarioRegistroIdAndChaveIdempotencia(1L, "pedido-teste-001"))
                .thenAnswer(invocation -> Optional.ofNullable(pedidoPersistido.get()));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 2)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(produto.getPrecoVigente());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(100L);
            pedidoPersistido.set(pedido);
            return pedido;
        });
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        PedidoResponseDTO primeira = pedidoService.criarPedido(dto, "cliente@raizes.com", "pedido-teste-001");
        PedidoResponseDTO repetida = pedidoService.criarPedido(dto, "cliente@raizes.com", "pedido-teste-001");

        assertEquals(primeira.getPedidoId(), repetida.getPedidoId());
        assertEquals(primeira.getTotal(), repetida.getTotal());
        verify(pedidoRepository, times(1)).save(any(Pedido.class));
        verify(estoqueRepository, times(1)).reservarEstoqueSeDisponivel(1L, 1L, 2);
        verify(unidadeRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("Deve rejeitar a mesma chave idempotente com conteudo diferente")
    void deveRejeitarChaveIdempotenteComConteudoDiferente() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        Pedido pedidoAnterior = new Pedido();
        pedidoAnterior.setId(99L);
        pedidoAnterior.setHashRequisicao("hash-de-outro-conteudo");

        when(usuarioRepository.findByEmailForUpdate("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(pedidoRepository.findByUsuarioRegistroIdAndChaveIdempotencia(1L, "pedido-teste-002"))
                .thenReturn(Optional.of(pedidoAnterior));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com", "pedido-teste-002"));

        assertEquals("CHAVE_IDEMPOTENCIA_REUTILIZADA", exception.getErrorCode());
        verify(unidadeRepository, never()).findById(anyLong());
        verify(estoqueRepository, never()).reservarEstoqueSeDisponivel(anyLong(), anyLong(), anyInt());
    }

    @Test
    @DisplayName("Deve rejeitar chave idempotente fora do formato permitido")
    void deveRejeitarChaveIdempotenteInvalida() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);

        assertThrows(IllegalArgumentException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com", "curta"));

        verifyNoInteractions(usuarioRepository, unidadeRepository, estoqueRepository, pedidoRepository);
    }

    @Test
    @DisplayName("Deve lancar excecao quando usuario nao existe")
    void deveLancarExcecaoQuandoUsuarioNaoExiste() {
        // Arrange - DTO com itens validos para passar pela validacao inicial
        PedidoDTO dto = criarPedidoDTO(1L, 2);

        when(usuarioRepository.findByEmail("inexistente@raizes.com")).thenReturn(Optional.empty());

        // Act & Assert
        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> pedidoService.criarPedido(dto, "inexistente@raizes.com"));

        assertEquals("USUARIO_NAO_ENCONTRADO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve rejeitar criacao de pedido em unidade inativa antes de reservar estoque")
    void deveRejeitarCriacaoDePedidoEmUnidadeInativa() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        unidade.setAtivo(false);

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com"));

        assertEquals("UNIDADE_INATIVA", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("nao pode receber pedidos"));
        verifyNoInteractions(produtoRepository, estoqueRepository, descontoService);
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(acessoUnidadeService);
    }

    @Test
    @DisplayName("Deve lancar excecao quando estoque e insuficiente")
    void deveLancarExcecaoQuandoEstoqueInsuficiente() {
        PedidoDTO dto = criarPedidoDTO(1L, 100);

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 100)).thenReturn(0);
        when(estoqueRepository.findByUnidadeIdAndProdutoId(1L, 1L)).thenReturn(Optional.of(estoque));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com"));

        assertEquals("ESTOQUE_INSUFICIENTE", exception.getErrorCode());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve consolidar produtos repetidos antes da reserva atomica")
    void deveConsolidarProdutosRepetidos() {
        ItemPedidoRequestDTO primeiro = new ItemPedidoRequestDTO();
        primeiro.setProdutoId(1L);
        primeiro.setQuantidade(1);
        ItemPedidoRequestDTO segundo = new ItemPedidoRequestDTO();
        segundo.setProdutoId(1L);
        segundo.setQuantidade(2);
        PedidoDTO dto = criarPedidoDTO(1L, 1);
        dto.setItens(List.of(primeiro, segundo));

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 3)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(produto.getPrecoVigente());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido salvo = invocation.getArgument(0);
            salvo.setId(100L);
            return salvo;
        });

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "cliente@raizes.com");

        assertEquals(1, response.getItens().size());
        assertEquals(3, response.getItens().get(0).getQuantidade());
        assertEquals(new BigDecimal("43.50"), response.getTotal());
        verify(estoqueRepository).reservarEstoqueSeDisponivel(1L, 1L, 3);
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto esta inativo")
    void deveLancarExcecaoQuandoProdutoInativo() {
        produto.setAtivo(false);
        PedidoDTO dto = criarPedidoDTO(1L, 2);

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com"));

        assertEquals("PRODUTO_INATIVO", exception.getErrorCode());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando cliente tenta criar pedido para outro cliente")
    void deveLancarExcecaoQuandoClienteTentaCriarPedidoParaOutroCliente() {
        Usuario outroCliente = new Usuario();
        outroCliente.setId(99L);
        outroCliente.setEmail("outro@raizes.com");

        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setClienteId(99L);

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(outroCliente));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com"));

        assertTrue(exception.getMessage().contains("atendentes"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando cliente tenta criar pedido interno")
    void deveLancarExcecaoQuandoClienteTentaCriarPedidoInterno() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setPedidoInterno(true);

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        AccessDeniedException exception = assertThrows(AccessDeniedException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com"));

        assertTrue(exception.getMessage().contains("internos"));
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir atendente criar pedido para outro cliente")
    void devePermitirAtendenteCriarPedidoParaOutroCliente() {
        Usuario outroCliente = new Usuario();
        outroCliente.setId(99L);
        outroCliente.setEmail("outro@raizes.com");
        outroCliente.setPontos(50);

        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setClienteId(99L);

        when(usuarioRepository.findByEmail("atendente@raizes.com")).thenReturn(Optional.of(atendente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(usuarioRepository.findById(99L)).thenReturn(Optional.of(outroCliente));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 2)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(produto.getPrecoVigente());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "atendente@raizes.com");

        assertNotNull(response);
        assertEquals(100L, response.getPedidoId());
        verify(pedidoRepository).save(any(Pedido.class));
    }

    @Test
    @DisplayName("Deve bloquear o cliente beneficiario ao resgatar pontos em pedido assistido")
    void deveBloquearClienteBeneficiarioAoResgatarPontosEmPedidoAssistido() {
        Usuario outroCliente = new Usuario();
        outroCliente.setId(99L);
        outroCliente.setEmail("outro@raizes.com");
        outroCliente.setPontos(50);

        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setClienteId(99L);
        dto.setUsarPontos(true);

        when(usuarioRepository.findByEmailForUpdate("atendente@raizes.com")).thenReturn(Optional.of(atendente));
        when(usuarioRepository.findByIdForUpdate(99L)).thenReturn(Optional.of(outroCliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 2)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(produto.getPrecoVigente());
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido pedido = invocation.getArgument(0);
            pedido.setId(100L);
            return pedido;
        });

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "atendente@raizes.com");

        assertEquals(new BigDecimal("5.00"), response.getDesconto());
        assertEquals(50, response.getPontosUtilizados());
        verify(usuarioRepository).findByIdForUpdate(99L);
        verify(fidelidadeService).validarConsentimentoParaResgate(outroCliente);
    }

    @Test
    @DisplayName("Deve aplicar desconto de fidelidade quando cliente usa pontos")
    void deveAplicarDescontoDeFidelidade() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setUsarPontos(true);

        when(usuarioRepository.findByEmailForUpdate("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        configurarMocksBasicos();

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "cliente@raizes.com");

        assertEquals(new BigDecimal("10.00"), response.getDesconto());
        assertEquals(new BigDecimal("19.00"), response.getTotal());
        assertEquals(100, response.getPontosUtilizados());
        assertEquals(0, cliente.getPontos());
        verify(usuarioRepository).save(cliente);
        verify(usuarioRepository).findByEmailForUpdate("cliente@raizes.com");
        verify(fidelidadeService).validarConsentimentoParaResgate(cliente);
    }

    @Test
    @DisplayName("Nao deve permitir resgate enquanto houver saldo devedor")
    void naoDeveResgatarPontosComSaldoDevedor() {
        cliente.setPontos(-15);
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setUsarPontos(true);

        when(usuarioRepository.findByEmailForUpdate("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        configurarMocksBasicos();

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "cliente@raizes.com");

        assertEquals(BigDecimal.ZERO, response.getDesconto());
        assertEquals(new BigDecimal("29.00"), response.getTotal());
        assertEquals(0, response.getPontosUtilizados());
        assertEquals(-15, cliente.getPontos());
        verify(usuarioRepository, never()).save(cliente);
        verify(fidelidadeService).validarConsentimentoParaResgate(cliente);
    }

    @Test
    @DisplayName("Deve impedir resgate de pontos sem consentimento LGPD ativo")
    void deveImpedirResgateSemConsentimentoLgpd() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setUsarPontos(true);

        when(usuarioRepository.findByEmailForUpdate("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);
        doThrow(new RegraNegocioException("CONSENTIMENTO_FIDELIDADE_NECESSARIO", "Consentimento necessario."))
                .when(fidelidadeService).validarConsentimentoParaResgate(cliente);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.criarPedido(dto, "cliente@raizes.com"));

        assertEquals("CONSENTIMENTO_FIDELIDADE_NECESSARIO", exception.getErrorCode());
        verify(produtoRepository, never()).findById(anyLong());
        verify(estoqueRepository, never()).reservarEstoqueSeDisponivel(anyLong(), anyLong(), anyInt());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve aplicar desconto de campanha quando ha promocao ativa")
    void deveAplicarDescontoDeCampanha() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 2)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(new BigDecimal("9.90"));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "cliente@raizes.com");

        assertEquals(new BigDecimal("19.80"), response.getTotal());
        verify(descontoService).obterPrecoDoProduto(produto, 1L);
    }

    @Test
    @DisplayName("Deve aplicar cupom promocional quando codigo e fornecido")
    void deveAplicarCupomPromocional() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setCodigoCupom("NORDESTE10");

        when(usuarioRepository.findByEmail("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 2)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(produto.getPrecoVigente());
        when(descontoService.validarCupom("NORDESTE10", new BigDecimal("29.00"), 1L))
                .thenReturn(new BigDecimal("2.90"));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "cliente@raizes.com");

        assertEquals(new BigDecimal("2.90"), response.getDesconto());
        assertEquals(new BigDecimal("26.10"), response.getTotal());
        assertNotNull(response.getCupomAplicado());
        assertEquals("NORDESTE10", response.getCupomAplicado().getCodigo());
        assertEquals(new BigDecimal("2.90"), response.getCupomAplicado().getValorDesconto());

        verify(descontoService).validarCupom("NORDESTE10", new BigDecimal("29.00"), 1L);
        verify(descontoService).registrarUsoCupom(eq("NORDESTE10"), any(Pedido.class), eq(cliente), eq(new BigDecimal("2.90")));
    }

    @Test
    @DisplayName("Deve combinar campanha + cupom + fidelidade corretamente")
    void deveCombinarTodosOsDescontos() {
        PedidoDTO dto = criarPedidoDTO(1L, 2);
        dto.setUsarPontos(true);
        dto.setCodigoCupom("NORDESTE10");

        when(usuarioRepository.findByEmailForUpdate("cliente@raizes.com")).thenReturn(Optional.of(cliente));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(estoqueRepository.reservarEstoqueSeDisponivel(1L, 1L, 2)).thenReturn(1);
        when(descontoService.obterPrecoDoProduto(produto, 1L)).thenReturn(new BigDecimal("9.90"));
        when(descontoService.validarCupom("NORDESTE10", new BigDecimal("19.80"), 1L))
                .thenReturn(new BigDecimal("1.98"));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(invocation -> {
            Pedido p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        PedidoResponseDTO response = pedidoService.criarPedido(dto, "cliente@raizes.com");

        assertEquals(new BigDecimal("11.98"), response.getDesconto());
        assertEquals(new BigDecimal("7.82"), response.getTotal());
        assertEquals(100, response.getPontosUtilizados());
        assertEquals(0, cliente.getPontos());
    }

    @Test
    @DisplayName("Deve buscar pedido por ID com sucesso")
    void deveBuscarPedidoPorId() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(cliente);
        pedido.setUnidade(unidade);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setValorTotal(new BigDecimal("29.00"));
        pedido.setDesconto(BigDecimal.ZERO);
        pedido.setFormaPagamento(FormaPagamento.PIX);
        pedido.setCriadoEm(LocalDateTime.now());
        pedido.setItens(new ArrayList<>());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        PedidoResponseDTO response = pedidoService.buscarPorId(1L, "cliente@raizes.com");

        assertNotNull(response);
        assertEquals(1L, response.getPedidoId());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, response.getStatus());
        verify(autorizacaoPedidoService).verificarConsulta(pedido, "cliente@raizes.com");
    }

    @Test
    @DisplayName("Cliente deve listar somente os proprios pedidos")
    void clienteDeveListarSomenteOsPropriosPedidos() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(cliente);
        pedido.setUnidade(unidade);
        pedido.setItens(new ArrayList<>());

        PageRequest pageable = PageRequest.of(0, 10);
        when(autorizacaoPedidoService.obterEscopo("cliente@raizes.com"))
                .thenReturn(new AutorizacaoPedidoService.EscopoAcesso(
                        cliente.getId(), Perfil.CLIENTE, List.of()));
        when(pedidoRepository.findByUsuarioId(cliente.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(pedido), pageable, 1));

        Page<PedidoResponseDTO> resultado = pedidoService.listarFiltrado(
                null, null, pageable, "cliente@raizes.com");

        assertEquals(1, resultado.getTotalElements());
        verify(pedidoRepository).findByUsuarioId(cliente.getId(), pageable);
        verify(pedidoRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Funcionario deve listar somente pedidos das unidades vinculadas")
    void funcionarioDeveListarSomentePedidosDasUnidadesVinculadas() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(autorizacaoPedidoService.obterEscopo("atendente@raizes.com"))
                .thenReturn(new AutorizacaoPedidoService.EscopoAcesso(
                        atendente.getId(), Perfil.ATENDENTE, List.of(1L, 2L)));
        when(pedidoRepository.findByUnidadeIdIn(List.of(1L, 2L), pageable))
                .thenReturn(Page.empty(pageable));

        pedidoService.listarFiltrado(null, null, pageable, "atendente@raizes.com");

        verify(pedidoRepository).findByUnidadeIdIn(List.of(1L, 2L), pageable);
        verify(pedidoRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Funcionario nao deve contornar o escopo usando filtro de unidade")
    void funcionarioNaoDeveFiltrarUnidadeForaDoEscopo() {
        PageRequest pageable = PageRequest.of(0, 10);
        when(autorizacaoPedidoService.obterEscopo("atendente@raizes.com"))
                .thenReturn(new AutorizacaoPedidoService.EscopoAcesso(
                        atendente.getId(), Perfil.ATENDENTE, List.of(1L, 2L)));

        assertThrows(AccessDeniedException.class, () -> pedidoService.listarFiltrado(
                CanalPedido.APP, StatusPedido.RECEBIDO, 99L, pageable, "atendente@raizes.com"));

        verifyNoInteractions(pedidoRepository);
    }

    @Test
    @DisplayName("Deve interromper consulta quando usuario nao tem acesso ao pedido")
    void deveInterromperConsultaQuandoUsuarioNaoTemAcesso() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        doThrow(new AccessDeniedException("Acesso negado"))
                .when(autorizacaoPedidoService)
                .verificarConsulta(pedido, "cliente@raizes.com");

        assertThrows(AccessDeniedException.class, () -> pedidoService.buscarPorId(
                1L, "cliente@raizes.com"));
    }

    @Test
    @DisplayName("Deve impedir atualizacao de status sem pagamento")
    void deveImpedirAtualizacaoSemPagamento() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.atualizarStatus(1L, StatusPedido.EM_PREPARACAO, "cozinha@raizes.com"));

        assertEquals("PAGAMENTO_PENDENTE", exception.getErrorCode());
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve impedir saltos, retrocessos e repeticao de status")
    void deveImpedirTransicoesOperacionaisInvalidas() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        StatusPedido[][] transicoesInvalidas = {
                {StatusPedido.RECEBIDO, StatusPedido.PRONTO},
                {StatusPedido.PRONTO, StatusPedido.EM_PREPARACAO},
                {StatusPedido.EM_PREPARACAO, StatusPedido.EM_PREPARACAO}
        };

        for (StatusPedido[] transicao : transicoesInvalidas) {
            pedido.setStatus(transicao[0]);
            RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                    () -> pedidoService.atualizarStatus(1L, transicao[1], "admin@raizes.com"));

            assertEquals("TRANSICAO_STATUS_INVALIDA", exception.getErrorCode());
            assertEquals(transicao[0], pedido.getStatus());
        }

        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar cancelamento pela atualizacao de status")
    void deveRejeitarCancelamentoPelaAtualizacaoDeStatus() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setItens(new ArrayList<>());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.atualizarStatus(1L, StatusPedido.CANCELADO, "admin@raizes.com"));

        assertEquals("CANCELAMENTO_REQUER_ROTA_DEDICADA", exception.getErrorCode());
        assertEquals(StatusPedido.AGUARDANDO_PAGAMENTO, pedido.getStatus());
        verify(autorizacaoPedidoService).verificarOperacao(pedido, "admin@raizes.com");
        verifyNoInteractions(reservaPedidoService, pagamentoService);
        verify(pedidoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir cliente cancelar pedido pendente pelo endpoint dedicado")
    void devePermitirClienteCancelarPedidoPendente() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.AGUARDANDO_PAGAMENTO);
        pedido.setItens(new ArrayList<>());
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(reservaPedidoService.cancelarEDevolverEstoque(pedido, "CANCELADO_PELO_USUARIO"))
                .thenAnswer(invocation -> {
                    pedido.setStatus(StatusPedido.CANCELADO);
                    return true;
                });

        PedidoResponseDTO response = pedidoService.cancelarPedido(1L, "cliente@raizes.com");

        assertEquals(StatusPedido.CANCELADO, response.getStatus());
        verify(pedidoRepository).findByIdForUpdate(1L);
        verify(pedidoRepository, never()).findById(1L);
        verify(autorizacaoPedidoService).verificarCancelamento(pedido, "cliente@raizes.com");
        verify(reservaPedidoService).cancelarEDevolverEstoque(pedido, "CANCELADO_PELO_USUARIO");
    }

    @Test
    @DisplayName("Deve atribuir pontos ao cliente quando pedido e entregue")
    void deveAtribuirPontosQuandoEntregue() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(cliente);
        pedido.setStatus(StatusPedido.PRONTO);
        pedido.setValorTotal(new BigDecimal("50.00"));
        pedido.setItens(new ArrayList<>());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        pedidoService.atualizarStatus(1L, StatusPedido.ENTREGUE, "cliente@raizes.com");

        assertEquals(150, cliente.getPontos());
        assertEquals(50, pedido.getPontosConcedidos());
        verify(usuarioRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve amortizar saldo devedor antes de disponibilizar novos pontos")
    void deveAmortizarSaldoDevedorComPontosDePedidoEntregue() {
        cliente.setPontos(-20);
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(cliente);
        pedido.setStatus(StatusPedido.PRONTO);
        pedido.setValorTotal(new BigDecimal("50.00"));
        pedido.setItens(new ArrayList<>());

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenReturn(pedido);

        pedidoService.atualizarStatus(1L, StatusPedido.ENTREGUE, "cliente@raizes.com");

        assertEquals(30, cliente.getPontos());
        assertEquals(50, pedido.getPontosConcedidos());
        verify(usuarioRepository).save(cliente);
    }

    @Test
    @DisplayName("Deve cancelar pedido pago e registrar estorno financeiro")
    void deveCancelarPedidoPagoComEstorno() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(cliente);
        pedido.setUnidade(unidade);
        pedido.setStatus(StatusPedido.RECEBIDO);
        ItemPedido item = new ItemPedido();
        item.setProduto(produto);
        item.setQuantidade(2);
        pedido.setItens(List.of(item));
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        PedidoResponseDTO response = pedidoService.cancelarPedido(1L, "cliente@raizes.com");

        assertEquals(StatusPedido.CANCELADO, response.getStatus());
        verify(pagamentoService).estornarPagamento(pedido);
        verify(estoqueService).estornarEstoque(produto.getId(), unidade.getId(), 2);
        verify(descontoService).estornarBeneficios(pedido);
        verify(pedidoRepository).save(pedido);
    }

    @Test
    @DisplayName("Deve preservar pedido pago quando gateway de estorno esta indisponivel")
    void devePreservarPedidoQuandoGatewayDeEstornoIndisponivel() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(cliente);
        pedido.setUnidade(unidade);
        pedido.setStatus(StatusPedido.RECEBIDO);
        pedido.setItens(new ArrayList<>());
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(pagamentoService.estornarPagamento(pedido))
                .thenThrow(new GatewayPagamentoIndisponivelException());

        assertThrows(GatewayPagamentoIndisponivelException.class,
                () -> pedidoService.cancelarPedido(1L, "cliente@raizes.com"));

        assertEquals(StatusPedido.RECEBIDO, pedido.getStatus());
        verify(pedidoRepository, never()).save(any());
        verifyNoInteractions(estoqueService, descontoService);
    }

    @Test
    @DisplayName("Nao deve devolver ao estoque pedido que ja foi preparado")
    void naoDeveDevolverEstoqueDePedidoPreparadoAoCancelar() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(cliente);
        pedido.setUnidade(unidade);
        pedido.setStatus(StatusPedido.PRONTO);
        pedido.setItens(new ArrayList<>());
        when(pedidoRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(pedido)).thenReturn(pedido);

        pedidoService.cancelarPedido(1L, "cliente@raizes.com");

        verify(pagamentoService).estornarPagamento(pedido);
        verifyNoInteractions(estoqueService);
    }

    @Test
    @DisplayName("Deve impedir alteracao de pedido ja entregue")
    void deveImpedirAlteracaoPedidoJaEntregue() {
        Pedido pedido = new Pedido();
        pedido.setId(1L);
        pedido.setStatus(StatusPedido.ENTREGUE);

        when(pedidoRepository.findById(1L)).thenReturn(Optional.of(pedido));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> pedidoService.atualizarStatus(1L, StatusPedido.EM_PREPARACAO, "admin@raizes.com"));

        assertEquals("PEDIDO_FINALIZADO", exception.getErrorCode());
        verify(pedidoRepository, never()).save(any());
    }
}
