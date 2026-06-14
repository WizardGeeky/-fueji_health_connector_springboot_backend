package com.healthconnector.app.exception;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.healthconnector.app.constants.ErrorCodes;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

	// ── Bean Validation ───────────────────────────────────────────────
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex,
			HttpServletRequest request) {
		List<Map<String, String>> errors = new ArrayList<>();
		ex.getBindingResult().getAllErrors().forEach(error -> {
			String field = error instanceof FieldError fe ? fe.getField() : error.getObjectName();
			errors.add(Map.of("field", field, "message", error.getDefaultMessage()));
		});
		return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, "Validation failed",
				request.getRequestURI(), errors);
	}

	// ── Custom Exceptions ─────────────────────────────────────────────
	@ExceptionHandler(ResourceNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleNotFound(ResourceNotFoundException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND, ex.getMessage(), req.getRequestURI(),
				null);
	}

	@ExceptionHandler(DuplicateResourceException.class)
	public ResponseEntity<ApiErrorResponse> handleDuplicate(DuplicateResourceException ex, HttpServletRequest req) {
		List<Map<String, String>> errors = ex.getField() != null
				? List.of(Map.of("field", ex.getField(), "message", ex.getMessage()))
				: null;
		return buildResponse(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_RESOURCE, ex.getMessage(), req.getRequestURI(),
				errors);
	}

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.UNPROCESSABLE_ENTITY, ex.getErrorCode(), ex.getMessage(), req.getRequestURI(),
				null);
	}

	@ExceptionHandler(ValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleCustomValidation(ValidationException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, ex.getMessage(), req.getRequestURI(),
				ex.getErrors().isEmpty() ? null : ex.getErrors());
	}

	@ExceptionHandler(UnauthorizedException.class)
	public ResponseEntity<ApiErrorResponse> handleUnauthorized(UnauthorizedException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, ex.getMessage(), req.getRequestURI(),
				null);
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiErrorResponse> handleForbidden(ForbiddenException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN, ex.getMessage(), req.getRequestURI(), null);
	}

	@ExceptionHandler(AESException.class)
	public ResponseEntity<ApiErrorResponse> handleAES(AESException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.AES_ENCRYPTION_ERROR,
				"Data encryption/decryption error", req.getRequestURI(), null);
	}

	@ExceptionHandler(GeminiIntegrationException.class)
	public ResponseEntity<ApiErrorResponse> handleGemini(GeminiIntegrationException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.SERVICE_UNAVAILABLE, ErrorCodes.GEMINI_INTEGRATION_ERROR, ex.getMessage(),
				req.getRequestURI(), null);
	}

	@ExceptionHandler(FHIRValidationException.class)
	public ResponseEntity<ApiErrorResponse> handleFHIR(FHIRValidationException ex, HttpServletRequest req) {
		List<Map<String, String>> errors = ex.getValidationErrors().stream()
				.map(e -> Map.of("field", "fhir", "message", e)).toList();
		return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.FHIR_VALIDATION_ERROR, ex.getMessage(),
				req.getRequestURI(), errors.isEmpty() ? null : errors);
	}

	// ── Spring Security ───────────────────────────────────────────────
	@ExceptionHandler(BadCredentialsException.class)
	public ResponseEntity<ApiErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCodes.INVALID_CREDENTIALS, "Invalid email or password",
				req.getRequestURI(), null);
	}

	@ExceptionHandler(LockedException.class)
	public ResponseEntity<ApiErrorResponse> handleLocked(LockedException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_LOCKED,
				"Account is locked. Please contact administrator", req.getRequestURI(), null);
	}

	@ExceptionHandler(DisabledException.class)
	public ResponseEntity<ApiErrorResponse> handleDisabled(DisabledException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.FORBIDDEN, ErrorCodes.ACCOUNT_BLOCKED,
				"Account has been disabled. Please contact administrator", req.getRequestURI(), null);
	}

	@ExceptionHandler(AccessDeniedException.class)
	public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.FORBIDDEN, ErrorCodes.FORBIDDEN,
				"You do not have permission to access this resource", req.getRequestURI(), null);
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<ApiErrorResponse> handleAuth(AuthenticationException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHORIZED, ex.getMessage(), req.getRequestURI(),
				null);
	}

	// ── MongoDB Exceptions ────────────────────────────────────────────
	@ExceptionHandler(DuplicateKeyException.class)
	public ResponseEntity<ApiErrorResponse> handleMongoConflict(DuplicateKeyException ex, HttpServletRequest req) {
		return buildResponse(HttpStatus.CONFLICT, ErrorCodes.DUPLICATE_RESOURCE,
				"A record with the given data already exists", req.getRequestURI(), null);
	}

	// ── HTTP Protocol Exceptions ──────────────────────────────────────
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex,
			HttpServletRequest req) {
		return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, ErrorCodes.METHOD_NOT_ALLOWED, ex.getMessage(),
				req.getRequestURI(), null);
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ApiErrorResponse> handleMediaType(HttpMediaTypeNotSupportedException ex,
			HttpServletRequest req) {
		return buildResponse(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ErrorCodes.MEDIA_TYPE_NOT_SUPPORTED, ex.getMessage(),
				req.getRequestURI(), null);
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiErrorResponse> handleNotReadable(HttpMessageNotReadableException ex,
			HttpServletRequest req) {
		return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR,
				"Request body is missing or malformed", req.getRequestURI(), null);
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
			HttpServletRequest req) {
		String message = String.format("Parameter '%s' should be of type '%s'", ex.getName(),
				ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
		return buildResponse(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, message, req.getRequestURI(), null);
	}

	// ── Fallback ──────────────────────────────────────────────────────
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleAll(Exception ex, HttpServletRequest req) {
		log.error("Unhandled exception on {}: {}", req.getRequestURI(), ex.getMessage(), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred. Please try again later.", req.getRequestURI(), null);
	}

	@ExceptionHandler(Throwable.class)
	public ResponseEntity<ApiErrorResponse> handleThrowable(Throwable ex, HttpServletRequest req) {
		log.error("Unhandled throwable on {}: {}", req.getRequestURI(), ex.getMessage(), ex);
		return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCodes.INTERNAL_SERVER_ERROR,
				"An unexpected error occurred. Please try again later.", req.getRequestURI(), null);
	}

	// ── Helper ────────────────────────────────────────────────────────
	private ResponseEntity<ApiErrorResponse> buildResponse(HttpStatus status, String errorCode, String message,
			String path, List<Map<String, String>> errors) {
		ApiErrorResponse body = ApiErrorResponse.builder().status(status.value()).errorCode(errorCode).message(message)
				.path(path).errors(errors).build();
		return ResponseEntity.status(status).body(body);
	}
}
