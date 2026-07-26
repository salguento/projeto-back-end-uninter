package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.EstoqueDTO;
import com.raizesdonordeste.backendapi.dto.EstoqueResponseDTO;
import com.raizesdonordeste.backendapi.dto.MovimentacaoEstoqueRequestDTO;
import com.raizesdonordeste.backendapi.dto.MovimentacaoEstoqueResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EstoqueService - Testes de Logica de Negocio")
class EstoqueServiceTest {

    @Mock private EstoqueRepository estoqueRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private MovimentacaoEstoqueRepository movimentacaoEstoqueRepository;
    @Mock private AcessoUnidadeService acessoUnidadeService;

    @InjectMocks
    private EstoqueService estoqueService;

    private Produto produto;
    private Unidade unidade;
    private Estoque estoque;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Tapioca Tradicional");
        produto.setAtivo(true);

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Unidade Centro");

        estoque = new Estoque();
        estoque.setId(1L);
        estoque.setProduto(produto);
        estoque.setUnidade(unidade);
        estoque.setQuantidade(50);
    }

    private EstoqueDTO criarEstoqueDTO(Long produtoId, Long unidadeId, Integer quantidade) {
        EstoqueDTO dto = new EstoqueDTO();
        dto.setProdutoId(produtoId);
        dto.setUnidadeId(unidadeId);
        dto.setQuantidade(quantidade);
        return dto;
    }

    private MovimentacaoEstoqueRequestDTO criarMovimentacao(
            TipoMovimentacaoEstoque tipo, Integer quantidade, String motivo) {
        MovimentacaoEstoqueRequestDTO dto = new MovimentacaoEstoqueRequestDTO();
        dto.setTipo(tipo);
        dto.setQuantidade(quantidade);
        dto.setMotivo(motivo);
        return dto;
    }

    private void configurarMocksCriacaoBasico() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(estoqueRepository.findByUnidadeIdAndProdutoId(1L, 1L)).thenReturn(Optional.empty());
        when(estoqueRepository.save(any(Estoque.class))).thenAnswer(invocation -> {
            Estoque e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);
    }

    // ============================================
    // TESTES DE CRIACAO - SUCESSO
    // ============================================

    @Test
    @DisplayName("Deve criar estoque com sucesso quando dados sao validos")
    void deveCriarEstoqueComSucesso() {
        EstoqueDTO dto = criarEstoqueDTO(1L, 1L, 100);
        configurarMocksCriacaoBasico();

        EstoqueResponseDTO response = estoqueService.criar(dto);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(100, response.getQuantidade());
        assertEquals(1L, response.getProdutoId());
        assertEquals(1L, response.getUnidadeId());
        verify(estoqueRepository).save(any(Estoque.class));
    }

    @Test
    @DisplayName("Não deve criar estoque em unidade inativa")
    void naoDeveCriarEstoqueEmUnidadeInativa() {
        EstoqueDTO dto = criarEstoqueDTO(1L, 1L, 10);
        unidade.setAtivo(false);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> estoqueService.criar(dto));

        assertEquals("UNIDADE_INATIVA", exception.getErrorCode());
        verify(estoqueRepository, never()).save(any());
        verifyNoInteractions(acessoUnidadeService);
    }

    // ============================================
    // TESTES DE CRIACAO - VALIDACOES
    // ============================================

    @Test
    @DisplayName("Deve lancar excecao quando quantidade e negativa")
    void deveLancarExcecaoQuandoQuantidadeNegativa() {
        EstoqueDTO dto = criarEstoqueDTO(1L, 1L, -10);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> estoqueService.criar(dto));

        assertEquals("QUANTIDADE_INVALIDA", exception.getErrorCode());
        verify(produtoRepository, never()).findById(anyLong());
        verify(unidadeRepository, never()).findById(anyLong());
        verify(estoqueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto nao existe")
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        EstoqueDTO dto = criarEstoqueDTO(999L, 1L, 100);

        when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> estoqueService.criar(dto));

        assertEquals("PRODUTO_NAO_ENCONTRADO", exception.getErrorCode());
        verify(unidadeRepository, never()).findById(anyLong());
        verify(estoqueRepository, never()).findByUnidadeIdAndProdutoId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto esta inativo")
    void deveLancarExcecaoQuandoProdutoInativo() {
        produto.setAtivo(false);
        EstoqueDTO dto = criarEstoqueDTO(1L, 1L, 100);

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> estoqueService.criar(dto));

        assertEquals("PRODUTO_INATIVO", exception.getErrorCode());
        verify(unidadeRepository, never()).findById(anyLong());
        verify(estoqueRepository, never()).findByUnidadeIdAndProdutoId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Deve lancar excecao quando unidade nao existe")
    void deveLancarExcecaoQuandoUnidadeNaoExiste() {
        EstoqueDTO dto = criarEstoqueDTO(1L, 999L, 100);

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> estoqueService.criar(dto));

        assertEquals("UNIDADE_NAO_ENCONTRADA", exception.getErrorCode());
        verify(estoqueRepository, never()).findByUnidadeIdAndProdutoId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("Deve lancar excecao quando estoque ja existe")
    void deveLancarExcecaoQuandoEstoqueJaExiste() {
        Estoque estoqueExistente = new Estoque();
        estoqueExistente.setId(1L);
        estoqueExistente.setProduto(produto);
        estoqueExistente.setUnidade(unidade);
        estoqueExistente.setQuantidade(50);

        EstoqueDTO dto = criarEstoqueDTO(1L, 1L, 100);

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(estoqueRepository.findByUnidadeIdAndProdutoId(1L, 1L)).thenReturn(Optional.of(estoqueExistente));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> estoqueService.criar(dto));

        assertEquals("ESTOQUE_JA_EXISTE", exception.getErrorCode());
        verify(estoqueRepository, never()).save(any());
    }

    // ============================================
    // TESTES DE LISTAGEM
    // ============================================

    @Test
    @DisplayName("Deve listar estoque por unidade com sucesso")
    void deveListarEstoquePorUnidadeComSucesso() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Estoque> page = new PageImpl<>(List.of(estoque));

        when(estoqueRepository.findByUnidadeId(1L, pageable)).thenReturn(page);
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        Page<EstoqueResponseDTO> response = estoqueService.listarPorUnidade(1L, pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals(50, response.getContent().get(0).getQuantidade());
    }

    // ============================================
    // TESTES DE BUSCA POR ID
    // ============================================

    @Test
    @DisplayName("Deve buscar estoque por ID com sucesso")
    void deveBuscarEstoquePorIdComSucesso() {
        when(estoqueRepository.findById(1L)).thenReturn(Optional.of(estoque));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        EstoqueResponseDTO response = estoqueService.buscarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals(50, response.getQuantidade());
    }

    @Test
    @DisplayName("Deve lancar excecao quando estoque nao existe na busca")
    void deveLancarExcecaoQuandoEstoqueNaoExisteNaBusca() {
        when(estoqueRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> estoqueService.buscarPorId(999L));

        assertEquals("ESTOQUE_NAO_ENCONTRADO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE ATUALIZACAO
    // ============================================

    @Test
    @DisplayName("Deve atualizar quantidade com sucesso")
    void deveAtualizarQuantidadeComSucesso() {
        EstoqueDTO dto = criarEstoqueDTO(null, null, 75);

        when(estoqueRepository.findById(1L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(estoque)).thenReturn(estoque);
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        EstoqueResponseDTO response = estoqueService.atualizarQuantidade(1L, dto);

        assertEquals(75, response.getQuantidade());
        verify(estoqueRepository).save(estoque);
    }

    @Test
    @DisplayName("Deve lancar excecao quando estoque nao existe na atualizacao")
    void deveLancarExcecaoQuandoEstoqueNaoExisteNaAtualizacao() {
        EstoqueDTO dto = criarEstoqueDTO(null, null, 75);

        when(estoqueRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> estoqueService.atualizarQuantidade(999L, dto));

        assertEquals("ESTOQUE_NAO_ENCONTRADO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE DELECAO
    // ============================================

    @Test
    @DisplayName("Deve deletar estoque com sucesso")
    void deveDeletarEstoqueComSucesso() {
        when(estoqueRepository.findById(1L)).thenReturn(Optional.of(estoque));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);
        doNothing().when(estoqueRepository).deleteById(1L);

        assertDoesNotThrow(() -> estoqueService.deletar(1L));
        verify(estoqueRepository).deleteById(1L);
    }

    @Test
    @DisplayName("Deve registrar entrada com saldo e ator pseudonimizado")
    void deveRegistrarEntradaDeEstoque() {
        MovimentacaoEstoqueRequestDTO dto = criarMovimentacao(
                TipoMovimentacaoEstoque.ENTRADA, 10, "Recebimento do fornecedor");
        when(estoqueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(estoque)).thenReturn(estoque);

        MovimentacaoEstoqueResponseDTO response = estoqueService.movimentar(
                1L, dto, "gerente@raizes.com");

        assertEquals(50, response.getSaldoAnterior());
        assertEquals(60, response.getSaldoPosterior());
        assertEquals(60, estoque.getQuantidade());
        assertTrue(response.getAtor().startsWith("usr_"));
        assertNotEquals("gerente@raizes.com", response.getAtor());
        verify(movimentacaoEstoqueRepository).save(any(MovimentacaoEstoque.class));
    }

    @Test
    @DisplayName("Deve registrar saida quando existe saldo suficiente")
    void deveRegistrarSaidaDeEstoque() {
        MovimentacaoEstoqueRequestDTO dto = criarMovimentacao(
                TipoMovimentacaoEstoque.SAIDA, 15, "Perda operacional");
        when(estoqueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(estoque));
        when(estoqueRepository.save(estoque)).thenReturn(estoque);

        MovimentacaoEstoqueResponseDTO response = estoqueService.movimentar(
                1L, dto, "admin@raizes.com");

        assertEquals(50, response.getSaldoAnterior());
        assertEquals(35, response.getSaldoPosterior());
        assertEquals(TipoMovimentacaoEstoque.SAIDA, response.getTipo());
    }

    @Test
    @DisplayName("Nao deve registrar saida maior que o saldo")
    void naoDeveRegistrarSaidaSemSaldo() {
        MovimentacaoEstoqueRequestDTO dto = criarMovimentacao(
                TipoMovimentacaoEstoque.SAIDA, 51, "Ajuste de inventario");
        when(estoqueRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(estoque));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> estoqueService.movimentar(1L, dto, "admin@raizes.com"));

        assertEquals("ESTOQUE_INSUFICIENTE", exception.getErrorCode());
        assertEquals(50, estoque.getQuantidade());
        verify(estoqueRepository, never()).save(any());
        verify(movimentacaoEstoqueRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve rejeitar movimentacao sem motivo")
    void deveRejeitarMovimentacaoSemMotivo() {
        MovimentacaoEstoqueRequestDTO dto = criarMovimentacao(
                TipoMovimentacaoEstoque.ENTRADA, 10, "  ");

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> estoqueService.movimentar(1L, dto, "admin@raizes.com"));

        assertEquals("MOTIVO_OBRIGATORIO", exception.getErrorCode());
        verifyNoInteractions(estoqueRepository, movimentacaoEstoqueRepository);
    }

    @Test
    @DisplayName("Deve listar historico mais recente do estoque")
    void deveListarHistoricoDeMovimentacoes() {
        Pageable pageable = PageRequest.of(0, 10);
        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setId(1L);
        movimentacao.setEstoque(estoque);
        movimentacao.setTipo(TipoMovimentacaoEstoque.ENTRADA);
        movimentacao.setQuantidade(10);
        movimentacao.setSaldoAnterior(40);
        movimentacao.setSaldoPosterior(50);
        movimentacao.setMotivo("Reposicao");
        movimentacao.setAtor("usr_123456789abc");
        when(estoqueRepository.findById(1L)).thenReturn(Optional.of(estoque));
        when(movimentacaoEstoqueRepository.findByEstoqueIdOrderByCriadoEmDesc(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(movimentacao), pageable, 1));

        Page<MovimentacaoEstoqueResponseDTO> response = estoqueService.listarMovimentacoes(1L, pageable);

        assertEquals(1, response.getTotalElements());
        assertEquals("Reposicao", response.getContent().get(0).getMotivo());
        verify(acessoUnidadeService).verificarAcessoUnidade(1L);
    }
}
