package ec.edu.ups.icc.proyecto.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.OffsetDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (ex instanceof TooManyRequestsException tmr) {
            headers.add("Retry-After", String.valueOf(tmr.getRetryAfterSeconds()));
        }
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(), ex.getStatus().value(), ex.getInternalCode(),
                ex.getMessage(), request.getRequestURI(), null
        );
        return ResponseEntity.status(ex.getStatus()).headers(headers).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.BAD_REQUEST.value(), "VALIDATION_ERROR",
                "Uno o mas campos son invalidos", request.getRequestURI(), fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.FORBIDDEN.value(), "ACCESS_FORBIDDEN",
                "No tiene permisos para realizar esta operacion", request.getRequestURI(), null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.UNAUTHORIZED.value(), "INVALID_CREDENTIALS",
                "Correo o contrasena invalidos", request.getRequestURI(), null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    /**
     * Se lanza cuando se pide una ruta que no existe en absoluto (ej: "/" o
     * "/favicon.ico", que Spring intenta resolver como recurso estatico).
     * Sin este manejador especifico, caeria en handleGeneric() y devolveria
     * un 500 enganoso para algo que en realidad es un simple 404.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.NOT_FOUND.value(), "RESOURCE_NOT_FOUND",
                "El recurso solicitado no existe", request.getRequestURI(), null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Error no controlado en {} {}", request.getMethod(), request.getRequestURI(), ex);

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(), HttpStatus.INTERNAL_SERVER_ERROR.value(), "INTERNAL_ERROR",
                "Ocurrio un error inesperado", request.getRequestURI(), null
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
