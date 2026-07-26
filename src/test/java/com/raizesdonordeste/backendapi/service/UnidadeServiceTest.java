package com.raizesdonordeste.backendapi.service;

import com.raizesdonordeste.backendapi.dto.UnidadeDTO;
import com.raizesdonordeste.backendapi.dto.UnidadeResponseDTO;
import com.raizesdonordeste.backendapi.exception.RecursoNaoEncontradoException;
import com.raizesdonordeste.backendapi.exception.RegraNegocioException;
import com.raizesdonordeste.backendapi.model.Unidade;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UnidadeService - Testes de Logica de Negocio")
class UnidadeServiceTest {

	@Mock
	private UnidadeRepository unidadeRepository;

	@InjectMocks
	private UnidadeService unidadeService;

	private Unidade unidade;
	private UnidadeDTO dto;

	@BeforeEach
	void setUp() {
		unidade = new Unidade();
		unidade.setId(1L);
		unidade.setNome("Unidade Centro");
		unidade.setEndereco("Rua A, 123");
		unidade.setCidade("Sao Paulo");
		unidade.setEstado("SP");
		unidade.setCep("01000-000");
		unidade.setTelefone("1133334444");
		unidade.setAtivo(true);

		dto = new UnidadeDTO();
		dto.setNome("Unidade Centro");
		dto.setEndereco("Rua A, 123");
		dto.setCidade("Sao Paulo");
		dto.setEstado("SP");
		dto.setCep("01000-000");
		dto.setTelefone("1133334444");
	}

	// ============================================
	// TESTES DE CRIACAO
	// ============================================

	@Test
	@DisplayName("Deve criar unidade com sucesso")
	void deveCriarUnidadeComSucesso() {
		when(unidadeRepository.findByNomeAndAtivoTrue("Unidade Centro")).thenReturn(Optional.empty());
		when(unidadeRepository.save(any(Unidade.class))).thenAnswer(invocation -> {
			Unidade u = invocation.getArgument(0);
			u.setId(1L);
			return u;
		});

		UnidadeResponseDTO response = unidadeService.criar(dto);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Unidade Centro", response.getNome());
		assertEquals("SP", response.getEstado());
		verify(unidadeRepository).save(any(Unidade.class));
	}

	@Test
	@DisplayName("Deve lancar excecao quando nome da unidade ja existe (ativa)")
	void deveLancarExcecaoQuandoNomeJaExiste() {
		Unidade unidadeExistente = new Unidade();
		unidadeExistente.setId(2L);
		unidadeExistente.setNome("Unidade Centro");
		unidadeExistente.setAtivo(true);

		when(unidadeRepository.findByNomeAndAtivoTrue("Unidade Centro")).thenReturn(Optional.of(unidadeExistente));

		RegraNegocioException exception = assertThrows(RegraNegocioException.class, () -> unidadeService.criar(dto));

		assertEquals("UNIDADE_JA_EXISTE", exception.getErrorCode());
		verify(unidadeRepository, never()).save(any());
	}

	@Test
	@DisplayName("Deve permitir criar unidade com nome de unidade inativa")
	void devePermitirCriarComNomeDeUnidadeInativa() {
		// findByNomeAndAtivoTrue nao retorna a inativa
		when(unidadeRepository.findByNomeAndAtivoTrue("Unidade Centro")).thenReturn(Optional.empty());
		when(unidadeRepository.save(any(Unidade.class))).thenAnswer(invocation -> {
			Unidade u = invocation.getArgument(0);
			u.setId(3L);
			return u;
		});

		UnidadeResponseDTO response = unidadeService.criar(dto);

		assertNotNull(response);
		assertEquals(3L, response.getId());
	}

	// ============================================
	// TESTES DE LISTAGEM
	// ============================================

	@Test
	@DisplayName("Deve listar apenas unidades ativas")
	void deveListarApenasUnidadesAtivas() {
		Pageable pageable = PageRequest.of(0, 10);
		when(unidadeRepository.findByAtivoTrue(pageable))
				.thenReturn(new PageImpl<>(List.of(unidade), pageable, 1));

		Page<UnidadeResponseDTO> response = unidadeService.listarTodas(pageable);

		assertEquals(1, response.getTotalElements());
		assertEquals("Unidade Centro", response.getContent().get(0).getNome());
		verify(unidadeRepository).findByAtivoTrue(pageable);
		verify(unidadeRepository, never()).findAll();
	}

	// ============================================
	// TESTES DE BUSCA POR ID
	// ============================================

	@Test
	@DisplayName("Deve buscar unidade ativa por ID com sucesso")
	void deveBuscarUnidadeAtivaPorIdComSucesso() {
		when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

		UnidadeResponseDTO response = unidadeService.buscarPorId(1L);

		assertNotNull(response);
		assertEquals(1L, response.getId());
		assertEquals("Unidade Centro", response.getNome());
	}

	@Test
	@DisplayName("Deve lancar excecao quando unidade nao existe")
	void deveLancarExcecaoQuandoUnidadeNaoExiste() {
		when(unidadeRepository.findById(999L)).thenReturn(Optional.empty());

		RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
				() -> unidadeService.buscarPorId(999L));

		assertEquals("UNIDADE_NAO_ENCONTRADA", exception.getErrorCode());
	}

	@Test
	@DisplayName("Deve lancar excecao quando unidade esta inativa")
	void deveLancarExcecaoQuandoUnidadeEstaInativa() {
		unidade.setAtivo(false);
		when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

		RegraNegocioException exception = assertThrows(RegraNegocioException.class,
				() -> unidadeService.buscarPorId(1L));

		assertEquals("UNIDADE_INATIVA", exception.getErrorCode());
	}

	// ============================================
	// TESTES DE ATUALIZACAO
	// ============================================

	@Test
	@DisplayName("Deve atualizar unidade ativa com sucesso")
	void deveAtualizarUnidadeAtivaComSucesso() {
		dto.setNome("Unidade Atualizada");
		when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
		when(unidadeRepository.findByNomeAndAtivoTrue("Unidade Atualizada")).thenReturn(Optional.empty());
		when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

		UnidadeResponseDTO response = unidadeService.atualizar(1L, dto);

		assertNotNull(response);
		verify(unidadeRepository).save(any(Unidade.class));
	}

	@Test
	@DisplayName("Deve lancar excecao ao tentar atualizar unidade inativa")
	void deveLancarExcecaoAoAtualizarUnidadeInativa() {
		unidade.setAtivo(false);
		when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

		RegraNegocioException exception = assertThrows(RegraNegocioException.class,
				() -> unidadeService.atualizar(1L, dto));

		assertEquals("UNIDADE_INATIVA", exception.getErrorCode());
		verify(unidadeRepository, never()).save(any());
	}

	// ============================================
	// TESTES DE DESATIVACAO (SOFT-DELETE)
	// ============================================

	@Test
	@DisplayName("Deve desativar unidade com sucesso")
	void deveDesativarUnidadeComSucesso() {
		when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));
		when(unidadeRepository.save(any(Unidade.class))).thenReturn(unidade);

		unidadeService.desativar(1L);

		assertFalse(unidade.getAtivo());
		verify(unidadeRepository).save(any(Unidade.class));
		verify(unidadeRepository, never()).deleteById(anyLong());
	}

	@Test
	@DisplayName("Deve lancar excecao ao desativar unidade inexistente")
	void deveLancarExcecaoAoDesativarUnidadeInexistente() {
		when(unidadeRepository.findById(999L)).thenReturn(Optional.empty());

		RecursoNaoEncontradoException exception = assertThrows(RecursoNaoEncontradoException.class,
				() -> unidadeService.desativar(999L));

		assertEquals("UNIDADE_NAO_ENCONTRADA", exception.getErrorCode());
		verify(unidadeRepository, never()).save(any());
	}

	@Test
	@DisplayName("Deve ignorar desativacao quando unidade ja esta inativa")
	void deveIgnorarDesativacaoQuandoUnidadeJaInativa() {
		unidade.setAtivo(false);
		when(unidadeRepository.findById(1L)).thenReturn(Optional.of(unidade));

		unidadeService.desativar(1L);

		assertFalse(unidade.getAtivo());
		verify(unidadeRepository, never()).save(any());
	}
}
