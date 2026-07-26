package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.*;
import com.raizesdonordeste.backendapi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DescontoService - Motor de Descontos")
class DescontoServiceTest {

    @Mock
    private CampanhaRepository campanhaRepository;

    @Mock
    private CupomRepository cupomRepository;

    @Mock
    private CupomUsadoRepository cupomUsadoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private DescontoService descontoService;

    private Produto produto;
    private Unidade unidade;
    private Usuario usuario;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Tapioca");
        produto.setPrecoVigente(new BigDecimal("14.50"));

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Centro");

        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setEmail("cliente@raizes.com");

        pedido = new Pedido();
        pedido.setId(1L);
        pedido.setUsuario(usuario);
    }

    private Cupom criarCupomBase() {
        Cupom cupom = new Cupom();
        cupom.setId(1L);
        cupom.setCodigo("TESTE");
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValor(new BigDecimal("10"));
        cupom.setValorMinimoPedido(BigDecimal.ZERO);
        cupom.setDataInicio(LocalDateTime.now().minusDays(1));
        cupom.setDataFim(LocalDateTime.now().plusDays(30));
        cupom.setAtivo(true);
        cupom.setUsoAtual(0);
        return cupom;
    }

    // ============================================
    // TESTES DE CAMPANHAS (obterPrecoDoProduto)
    // ============================================

    @Test
    @DisplayName("Deve retornar preco promocional quando ha campanha ativa")
    void deveRetornarPrecoPromocional() {
        Campanha campanha = new Campanha();
        campanha.setNome("Terca da Tapioca");
        campanha.setPrecoPromocional(new BigDecimal("9.90"));

        when(campanhaRepository.findCampanhaAtiva(eq(1L), eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(campanha));

        BigDecimal preco = descontoService.obterPrecoDoProduto(produto, 1L);

        assertEquals(new BigDecimal("9.90"), preco);
    }

    @Test
    @DisplayName("Deve retornar preco vigente quando nao ha campanha")
    void deveRetornarPrecoVigente() {
        when(campanhaRepository.findCampanhaAtiva(anyLong(), anyLong(), any(LocalDateTime.class)))
                .thenReturn(Optional.empty());

        BigDecimal preco = descontoService.obterPrecoDoProduto(produto, 1L);

        assertEquals(new BigDecimal("14.50"), preco);
    }

    @Test
    @DisplayName("Deve lancar excecao quando preco promocional e negativo")
    void deveLancarExcecaoQuandoPrecoPromocionalNegativo() {
        Campanha campanha = new Campanha();
        campanha.setNome("Campanha Invalida");
        campanha.setPrecoPromocional(new BigDecimal("-5.00"));

        when(campanhaRepository.findCampanhaAtiva(eq(1L), eq(1L), any(LocalDateTime.class)))
                .thenReturn(Optional.of(campanha));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> descontoService.obterPrecoDoProduto(produto, 1L));

        assertEquals("VALOR_INVALIDO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE VALIDACAO DE CUPOM (validarCupom)
    // ============================================

    @Test
    @DisplayName("Deve retornar ZERO quando codigo do cupom e nulo")
    void deveRetornarZeroQuandoCodigoNulo() {
        BigDecimal desconto = descontoService.validarCupom(null, new BigDecimal("100"), 1L);

        assertEquals(BigDecimal.ZERO, desconto);
    }

    @Test
    @DisplayName("Deve lancar excecao para cupom inexistente")
    void deveLancarExcecaoCupomInexistente() {
        when(cupomRepository.findByCodigo("INVALIDO")).thenReturn(Optional.empty());

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> descontoService.validarCupom("INVALIDO", new BigDecimal("50"), 1L));

        assertEquals("CUPOM_INVALIDO", ex.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao para cupom expirado")
    void deveLancarExcecaoCupomExpirado() {
        Cupom cupom = criarCupomBase();
        cupom.setDataInicio(LocalDateTime.now().minusDays(30));
        cupom.setDataFim(LocalDateTime.now().minusDays(1));

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> descontoService.validarCupom("TESTE", new BigDecimal("50"), 1L));

        assertEquals("CUPOM_EXPIRADO", ex.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao para cupom inativo")
    void deveLancarExcecaoCupomInativo() {
        Cupom cupom = criarCupomBase();
        cupom.setAtivo(false);

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> descontoService.validarCupom("TESTE", new BigDecimal("50"), 1L));

        assertEquals("CUPOM_INATIVO", ex.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando valor do pedido e inferior ao minimo")
    void deveLancarExcecaoValorMinimo() {
        Cupom cupom = criarCupomBase();
        cupom.setValorMinimoPedido(new BigDecimal("100"));

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> descontoService.validarCupom("TESTE", new BigDecimal("50"), 1L));

        assertEquals("CUPOM_VALOR_MINIMO", ex.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando cupom e de unidade diferente")
    void deveLancarExcecaoUnidadeDiferente() {
        Cupom cupom = criarCupomBase();
        Unidade unidadeCupom = new Unidade();
        unidadeCupom.setId(2L);
        unidadeCupom.setNome("Zona Sul");
        cupom.setUnidade(unidadeCupom);

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> descontoService.validarCupom("TESTE", new BigDecimal("50"), 1L));

        assertEquals("CUPOM_UNIDADE_INVALIDA", ex.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando cupom atingiu limite de usos")
    void deveLancarExcecaoLimiteUsos() {
        Cupom cupom = criarCupomBase();
        cupom.setUsoMaximo(10);
        cupom.setUsoAtual(10);

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        RegraNegocioException ex = assertThrows(RegraNegocioException.class,
                () -> descontoService.validarCupom("TESTE", new BigDecimal("50"), 1L));

        assertEquals("CUPOM_ESGOTADO", ex.getErrorCode());
    }

    @Test
    @DisplayName("Deve calcular desconto percentual corretamente")
    void deveCalcularDescontoPercentual() {
        Cupom cupom = criarCupomBase();
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValor(new BigDecimal("10"));

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        BigDecimal desconto = descontoService.validarCupom("TESTE", new BigDecimal("100"), 1L);

        assertEquals(new BigDecimal("10.00"), desconto);
    }

    @Test
    @DisplayName("Deve calcular desconto nominal corretamente")
    void deveCalcularDescontoNominal() {
        Cupom cupom = criarCupomBase();
        cupom.setTipoDesconto(TipoDesconto.NOMINAL);
        cupom.setValor(new BigDecimal("15"));

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        BigDecimal desconto = descontoService.validarCupom("TESTE", new BigDecimal("100"), 1L);

        assertEquals(new BigDecimal("15"), desconto);
    }

    @Test
    @DisplayName("Deve limitar desconto nominal ao valor do pedido")
    void deveLimitarDescontoNominalAoValorDoPedido() {
        Cupom cupom = criarCupomBase();
        cupom.setTipoDesconto(TipoDesconto.NOMINAL);
        cupom.setValor(new BigDecimal("200"));

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));

        BigDecimal desconto = descontoService.validarCupom("TESTE", new BigDecimal("100"), 1L);

        assertEquals(new BigDecimal("100"), desconto);
    }

    @Test
    @DisplayName("Deve lancar excecao quando subtotal e negativo")
    void deveLancarExcecaoQuandoSubtotalNegativo() {
        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> descontoService.validarCupom("TESTE", new BigDecimal("-50"), 1L));

        assertEquals("SUBTOTAL_INVALIDO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE REGISTRO DE USO (registrarUsoCupom)
    // ============================================

    @Test
    @DisplayName("Deve registrar uso do cupom e incrementar contador")
    void deveRegistrarUsoDoCupom() {
        Cupom cupom = criarCupomBase();
        cupom.setUsoAtual(5);

        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));
        when(cupomRepository.reservarUsoSeDisponivel(cupom.getId())).thenReturn(1);
        when(cupomUsadoRepository.save(any(CupomUsado.class))).thenAnswer(inv -> inv.getArgument(0));

        descontoService.registrarUsoCupom("TESTE", pedido, usuario, new BigDecimal("10.00"));

        verify(cupomUsadoRepository).save(argThat(uso ->
                uso.getCupom().equals(cupom) &&
                uso.getPedido().equals(pedido) &&
                uso.getUsuario().equals(usuario) &&
                uso.getValorDesconto().equals(new BigDecimal("10.00"))
        ));

        verify(cupomRepository).reservarUsoSeDisponivel(cupom.getId());
        verify(cupomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar uso quando outra transacao consumiu a ultima disponibilidade")
    void deveRejeitarCupomEsgotadoDuranteRegistro() {
        Cupom cupom = criarCupomBase();
        cupom.setUsoMaximo(1);
        when(cupomRepository.findByCodigo("TESTE")).thenReturn(Optional.of(cupom));
        when(cupomRepository.reservarUsoSeDisponivel(cupom.getId())).thenReturn(0);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> descontoService.registrarUsoCupom("TESTE", pedido, usuario, new BigDecimal("10.00")));

        assertEquals("CUPOM_ESGOTADO", exception.getErrorCode());
        verify(cupomUsadoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando cupom nao existe para auditoria")
    void deveLancarExcecaoQuandoCupomNaoExisteParaAuditoria() {
        when(cupomRepository.findByCodigo("INEXISTENTE")).thenReturn(Optional.empty());

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> descontoService.registrarUsoCupom("INEXISTENTE", pedido, usuario, new BigDecimal("10.00")));

        assertEquals("CUPOM_INVALIDO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve estornar pontos e cupom preservando a auditoria")
    void deveEstornarBeneficiosDoPedido() {
        usuario.setPontos(25);
        pedido.setPontosUtilizados(40);
        pedido.setPontosConcedidos(10);
        Cupom cupom = criarCupomBase();
        cupom.setUsoAtual(3);
        CupomUsado uso = new CupomUsado();
        uso.setPedido(pedido);
        uso.setCupom(cupom);
        when(cupomUsadoRepository.findByPedidoAndEstornadoEmIsNull(pedido)).thenReturn(Optional.of(uso));
        when(cupomRepository.liberarUso(cupom.getId())).thenReturn(1);

        descontoService.estornarBeneficios(pedido);

        assertEquals(55, usuario.getPontos());
        assertEquals(0, pedido.getPontosUtilizados());
        assertEquals(0, pedido.getPontosConcedidos());
        assertNotNull(uso.getEstornadoEm());
        verify(usuarioRepository).save(usuario);
        verify(cupomRepository).liberarUso(cupom.getId());
        verify(cupomUsadoRepository).save(uso);
    }

    @Test
    @DisplayName("Deve registrar saldo devedor quando pontos concedidos ja foram consumidos")
    void deveRegistrarSaldoDevedorAoEstornarPontosConcedidos() {
        usuario.setPontos(5);
        pedido.setPontosUtilizados(0);
        pedido.setPontosConcedidos(20);
        when(cupomUsadoRepository.findByPedidoAndEstornadoEmIsNull(pedido)).thenReturn(Optional.empty());

        descontoService.estornarBeneficios(pedido);

        assertEquals(-15, usuario.getPontos());
        assertEquals(0, pedido.getPontosUtilizados());
        assertEquals(0, pedido.getPontosConcedidos());
        verify(usuarioRepository).save(usuario);
        verifyNoInteractions(cupomRepository);
    }

    @Test
    @DisplayName("Nao deve estornar beneficios duas vezes")
    void naoDeveEstornarBeneficiosDuasVezes() {
        usuario.setPontos(25);
        pedido.setPontosUtilizados(0);
        when(cupomUsadoRepository.findByPedidoAndEstornadoEmIsNull(pedido)).thenReturn(Optional.empty());

        descontoService.estornarBeneficios(pedido);

        assertEquals(25, usuario.getPontos());
        verifyNoInteractions(usuarioRepository, cupomRepository);
        verify(cupomUsadoRepository, never()).save(any());
    }
}
