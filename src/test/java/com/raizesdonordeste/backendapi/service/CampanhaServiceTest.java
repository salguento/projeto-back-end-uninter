package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.CampanhaRequestDTO;
import com.raizesdonordeste.backendapi.dto.CampanhaResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Campanha;
import com.raizesdonordeste.backendapi.model.Produto;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.repository.CampanhaRepository;
import com.raizesdonordeste.backendapi.repository.ProdutoRepository;
import com.raizesdonordeste.backendapi.repository.UnidadeRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CampanhaService - Testes de Logica de Negocio")
class CampanhaServiceTest {

    @Mock private CampanhaRepository campanhaRepository;
    @Mock private ProdutoRepository produtoRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private AcessoUnidadeService acessoUnidadeService;

    @InjectMocks
    private CampanhaService campanhaService;

    private Produto produto;
    private Unidade unidade;
    private Campanha campanha;

    @BeforeEach
    void setUp() {
        produto = new Produto();
        produto.setId(1L);
        produto.setNome("Tapioca Tradicional");
        produto.setPrecoVigente(new BigDecimal("14.50"));
        produto.setAtivo(true);

        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Unidade Centro");

        campanha = new Campanha();
        campanha.setId(1L);
        campanha.setNome("Terca da Tapioca");
        campanha.setProduto(produto);
        campanha.setUnidade(unidade);
        campanha.setPrecoPromocional(new BigDecimal("9.90"));
        campanha.setDataInicio(LocalDateTime.now().minusDays(1));
        campanha.setDataFim(LocalDateTime.now().plusDays(30));
        campanha.setAtiva(true);
    }

    private CampanhaRequestDTO criarCampanhaRequestDTO(String nome, BigDecimal preco) {
        CampanhaRequestDTO dto = new CampanhaRequestDTO();
        dto.setNome(nome);
        dto.setProdutoId(1L);
        dto.setUnidadeId(1L);
        dto.setPrecoPromocional(preco);
        dto.setDataInicio(LocalDateTime.now().minusDays(1));
        dto.setDataFim(LocalDateTime.now().plusDays(30));
        dto.setAtiva(true);
        return dto;
    }

    private void configurarMocksCriacao() {
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unidade));
        when(campanhaRepository.findByProdutoIdAndUnidadeIdAndAtivaTrue(1L, 1L))
                .thenReturn(List.of());
        when(campanhaRepository.save(any(Campanha.class))).thenAnswer(invocation -> {
            Campanha c = invocation.getArgument(0);
            if (c.getId() == null) {
                c.setId(10L);
            }
            return c;
        });
    }

    // ============================================
    // TESTES DE CRIACAO - SUCESSO
    // ============================================

    @Test
    @DisplayName("Deve criar campanha com sucesso quando dados sao validos")
    void deveCriarCampanhaComSucesso() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Terca da Tapioca", new BigDecimal("9.90"));
        configurarMocksCriacao();

        CampanhaResponseDTO response = campanhaService.criar(dto);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Terca da Tapioca", response.getNome());
        assertEquals(new BigDecimal("9.90"), response.getPrecoPromocional());
        verify(acessoUnidadeService).verificarAcessoGerencialUnidade(1L);
        verify(unidadeRepository).findByIdForUpdate(1L);
        verify(unidadeRepository, never()).findById(1L);
        verify(campanhaRepository).save(any(Campanha.class));
    }

    @Test
    @DisplayName("Não deve criar campanha em unidade inativa")
    void naoDeveCriarCampanhaEmUnidadeInativa() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha invalida", new BigDecimal("9.90"));
        unidade.setAtivo(false);
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unidade));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> campanhaService.criar(dto));

        assertEquals("UNIDADE_INATIVA", exception.getErrorCode());
        verify(acessoUnidadeService).verificarAcessoGerencialUnidade(1L);
        verify(campanhaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando nome tem menos de 3 caracteres")
    void deveLancarExcecaoQuandoNomeMuitoCurto() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("AB", new BigDecimal("9.90"));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> campanhaService.criar(dto));

        assertEquals("NOME_INVALIDO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando preco promocional e maior ou igual ao vigente")
    void deveLancarExcecaoQuandoPrecoPromocionalMaiorOuIgualVigente() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha Invalida", new BigDecimal("14.50"));

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unidade));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> campanhaService.criar(dto));

        assertEquals("PRECO_INVALIDO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando data de fim e igual a data de inicio")
    void deveLancarExcecaoQuandoDataFimIgualDataInicio() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha Invalida", new BigDecimal("9.90"));
        LocalDateTime data = LocalDateTime.of(2026, 7, 18, 12, 0);
        dto.setDataInicio(data);
        dto.setDataFim(data);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> campanhaService.criar(dto));

        assertEquals("DATA_INVALIDA", exception.getErrorCode());
        verify(produtoRepository, never()).findById(anyLong());
        verify(campanhaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto nao existe")
    void deveLancarExcecaoQuandoProdutoNaoExiste() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha Valida", new BigDecimal("9.90"));
        dto.setProdutoId(999L);

        when(produtoRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> campanhaService.criar(dto));

        assertEquals("PRODUTO_NAO_ENCONTRADO", exception.getErrorCode());
        verify(campanhaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando produto esta inativo")
    void deveLancarExcecaoQuandoProdutoInativo() {
        produto.setAtivo(false);
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha Valida", new BigDecimal("9.90"));

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> campanhaService.criar(dto));

        assertEquals("PRODUTO_INATIVO", exception.getErrorCode());
        verify(campanhaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando unidade nao existe")
    void deveLancarExcecaoQuandoUnidadeNaoExiste() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha Valida", new BigDecimal("9.90"));
        dto.setUnidadeId(999L);

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> campanhaService.criar(dto));

        assertEquals("UNIDADE_NAO_ENCONTRADA", exception.getErrorCode());
        verify(campanhaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando ha sobreposicao de campanha")
    void deveLancarExcecaoQuandoHaSobreposicaoDeCampanha() {
        Campanha campanhaExistente = new Campanha();
        campanhaExistente.setId(2L);
        campanhaExistente.setNome("Campanha Existente");
        campanhaExistente.setProduto(produto);
        campanhaExistente.setUnidade(unidade);
        campanhaExistente.setDataInicio(LocalDateTime.now().minusDays(5));
        campanhaExistente.setDataFim(LocalDateTime.now().plusDays(5));
        campanhaExistente.setAtiva(true);

        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Nova Campanha", new BigDecimal("9.90"));

        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unidade));
        when(campanhaRepository.findByProdutoIdAndUnidadeIdAndAtivaTrue(1L, 1L))
                .thenReturn(List.of(campanhaExistente));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> campanhaService.criar(dto));

        assertEquals("CAMPANHA_SOBREPOSTA", exception.getErrorCode());
        assertTrue(exception.getMessage().contains("Campanha Existente"));
        verify(campanhaRepository, never()).save(any());
    }

    // ============================================
    // TESTES DE LISTAGEM
    // ============================================

    @Test
    @DisplayName("Deve listar todas as campanhas com sucesso")
    void deveListarTodasCampanhas() {
        Pageable pageable = PageRequest.of(0, 10);
        when(campanhaRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(campanha), pageable, 1));
        when(acessoUnidadeService.getUnidadesGerenciaisDoUsuario()).thenReturn(null);

        Page<CampanhaResponseDTO> resultado = campanhaService.listarTodas(pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Terca da Tapioca", resultado.getContent().get(0).getNome());
    }

    @Test
    @DisplayName("Gerente deve listar apenas campanhas das unidades vinculadas")
    void gerenteDeveListarApenasCampanhasDasUnidadesVinculadas() {
        Pageable pageable = PageRequest.of(0, 10);
        when(acessoUnidadeService.getUnidadesGerenciaisDoUsuario()).thenReturn(List.of(1L));
        when(campanhaRepository.findByUnidadeIdIn(List.of(1L), pageable))
                .thenReturn(new PageImpl<>(List.of(campanha), pageable, 1));

        Page<CampanhaResponseDTO> resultado = campanhaService.listarTodas(pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1L, resultado.getContent().get(0).getUnidadeId());
        verify(campanhaRepository, never()).findAll(pageable);
    }

    // ============================================
    // TESTES DE BUSCA POR ID
    // ============================================

    @Test
    @DisplayName("Deve buscar campanha por ID com sucesso")
    void deveBuscarCampanhaPorId() {
        when(campanhaRepository.findById(1L)).thenReturn(Optional.of(campanha));

        CampanhaResponseDTO response = campanhaService.buscarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Terca da Tapioca", response.getNome());
    }

    @Test
    @DisplayName("Deve lancar excecao ao buscar campanha inexistente")
    void deveLancarExcecaoAoBuscarCampanhaInexistente() {
        when(campanhaRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> campanhaService.buscarPorId(999L));

        assertEquals("CAMPANHA_NAO_ENCONTRADA", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE ATUALIZACAO
    // ============================================

    @Test
    @DisplayName("Deve atualizar campanha com sucesso")
    void deveAtualizarCampanhaComSucesso() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Terca da Tapioca Atualizada", new BigDecimal("8.90"));

        when(campanhaRepository.findById(1L)).thenReturn(Optional.of(campanha));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unidade));
        when(campanhaRepository.findByProdutoIdAndUnidadeIdAndAtivaTrue(1L, 1L))
                .thenReturn(List.of());
        when(campanhaRepository.save(campanha)).thenReturn(campanha);

        CampanhaResponseDTO response = campanhaService.atualizar(1L, dto);

        assertEquals("Terca da Tapioca Atualizada", response.getNome());
        assertEquals(new BigDecimal("8.90"), response.getPrecoPromocional());
        verify(campanhaRepository).save(campanha);
    }

    @Test
    @DisplayName("Deve lancar excecao quando ha sobreposicao de campanha na atualizacao")
    void deveLancarExcecaoQuandoHaSobreposicaoDeCampanhaNaAtualizacao() {
        Campanha campanhaExistente = new Campanha();
        campanhaExistente.setId(2L);
        campanhaExistente.setNome("Outra Campanha");
        campanhaExistente.setProduto(produto);
        campanhaExistente.setUnidade(unidade);
        campanhaExistente.setDataInicio(LocalDateTime.now().minusDays(5));
        campanhaExistente.setDataFim(LocalDateTime.now().plusDays(5));
        campanhaExistente.setAtiva(true);

        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha Atualizada", new BigDecimal("9.90"));

        when(campanhaRepository.findById(1L)).thenReturn(Optional.of(campanha));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unidade));
        when(campanhaRepository.findByProdutoIdAndUnidadeIdAndAtivaTrue(1L, 1L))
                .thenReturn(List.of(campanhaExistente));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> campanhaService.atualizar(1L, dto));

        assertEquals("CAMPANHA_SOBREPOSTA", exception.getErrorCode());
        verify(campanhaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve permitir atualizacao da propria campanha sem erro de sobreposicao")
    void devePermitirAtualizacaoDaPropriaCampanha() {
        CampanhaRequestDTO dto = criarCampanhaRequestDTO("Campanha Atualizada", new BigDecimal("9.90"));

        when(campanhaRepository.findById(1L)).thenReturn(Optional.of(campanha));
        when(produtoRepository.findById(1L)).thenReturn(Optional.of(produto));
        when(unidadeRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(unidade));
        when(campanhaRepository.findByProdutoIdAndUnidadeIdAndAtivaTrue(1L, 1L))
                .thenReturn(List.of(campanha));
        when(campanhaRepository.save(campanha)).thenReturn(campanha);

        CampanhaResponseDTO response = campanhaService.atualizar(1L, dto);

        assertNotNull(response);
        assertEquals("Campanha Atualizada", response.getNome());
        verify(campanhaRepository).save(campanha);
    }

    // ============================================
    // TESTES DE DESATIVACAO
    // ============================================

    @Test
    @DisplayName("Deve desativar campanha sem excluir o registro")
    void deveDesativarCampanhaSemExcluirRegistro() {
        when(campanhaRepository.findById(1L)).thenReturn(Optional.of(campanha));

        assertDoesNotThrow(() -> campanhaService.desativar(1L));

        assertFalse(campanha.getAtiva());
        verify(campanhaRepository).save(campanha);
        verify(campanhaRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve lancar excecao ao desativar campanha inexistente")
    void deveLancarExcecaoAoDesativarCampanhaInexistente() {
        when(campanhaRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> campanhaService.desativar(999L));

        assertEquals("CAMPANHA_NAO_ENCONTRADA", exception.getErrorCode());
        verify(campanhaRepository, never()).save(any());
        verify(campanhaRepository, never()).deleteById(anyLong());
    }
}
