package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.CupomRequestDTO;
import com.raizesdonordeste.backendapi.dto.CupomResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Cupom;
import com.raizesdonordeste.backendapi.model.TipoDesconto;
import com.raizesdonordeste.backendapi.model.Unidade;
import com.raizesdonordeste.backendapi.repository.CupomRepository;
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
@DisplayName("CupomService - Testes de Logica de Negocio")
class CupomServiceTest {

    @Mock private CupomRepository cupomRepository;
    @Mock private UnidadeRepository unidadeRepository;
    @Mock private AcessoUnidadeService acessoUnidadeService;

    @InjectMocks
    private CupomService cupomService;

    private Cupom cupom;
    private Unidade unidade;

    @BeforeEach
    void setUp() {
        unidade = new Unidade();
        unidade.setId(1L);
        unidade.setNome("Unidade Centro");

        cupom = new Cupom();
        cupom.setId(1L);
        cupom.setCodigo("NORDESTE10");
        cupom.setTipoDesconto(TipoDesconto.PERCENTUAL);
        cupom.setValor(new BigDecimal("10"));
        cupom.setValorMinimoPedido(BigDecimal.ZERO);
        cupom.setUnidade(unidade);
        cupom.setDataInicio(LocalDateTime.now().minusDays(1));
        cupom.setDataFim(LocalDateTime.now().plusDays(30));
        cupom.setUsoMaximo(100);
        cupom.setUsoAtual(0);
        cupom.setAtivo(true);
    }

    private CupomRequestDTO criarCupomRequestDTO(String codigo, BigDecimal valor) {
        CupomRequestDTO dto = new CupomRequestDTO();
        dto.setCodigo(codigo);
        dto.setTipoDesconto(TipoDesconto.PERCENTUAL);
        dto.setValor(valor);
        dto.setValorMinimoPedido(BigDecimal.ZERO);
        dto.setUnidadeId(1L);
        dto.setDataInicio(LocalDateTime.now().minusDays(1));
        dto.setDataFim(LocalDateTime.now().plusDays(30));
        dto.setUsoMaximo(100);
        dto.setAtivo(true);
        return dto;
    }

    private void configurarMocksCriacao() {
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(invocation -> {
            Cupom c = invocation.getArgument(0);
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
    @DisplayName("Deve criar cupom com sucesso")
    void deveCriarCupomComSucesso() {
        CupomRequestDTO dto = criarCupomRequestDTO("PROMO20", new BigDecimal("20"));

        when(cupomRepository.findByCodigo("PROMO20")).thenReturn(Optional.empty());
        configurarMocksCriacao();

        CupomResponseDTO response = cupomService.criar(dto);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("PROMO20", response.getCodigo());
        assertEquals(new BigDecimal("20"), response.getValor());
        verify(acessoUnidadeService).verificarAcessoGerencialUnidade(1L);
        verify(cupomRepository).save(any(Cupom.class));
    }

    @Test
    @DisplayName("Deve criar cupom global sem unidade")
    void deveCriarCupomGlobalSemUnidade() {
        CupomRequestDTO dto = criarCupomRequestDTO("GLOBAL10", new BigDecimal("10"));
        dto.setUnidadeId(null);

        when(cupomRepository.findByCodigo("GLOBAL10")).thenReturn(Optional.empty());
        when(cupomRepository.save(any(Cupom.class))).thenAnswer(invocation -> {
            Cupom c = invocation.getArgument(0);
            c.setId(10L);
            return c;
        });

        CupomResponseDTO response = cupomService.criar(dto);

        assertNotNull(response);
        assertNull(response.getUnidadeId());
        verify(acessoUnidadeService).verificarAcessoAdministrativoGlobal();
    }

    @Test
    @DisplayName("Não deve criar cupom em unidade inativa")
    void naoDeveCriarCupomEmUnidadeInativa() {
        CupomRequestDTO dto = criarCupomRequestDTO("INATIVO10", new BigDecimal("10"));
        unidade.setAtivo(false);
        when(cupomRepository.findByCodigo("INATIVO10")).thenReturn(Optional.empty());
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> cupomService.criar(dto));

        assertEquals("UNIDADE_INATIVA", exception.getErrorCode());
        verify(acessoUnidadeService).verificarAcessoGerencialUnidade(1L);
        verify(cupomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando codigo tem menos de 3 caracteres")
    void deveLancarExcecaoQuandoCodigoMuitoCurto() {
        CupomRequestDTO dto = criarCupomRequestDTO("AB", new BigDecimal("10"));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> cupomService.criar(dto));

        assertEquals("CODIGO_INVALIDO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando valor minimo e negativo")
    void deveLancarExcecaoQuandoValorMinimoNegativo() {
        CupomRequestDTO dto = criarCupomRequestDTO("PROMO10", new BigDecimal("10"));
        dto.setValorMinimoPedido(new BigDecimal("-50"));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> cupomService.criar(dto));

        assertEquals("VALOR_INVALIDO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao quando data de fim e igual a data de inicio")
    void deveLancarExcecaoQuandoDataFimIgualDataInicio() {
        CupomRequestDTO dto = criarCupomRequestDTO("PROMO10", new BigDecimal("10"));
        LocalDateTime data = LocalDateTime.of(2026, 7, 18, 12, 0);
        dto.setDataInicio(data);
        dto.setDataFim(data);

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> cupomService.criar(dto));

        assertEquals("DATA_INVALIDA", exception.getErrorCode());
        verify(cupomRepository, never()).findByCodigo(anyString());
        verify(cupomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando codigo ja existe")
    void deveLancarExcecaoQuandoCodigoJaExiste() {
        CupomRequestDTO dto = criarCupomRequestDTO("EXISTENTE", new BigDecimal("10"));

        when(cupomRepository.findByCodigo("EXISTENTE")).thenReturn(Optional.of(cupom));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> cupomService.criar(dto));

        assertEquals("CUPOM_JA_EXISTE", exception.getErrorCode());
        verify(cupomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando unidade nao existe")
    void deveLancarExcecaoQuandoUnidadeNaoExiste() {
        CupomRequestDTO dto = criarCupomRequestDTO("PROMO10", new BigDecimal("10"));
        dto.setUnidadeId(999L);

        when(cupomRepository.findByCodigo("PROMO10")).thenReturn(Optional.empty());
        when(unidadeRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> cupomService.criar(dto));

        assertEquals("UNIDADE_NAO_ENCONTRADA", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE LISTAGEM
    // ============================================

    @Test
    @DisplayName("Deve listar todos os cupons com sucesso")
    void deveListarTodosCuponsComSucesso() {
        Pageable pageable = PageRequest.of(0, 10);
        when(cupomRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(cupom), pageable, 1));
        when(acessoUnidadeService.getUnidadesGerenciaisDoUsuario()).thenReturn(null);

        Page<CupomResponseDTO> response = cupomService.listarTodos(pageable);

        assertNotNull(response);
        assertEquals(1, response.getTotalElements());
        assertEquals("NORDESTE10", response.getContent().get(0).getCodigo());
    }

    @Test
    @DisplayName("Gerente deve listar apenas cupons das unidades vinculadas")
    void gerenteDeveListarApenasCuponsDasUnidadesVinculadas() {
        Pageable pageable = PageRequest.of(0, 10);
        when(acessoUnidadeService.getUnidadesGerenciaisDoUsuario()).thenReturn(List.of(1L));
        when(cupomRepository.findByUnidadeIdIn(List.of(1L), pageable))
                .thenReturn(new PageImpl<>(List.of(cupom), pageable, 1));

        Page<CupomResponseDTO> resultado = cupomService.listarTodos(pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(1L, resultado.getContent().get(0).getUnidadeId());
        verify(cupomRepository, never()).findAll(pageable);
    }

    // ============================================
    // TESTES DE BUSCA POR ID
    // ============================================

    @Test
    @DisplayName("Deve buscar cupom por ID com sucesso")
    void deveBuscarCupomPorIdComSucesso() {
        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));

        CupomResponseDTO response = cupomService.buscarPorId(1L);

        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("NORDESTE10", response.getCodigo());
    }

    @Test
    @DisplayName("Deve lancar excecao quando cupom nao existe")
    void deveLancarExcecaoQuandoCupomNaoExiste() {
        when(cupomRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> cupomService.buscarPorId(999L));

        assertEquals("CUPOM_NAO_ENCONTRADO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE ATUALIZACAO
    // ============================================

    @Test
    @DisplayName("Deve atualizar cupom com sucesso")
    void deveAtualizarCupomComSucesso() {
        CupomRequestDTO dto = criarCupomRequestDTO("NORDESTE15", new BigDecimal("15"));

        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
        when(cupomRepository.save(cupom)).thenReturn(cupom);

        CupomResponseDTO response = cupomService.atualizar(1L, dto);

        assertEquals("NORDESTE15", response.getCodigo());
        assertEquals(new BigDecimal("15"), response.getValor());
        verify(cupomRepository).save(cupom);
    }

    @Test
    @DisplayName("Deve lancar excecao quando cupom nao existe na atualizacao")
    void deveLancarExcecaoQuandoCupomNaoExisteNaAtualizacao() {
        CupomRequestDTO dto = criarCupomRequestDTO("PROMO10", new BigDecimal("10"));

        when(cupomRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> cupomService.atualizar(999L, dto));

        assertEquals("CUPOM_NAO_ENCONTRADO", exception.getErrorCode());
    }

    @Test
    @DisplayName("Deve lancar excecao ao tentar alterar codigo de cupom ja utilizado")
    void deveLancarExcecaoAoAlterarCodigoDeCupomUtilizado() {
        cupom.setUsoAtual(5);
        CupomRequestDTO dto = criarCupomRequestDTO("NOVOCODIGO", new BigDecimal("10"));

        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> cupomService.atualizar(1L, dto));

        assertEquals("CUPOM_EM_USO", exception.getErrorCode());
        verify(cupomRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao quando uso maximo e menor que uso atual")
    void deveLancarExcecaoQuandoUsoMaximoMenorQueUsoAtual() {
        cupom.setUsoAtual(50);
        CupomRequestDTO dto = criarCupomRequestDTO("NORDESTE10", new BigDecimal("10"));
        dto.setUsoMaximo(30);

        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));
        when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

        RegraNegocioException exception = assertThrows(RegraNegocioException.class,
                () -> cupomService.atualizar(1L, dto));

        assertEquals("VALOR_INVALIDO", exception.getErrorCode());
    }

    // ============================================
    // TESTES DE DESATIVACAO
    // ============================================

    @Test
    @DisplayName("Deve desativar cupom sem excluir o registro")
    void deveDesativarCupomSemExcluirRegistro() {
        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));

        assertDoesNotThrow(() -> cupomService.desativar(1L));

        assertFalse(cupom.getAtivo());
        verify(cupomRepository).save(cupom);
        verify(cupomRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve lancar excecao quando cupom nao existe na desativacao")
    void deveLancarExcecaoQuandoCupomNaoExisteNaDesativacao() {
        when(cupomRepository.findById(999L)).thenReturn(Optional.empty());

        RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
                () -> cupomService.desativar(999L));

        assertEquals("CUPOM_NAO_ENCONTRADO", exception.getErrorCode());
        verify(cupomRepository, never()).save(any());
        verify(cupomRepository, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("Deve desativar cupom utilizado preservando seus vinculos historicos")
    void deveDesativarCupomUtilizadoPreservandoVinculosHistoricos() {
        cupom.setUsoAtual(5);

        when(cupomRepository.findById(1L)).thenReturn(Optional.of(cupom));

        assertDoesNotThrow(() -> cupomService.desativar(1L));

        assertFalse(cupom.getAtivo());
        assertEquals(5, cupom.getUsoAtual());
        verify(cupomRepository).save(cupom);
        verify(cupomRepository, never()).deleteById(anyLong());
    }
}
