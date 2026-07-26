package com.raizesdonordeste.backendapi.exception;

import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.MDC;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Testes de Tratamento de Excecoes")
class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@Mock
	private HttpServletRequest request;

	@Mock
	private BindingResult bindingResult;

	@Mock
	private HttpInputMessage httpInputMessage;

	private static final String URI = "/api/test";

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
		when(request.getRequestURI()).thenReturn(URI);
	}

	@AfterEach
	void limparMdc() {
		MDC.clear();
	}

	// ============================================
	// REGRA DE NEGOCIO (409 CONFLICT)
	// ============================================

	@Test
	@DisplayName("Deve retornar 503 quando gateway de pagamento estiver indisponivel")
	void deveRetornarServiceUnavailableParaGatewayPagamento() {
		GatewayPagamentoIndisponivelException ex = new GatewayPagamentoIndisponivelException();

		ResponseEntity<ErrorResponseDTO> response = handler.handleGatewayPagamentoIndisponivel(ex, request);

		assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals(GatewayPagamentoIndisponivelException.ERROR_CODE, response.getBody().error());
		assertEquals(GatewayPagamentoIndisponivelException.MESSAGE, response.getBody().message());
		assertEquals(URI, response.getBody().path());
	}

	@Test
	@DisplayName("Deve retornar 409 CONFLICT para RegraNegocioException")
	void deveRetornarConflictParaRegraNegocioException() {
		RegraNegocioException ex = new RegraNegocioException("PEDIDO_INVALIDO", "Pedido invalido");

		ResponseEntity<ErrorResponseDTO> response = handler.handleRegraNegocio(ex, request);

		assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("PEDIDO_INVALIDO", response.getBody().error());
		assertEquals("Pedido invalido", response.getBody().message());
		assertEquals(URI, response.getBody().path());
		assertNotNull(response.getBody().timestamp());
		assertTrue(response.getBody().details().isEmpty());
	}

	@Test
	@DisplayName("Deve incluir requestId na resposta de erro para rastreabilidade")
	void deveIncluirRequestIdNaRespostaDeErro() {
		MDC.put("requestId", "teste-request-001");
		RegraNegocioException ex = new RegraNegocioException("PEDIDO_INVALIDO", "Pedido invalido");

		ResponseEntity<ErrorResponseDTO> response = handler.handleRegraNegocio(ex, request);

		assertNotNull(response.getBody());
		assertEquals("teste-request-001", response.getBody().requestId());
	}

	// ============================================
	// VALIDACAO (422 UNPROCESSABLE_ENTITY)
	// ============================================

	@Test
	@DisplayName("Deve retornar 422 UNPROCESSABLE_ENTITY para erros de validacao")
	void deveRetornarUnprocessableEntityParaValidacao() {
		FieldError fieldError1 = new FieldError("obj", "email", "E-mail e obrigatorio");
		FieldError fieldError2 = new FieldError("obj", "senha", "Senha deve ter no minimo 6 caracteres");

		when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));
		MethodArgumentNotValidException ex = new MethodArgumentNotValidException(null, bindingResult);

		ResponseEntity<ErrorResponseDTO> response = handler.handleValidation(ex, request);

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("VALIDACAO", response.getBody().error());
		assertEquals("Erro de validacao nos campos enviados.", response.getBody().message());
		assertEquals(URI, response.getBody().path());
		assertEquals(2, response.getBody().details().size());

		List<ErrorResponseDTO.ErrorDetail> details = response.getBody().details();
		assertTrue(
				details.stream().anyMatch(d -> d.field().equals("email") && d.issue().equals("E-mail e obrigatorio")));
		assertTrue(details.stream()
				.anyMatch(d -> d.field().equals("senha") && d.issue().contains("Senha deve ter no minimo")));
	}

	// ============================================
	// CORPO INVALIDO (400 BAD_REQUEST)
	// ============================================

	@Test
	@DisplayName("Deve retornar 400 BAD_REQUEST para HttpMessageNotReadableException")
	void deveRetornarBadRequestParaMessageNotReadable() {
		HttpMessageNotReadableException ex = new HttpMessageNotReadableException("JSON malformado", null,
				httpInputMessage);

		ResponseEntity<ErrorResponseDTO> response = handler.handleMessageNotReadable(ex, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("CORPO_INVALIDO", response.getBody().error());
		assertEquals("O corpo da requisicao esta malformado ou contem tipos invalidos.", response.getBody().message());
		assertEquals(URI, response.getBody().path());
		assertTrue(response.getBody().details().isEmpty());
	}

	// ============================================
	// ACESSO NEGADO (403 FORBIDDEN)
	// ============================================

	@Test
	@DisplayName("Deve retornar 403 FORBIDDEN para AccessDeniedException")
	void deveRetornarForbiddenParaAccessDeniedException() {
		AccessDeniedException ex = new AccessDeniedException("Acesso negado");
		when(request.getUserPrincipal()).thenReturn(null);

		ResponseEntity<ErrorResponseDTO> response = handler.handleAccessDenied(ex, request);

		assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("ACESSO_NEGADO", response.getBody().error());
		assertEquals("Voce nao tem permissao para acessar este recurso.", response.getBody().message());
		assertEquals(URI, response.getBody().path());
	}

	// ============================================
	// NAO AUTENTICADO (401 UNAUTHORIZED)
	// ============================================

	@Test
	@DisplayName("Deve retornar 401 UNAUTHORIZED para AuthenticationException")
	void deveRetornarUnauthorizedParaAuthenticationException() {
		AuthenticationException ex = new BadCredentialsException("Credenciais invalidas");

		ResponseEntity<ErrorResponseDTO> response = handler.handleAuthentication(ex, request);

		assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("NAO_AUTENTICADO", response.getBody().error());
		assertEquals("Autenticacao necessaria para acessar este recurso.", response.getBody().message());
		assertEquals(URI, response.getBody().path());
	}

	// ============================================
	// PARAMETRO INVALIDO (400 BAD_REQUEST)
	// ============================================

	@Test
	@DisplayName("Deve retornar 400 BAD_REQUEST para IllegalArgumentException")
	void deveRetornarBadRequestParaIllegalArgumentException() {
		IllegalArgumentException ex = new IllegalArgumentException("Parametro 'id' nao pode ser negativo");

		ResponseEntity<ErrorResponseDTO> response = handler.handleIllegalArgument(ex, request);

		assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("PARAMETRO_INVALIDO", response.getBody().error());
		assertEquals("Parametro invalido na requisicao.", response.getBody().message());
		assertEquals(URI, response.getBody().path());
	}

	// ============================================
	// ERRO INTERNO (500 INTERNAL_SERVER_ERROR)
	// ============================================

	@Test
	@DisplayName("Deve retornar 500 INTERNAL_SERVER_ERROR para excecoes genericas")
	void deveRetornarInternalServerErrorParaExcecoesGenericas() {
		Exception ex = new RuntimeException("NullPointerException inesperado");

		ResponseEntity<ErrorResponseDTO> response = handler.handleGeneric(ex, request);

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("ERRO_INTERNO", response.getBody().error());
		assertEquals("Ocorreu um erro inesperado no servidor.", response.getBody().message());
		assertEquals(URI, response.getBody().path());
	}
	// ============================================
	// RECURSO NAO ENCONTRADO (404 NOT_FOUND)
	// ============================================

	@Test
	@DisplayName("Deve retornar 404 NOT_FOUND para RecursoNaoEncontradoException")
	void deveRetornarNotFoundParaRecursoNaoEncontradoException() {
		RecursoNaoEncontradoException ex = new RecursoNaoEncontradoException("PRODUTO_NAO_ENCONTRADO",
				"Produto ID 999 nao encontrado");

		ResponseEntity<ErrorResponseDTO> response = handler.handleRecursoNaoEncontrado(ex, request);

		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("PRODUTO_NAO_ENCONTRADO", response.getBody().error());
		assertEquals("Produto ID 999 nao encontrado", response.getBody().message());
		assertEquals(URI, response.getBody().path());
	}
}
