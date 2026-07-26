package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.model.Pedido;
import com.raizesdonordeste.backendapi.model.Perfil;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.model.Usuario;
import com.raizesdonordeste.backendapi.model.UsuarioUnidade;
import com.raizesdonordeste.backendapi.repository.UsuarioRepository;
import com.raizesdonordeste.backendapi.repository.UsuarioUnidadeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AutorizacaoPedidoService - Autorizacao por proprietario e unidade")
class AutorizacaoPedidoServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private UsuarioUnidadeRepository usuarioUnidadeRepository;

    @InjectMocks
    private AutorizacaoPedidoService autorizacaoPedidoService;

    private Usuario cliente;
    private Usuario outroCliente;
    private Usuario atendente;
    private Usuario gerente;
    private Usuario admin;
    private Unidade unidade1;
    private Unidade unidade2;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        cliente = usuario(1L, "cliente@raizes.com", Perfil.CLIENTE);
        outroCliente = usuario(2L, "outro@raizes.com", Perfil.CLIENTE);
        atendente = usuario(3L, "atendente@raizes.com", Perfil.ATENDENTE);
        gerente = usuario(4L, "gerente@raizes.com", Perfil.GERENTE);
        admin = usuario(5L, "admin@raizes.com", Perfil.ADMIN);

        unidade1 = unidade(1L);
        unidade2 = unidade(2L);

        pedido = new Pedido();
        pedido.setId(10L);
        pedido.setUsuario(cliente);
        pedido.setUnidade(unidade1);
    }

    @Test
    @DisplayName("Cliente pode consultar o proprio pedido")
    void clientePodeConsultarProprioPedido() {
        configurarEscopo(cliente, List.of());

        assertDoesNotThrow(() -> autorizacaoPedidoService.verificarConsulta(
                pedido, cliente.getEmail()));
    }

    @Test
    @DisplayName("Cliente nao pode consultar pedido de outro cliente")
    void clienteNaoPodeConsultarPedidoDeOutroCliente() {
        configurarEscopo(outroCliente, List.of());

        assertThrows(AccessDeniedException.class, () -> autorizacaoPedidoService.verificarConsulta(
                pedido, outroCliente.getEmail()));
    }

    @Test
    @DisplayName("Funcionario pode consultar pedido de unidade vinculada")
    void funcionarioPodeConsultarPedidoDeUnidadeVinculada() {
        configurarEscopo(atendente, List.of(unidade1));

        assertDoesNotThrow(() -> autorizacaoPedidoService.verificarConsulta(
                pedido, atendente.getEmail()));
    }

    @Test
    @DisplayName("Funcionario nao pode consultar pedido de unidade nao vinculada")
    void funcionarioNaoPodeConsultarPedidoDeUnidadeNaoVinculada() {
        configurarEscopo(atendente, List.of(unidade2));

        assertThrows(AccessDeniedException.class, () -> autorizacaoPedidoService.verificarConsulta(
                pedido, atendente.getEmail()));
    }

    @Test
    @DisplayName("Cliente nao pode pagar pedido de outro cliente")
    void clienteNaoPodePagarPedidoDeOutroCliente() {
        configurarEscopo(outroCliente, List.of());

        assertThrows(AccessDeniedException.class, () -> autorizacaoPedidoService.verificarPagamento(
                pedido, outroCliente.getEmail()));
    }

    @Test
    @DisplayName("Atendente pode pagar pedido de unidade vinculada")
    void atendentePodePagarPedidoDeUnidadeVinculada() {
        configurarEscopo(atendente, List.of(unidade1));

        assertDoesNotThrow(() -> autorizacaoPedidoService.verificarPagamento(
                pedido, atendente.getEmail()));
    }

    @Test
    @DisplayName("Gerente nao pode processar pagamento")
    void gerenteNaoPodeProcessarPagamento() {
        configurarEscopo(gerente, List.of(unidade1));

        assertThrows(AccessDeniedException.class, () -> autorizacaoPedidoService.verificarPagamento(
                pedido, gerente.getEmail()));
    }

    @Test
    @DisplayName("Cliente pode cancelar o proprio pedido")
    void clientePodeCancelarProprioPedido() {
        configurarEscopo(cliente, List.of());

        assertDoesNotThrow(() -> autorizacaoPedidoService.verificarCancelamento(
                pedido, cliente.getEmail()));
    }

    @Test
    @DisplayName("Cliente nao pode cancelar pedido de outro cliente")
    void clienteNaoPodeCancelarPedidoDeOutroCliente() {
        configurarEscopo(outroCliente, List.of());

        assertThrows(AccessDeniedException.class, () -> autorizacaoPedidoService.verificarCancelamento(
                pedido, outroCliente.getEmail()));
    }

    @Test
    @DisplayName("Gerente nao pode usar cancelamento reservado ao cliente e atendente")
    void gerenteNaoPodeUsarCancelamentoDoCliente() {
        configurarEscopo(gerente, List.of(unidade1));

        assertThrows(AccessDeniedException.class, () -> autorizacaoPedidoService.verificarCancelamento(
                pedido, gerente.getEmail()));
    }

    @Test
    @DisplayName("Admin possui acesso operacional global")
    void adminPossuiAcessoOperacionalGlobal() {
        configurarEscopo(admin, List.of());

        assertDoesNotThrow(() -> autorizacaoPedidoService.verificarOperacao(
                pedido, admin.getEmail()));
    }

    @Test
    @DisplayName("Escopo remove unidades duplicadas")
    void escopoRemoveUnidadesDuplicadas() {
        configurarEscopo(atendente, List.of(unidade1, unidade1, unidade2));

        AutorizacaoPedidoService.EscopoAcesso escopo = autorizacaoPedidoService.obterEscopo(
                atendente.getEmail());

        assertEquals(List.of(1L, 2L), escopo.unidadeIds());
    }

    private void configurarEscopo(Usuario usuario, List<Unidade> unidades) {
        when(usuarioRepository.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(usuarioUnidadeRepository.findByUsuarioId(usuario.getId()))
                .thenReturn(unidades.stream().map(unidade -> vinculo(usuario, unidade)).toList());
    }

    private Usuario usuario(Long id, String email, Perfil perfil) {
        Usuario usuario = new Usuario();
        usuario.setId(id);
        usuario.setEmail(email);
        usuario.setPerfil(perfil);
        return usuario;
    }

    private Unidade unidade(Long id) {
        Unidade unidade = new Unidade();
        unidade.setId(id);
        return unidade;
    }

    private UsuarioUnidade vinculo(Usuario usuario, Unidade unidade) {
        UsuarioUnidade vinculo = new UsuarioUnidade();
        vinculo.setUsuario(usuario);
        vinculo.setUnidade(unidade);
        return vinculo;
    }
}
