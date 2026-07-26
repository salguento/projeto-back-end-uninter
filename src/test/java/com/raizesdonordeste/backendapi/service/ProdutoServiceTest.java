package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.ProdutoEstoqueDTO;
import com.raizesdonordeste.backendapi.dto.ProdutoDTO;
import com.raizesdonordeste.backendapi.dto.ProdutoResponseDTO;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProdutoService - Testes de Logica de Negocio")
class ProdutoServiceTest {

    @Mock private ProdutoRepository produtoRepository;
    @Mock private EstoqueRepository estoqueRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private AcessoUnidadeService acessoUnidadeService;

    @InjectMocks
    private ProdutoService produtoService;

    private Produto produto;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Tapioca Tradicional");
        produto.setDescricao("Tapioca com queijo");
        produto.setPrecoVigente(new BigDecimal("14.50"));
        produto.setAtivo(true);
    }

    private ProdutoDTO criarProdutoDTO(String nome, BigDecimal preco) {
        ProdutoDTO dto = new ProdutoDTO();
        dto.setNome(nome);
        dto.setDescricao("Descricao");
        dto.setPrecoVigente(preco);
        dto.setAtivo(true);
        return dto;
    }

    private void configurarMockSave() {
        when(produtoRepository.save(any(Produto.class))).thenAnswer(invocation -> {
            Produto p = invocation.getArgument(0);
            if (p.getId() == null) {
                p.setId(10L);
            }
            return p;
        });
    }

    // ============================================
    // TESTES DE CRIACAO - SUCESSO
    // ============================================

    @Test
    @DisplayName("Deve criar produto com sucesso")
    void deveCriarProdutoComSucesso() {
        ProdutoDTO dto = criarProdutoDTO("Novo Produto", new BigDecimal("20.00"));
        configurarMockSave();

        ProdutoResponseDTO response = produtoService.criar(dto);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Novo Produto", response.getNome());
        assertEquals(new BigDecimal("20.00"), response.getPrecoVigente());
        verify(produtoRepository).save(any(Produto.class));
    }

    @Test
    @DisplayName("Deve lancar excecao quando nome tem menos de 3 caracteres")
    void deveLancarExcecaoQuandoNomeMuitoCurto() {
        ProdutoDTO dto = criarProdutoDTO("AB", new BigDecimal("20.00"));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> produtoService.criar(dto));

        assertEquals("NOME_INVALIDO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE BUSCA POR ID
    // ============================================

    @Test
    @DisplayName("Deve buscar produto por ID com sucesso")
    void deveBuscarProdutoPorId() {
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.of(produto));

        ProdutoResponseDTO response = produtoService.buscarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Tapioca Tradicional", response.getNome());
        verify(produtoRepository).findByIdAndAtivoTrue(1L);
        verify(produtoRepository, never()).findById(1L);
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto nao existe")
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        when(produtoRepository.findByIdAndAtivoTrue(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.buscarPorId(999L));

        assertEquals("PRODUTO_NAO_ENCONTRADO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Não deve expor produto inativo na consulta pública individual")
    void naoDeveExporProdutoInativoNaConsultaPublicaIndividual() {
        when(produtoRepository.findByIdAndAtivoTrue(1L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.buscarPorId(1L));

        assertEquals("PRODUTO_NAO_ENCONTRADO", exception.getErrorCode());
        verify(produtoRepository, never()).findById(1L);
    }

    // ============================================
    // TESTES DE LISTAGEM
    // ============================================

    @Test
    @DisplayName("Deve listar o catalogo publico somente pela consulta de produtos ativos")
    void deveListarCatalogoPublicoSomenteComProdutosAtivos() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Produto> page = new PageImpl<>(List.of(produto));

        when(produtoRepository.findByAtivoTrue(pageable)).thenReturn(page);

        Page<ProdutoResponseDTO> response = produtoService.listarAtivos(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("Tapioca Tradicional", response.getContent().get(0).getNome());
        assertTrue(response.getContent().get(0).getAtivo());
        verify(produtoRepository).findByAtivoTrue(pageable);
        verify(produtoRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("Deve listar produtos com estoque de uma unidade")
    void deveListarProdutosComEstoqueDaUnidade() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Matriz");
        unidade.setAtivo(true);

        Produto produto2 = new Produto();
        produto2.setId(2L);
        produto2.setNome("Cuscuz Completo");
        produto2.setPrecoVigente(new BigDecimal("19.90"));
        produto2.setAtivo(true);

        Estoque estoque1 = new Estoque();
        estoque1.setProduto(produto);
        estoque1.setQuantidade(50);

        Estoque estoque2 = new Estoque();
        estoque2.setProduto(produto2);
        estoque2.setQuantidade(30);

        Pageable pageable = PageRequest.of(0, 10);
        when(produtoRepository.findByAtivoTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(produto, produto2), pageable, 2));
        when(estoqueRepository.findByUnidadeId(1L)).thenReturn(List.of(estoque1, estoque2));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        Page<ProdutoEstoqueDTO> resultado = produtoService.listarComEstoqueNaUnidade(1L, pageable);

        assertEquals(2, resultado.getTotalElements());

        ProdutoEstoqueDTO dto1 = resultado.getContent().stream()
                .filter(p -> p.getProdutoId().equals(1L)).findFirst().orElseThrow();
        assertEquals(50, dto1.getEstoqueDisponivel());

        ProdutoEstoqueDTO dto2 = resultado.getContent().stream()
                .filter(p -> p.getProdutoId().equals(2L)).findFirst().orElseThrow();
        assertEquals(30, dto2.getEstoqueDisponivel());
    }

    @Test
    @DisplayName("Deve retornar estoque zero para produto sem estoque na unidade")
    void deveRetornarEstoqueZeroQuandoNaoHaEstoque() {
        Unidade unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Matriz");
        unidade.setAtivo(true);

        Pageable pageable = PageRequest.of(0, 10);
        when(produtoRepository.findByAtivoTrue(pageable))
                .thenReturn(new PageImpl<>(List.of(produto), pageable, 1));
        when(estoqueRepository.findByUnidadeId(1L)).thenReturn(List.of());
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(1L);

        Page<ProdutoEstoqueDTO> resultado = produtoService.listarComEstoqueNaUnidade(1L, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(0, resultado.getContent().get(0).getEstoqueDisponivel());
    }

    @Test
    @DisplayName("Deve rejeitar consulta de disponibilidade em unidade inexistente")
    void deveRejeitarConsultaEmUnidadeInexistente() {
        when(unidadeRepository.findById(999L)).thenReturn(Optional.empty());
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(999L);

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.listarComEstoqueNaUnidade(999L, PageRequest.of(0, 10)));

        assertEquals("UNIDADE_NAO_ENCONTRADA", exception.getErrorCode());
        verifyNoInteractions(produtoRepository, estoqueRepository);
    }

    @Test
    @DisplayName("Deve rejeitar consulta de disponibilidade em unidade inativa")
    void deveRejeitarConsultaEmUnidadeInativa() {
        Unidade unidade = new Unidade();
        unidade.setId(2L);
        unidade.setNome("Unidade desativada");
        unidade.setAtivo(false);

        when(unidadeRepository.findById(2L)).thenReturn(Optional.of(unidade));
        doNothing().when(acessoUnidadeService).verificarAcessoUnidade(2L);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> produtoService.listarComEstoqueNaUnidade(2L, PageRequest.of(0, 10)));

        assertEquals("UNIDADE_INATIVA", exception.getErrorCode());
        verifyNoInteractions(produtoRepository, estoqueRepository);
    }

    // ============================================
    // TESTES DE ATUALIZACAO
    // ============================================

    @Test
    @DisplayName("Deve atualizar produto com sucesso")
    void deveAtualizarProdutoComSucesso() {
        ProdutoDTO dto = criarProdutoDTO("Tapioca Atualizada", new BigDecimal("16.00"));

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(produto)).thenReturn(produto);

        ProdutoResponseDTO response = produtoService.atualizar(1L, dto);

        assertEquals("Tapioca Atualizada", response.getNome());
        assertEquals(new BigDecimal("16.00"), response.getPrecoVigente());
        verify(produtoRepository).save(produto);
    }

    @Test
    @DisplayName("Deve lancar excecao quando preco e invalido na atualizacao")
    void deveLancarExcecaoQuandoPrecoInvalidoNaAtualizacao() {
        ProdutoDTO dto = criarProdutoDTO("Produto Valido", BigDecimal.ZERO);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> produtoService.atualizar(1L, dto));

        assertEquals("PRECO_INVALIDO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto nao existe na atualizacao")
    void deveLancarExcecaoQuandoProdutoNaoExisteNaAtualizacao() {
        ProdutoDTO dto = criarProdutoDTO("Produto", new BigDecimal("10.00"));

        when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.atualizar(999L, dto));

        assertEquals("PRODUTO_NAO_ENCONTRADO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE DESATIVACAO (SOFT-DELETE)
    // ============================================

    @Test
    @DisplayName("Deve desativar produto com sucesso")
    void deveDesativarProdutoComSucesso() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(produtoRepository.save(produto)).thenReturn(produto);

        produtoService.desativar(1L);

        assertFalse(produto.getAtivo());
        verify(produtoRepository).save(produto);
        verify(produtoRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto nao existe na desativacao")
    void deveLancarExcecaoQuandoProdutoNaoExisteNaDesativacao() {
        when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> produtoService.desativar(999L));

        assertEquals("PRODUTO_NAO_ENCONTRADO", exception.getErrorCode());
        verify(produtoRepository, never()).save(any());
    }
}
