package com.raizesdonordeste.backendapi.exception;

import com.raizesdonordeste.backendapi.dto.ErrorResponseDTO;
import com.raizesdonordeste.backendapi.util.LogPseudonymizer;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final String CODIGO_VALIDACAO = "VALIDACAO";
	private static final String CODIGO_CORPO_INVALIDO = "CORPO_INVALIDO";
	private static final String CODIGO_NAO_ENCONTRADO = "NAO_ENCONTRADO";
	private static final String CODIGO_ACESSO_NEGADO = "ACESSO_NEGADO";
	private static final String CODIGO_NAO_AUTENTICADO = "NAO_AUTENTICADO";
	private static final String CODIGO_PARAMETRO_INVALIDO = "PARAMETRO_INVALIDO";
	private static final String CODIGO_ERRO_INTERNO = "ERRO_INTERNO";

	private static final String MSG_VALIDACAO = "Erro de validacao nos campos enviados.";
	private static final String MSG_CORPO_INVALIDO = "O corpo da requisicao esta malformado ou contem tipos invalidos.";
	private static final String MSG_ACESSO_NEGADO = "Voce nao tem permissao para acessar este recurso.";
	private static final String MSG_NAO_AUTENTICADO = "Autenticacao necessaria para acessar este recurso.";
	private static final String MSG_ERRO_INTERNO = "Ocorreu um erro inesperado no servidor.";
	private static final String MSG_PARAMETRO_INVALIDO = "Parametro invalido na requisicao.";

	@ExceptionHandler(GatewayPagamentoIndisponivelException.class)
	public ResponseEntity<ErrorResponseDTO> handleGatewayPagamentoIndisponivel(
			GatewayPagamentoIndisponivelException ex, HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.warn("Gateway de pagamento indisponivel | URI: {}", request.getRequestURI());
		ErrorResponseDTO error = criarErrorResponse(GatewayPagamentoIndisponivelException.ERROR_CODE,
				ex.getMessage(), new ArrayList<>(), request);
		return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
	}

	// ============================================
	// RECURSO NAO ENCONTRADO (404 NOT_FOUND)
	// ============================================

	@ExceptionHandler(RecursoNaoEncontradoException.class)
	public ResponseEntity<ErrorResponseDTO> handleRecursoNaoEncontrado(RecursoNaoEncontradoException ex,
			HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.info("Recurso nao encontrado | Codigo: {} | URI: {} | Mensagem: {}", ex.getErrorCode(),
				request.getRequestURI(), ex.getMessage());

		ErrorResponseDTO error = criarErrorResponse(ex.getErrorCode(), ex.getMessage(), new ArrayList<>(), request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(RegraNegocioException.class)
	public ResponseEntity<ErrorResponseDTO> handleRegraNegocio(RegraNegocioException ex, HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		HttpStatus status = isErroNaoEncontrado(ex.getErrorCode()) ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;

		if (status == HttpStatus.NOT_FOUND) {
			log.info("Recurso nao encontrado (detectado por sufixo) | Codigo: {} | URI: {} | Mensagem: {}",
					ex.getErrorCode(), request.getRequestURI(), ex.getMessage());
		} else {
			log.warn("Regra de negocio violada | Codigo: {} | URI: {} | Mensagem: {}", ex.getErrorCode(),
					request.getRequestURI(), ex.getMessage());
		}

		ErrorResponseDTO error = criarErrorResponse(ex.getErrorCode(), ex.getMessage(), new ArrayList<>(), request);
		return ResponseEntity.status(status).body(error);
	}

	private boolean isErroNaoEncontrado(String errorCode) {
		if (errorCode == null) {
			return false;
		}
		String upperCode = errorCode.toUpperCase();
		return upperCode.endsWith("_NAO_ENCONTRADO") || upperCode.endsWith("_NAO_ENCONTRADA")
				|| upperCode.endsWith("_INEXISTENTE");
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		List<ErrorResponseDTO.ErrorDetail> details = new ArrayList<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String field = ((FieldError) error).getField();
			String issue = error.getDefaultMessage();
			details.add(new ErrorResponseDTO.ErrorDetail(field, issue));
		});

		log.info("Erro de validacao | URI: {} | Campos: {}", request.getRequestURI(), details.size());

		ErrorResponseDTO error = criarErrorResponse(CODIGO_VALIDACAO, MSG_VALIDACAO, details, request);
		return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(error);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponseDTO> handleMessageNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.warn("Corpo da requisicao invalido | URI: {} | Motivo: {}", request.getRequestURI(), ex.getMessage());

		ErrorResponseDTO error = criarErrorResponse(CODIGO_CORPO_INVALIDO, MSG_CORPO_INVALIDO, new ArrayList<>(),
				request);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(NoSuchElementException.class)
	public ResponseEntity<ErrorResponseDTO> handleNotFound(NoSuchElementException ex, HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.info("Recurso nao encontrado | URI: {} | Mensagem: {}", request.getRequestURI(), ex.getMessage());

		ErrorResponseDTO error = criarErrorResponse(CODIGO_NAO_ENCONTRADO, ex.getMessage(), new ArrayList<>(), request);
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.warn("Acesso negado | URI: {} | Ator: {}", request.getRequestURI(),
				LogPseudonymizer.id(request.getUserPrincipal() != null
						? request.getUserPrincipal().getName() : null));

		ErrorResponseDTO error = criarErrorResponse(CODIGO_ACESSO_NEGADO, MSG_ACESSO_NEGADO, new ArrayList<>(),
				request);
		return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ErrorResponseDTO> handleAuthentication(AuthenticationException ex,
			HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.warn("Falha de autenticacao | URI: {} | Motivo: {}", request.getRequestURI(), ex.getMessage());

		ErrorResponseDTO error = criarErrorResponse(CODIGO_NAO_AUTENTICADO, MSG_NAO_AUTENTICADO, new ArrayList<>(),
				request);
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ErrorResponseDTO> handleIllegalArgument(IllegalArgumentException ex,
			HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.warn("Parametro invalido | URI: {} | Mensagem: {}", request.getRequestURI(), ex.getMessage());

		ErrorResponseDTO error = criarErrorResponse(CODIGO_PARAMETRO_INVALIDO, MSG_PARAMETRO_INVALIDO,
				new ArrayList<>(), request);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex, HttpServletRequest request) {
		Objects.requireNonNull(request, "Request nao pode ser nulo");
		validarRequest(request);

		log.error("Erro inesperado | URI: {} | Tipo: {} | Mensagem: {}", request.getRequestURI(),
				ex.getClass().getSimpleName(), ex.getMessage(), ex);

		ErrorResponseDTO error = criarErrorResponse(CODIGO_ERRO_INTERNO, MSG_ERRO_INTERNO, new ArrayList<>(), request);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
	}

	private void validarRequest(HttpServletRequest request) {
		if (request.getRequestURI() == null) {
			throw new IllegalArgumentException("URI da requisicao nao pode ser nula");
		}
	}

	private ErrorResponseDTO criarErrorResponse(String codigo, String mensagem,
			List<ErrorResponseDTO.ErrorDetail> details, HttpServletRequest request) {
		return new ErrorResponseDTO(codigo, mensagem, details, Instant.now(), request.getRequestURI());
	}
}
