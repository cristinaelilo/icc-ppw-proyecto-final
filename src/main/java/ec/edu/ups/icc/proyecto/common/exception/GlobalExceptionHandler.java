package ec.edu.ups.icc.proyecto.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Manejo centralizado de excepciones. Toda respuesta de error usa el mismo
 * formato (fecha, codigo HTTP, codigo interno, mensaje, ruta), tal como pide
 * la practica.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ---- Errores de negocio propios (404, 409, 422, 403, 401, 429) ----
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApiException(ApiException ex, HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (ex instanceof TooManyRequestsException tmr) {
            headers.add("Retry-After", String.valueOf(tmr.getRetryAfterSeconds()));
        }
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                ex.getStatus().value(),
                ex.getInternalCode(),
                ex.getMessage(),
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(ex.getStatus()).headers(headers).body(body);
    }

    // ---- Errores de validacion de Bean Validation (@Valid) por campo ----
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorResponse.FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                "Uno o mas campos son invalidos",
                request.getRequestURI(),
                fieldErrors
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ---- Acceso denegado por @PreAuthorize ----
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "ACCESS_FORBIDDEN",
                "No tiene permisos para realizar esta operacion",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }

    // ---- Credenciales invalidas: mensaje generico, no revela si el correo existe ----
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                "Correo o contrasena invalidos",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }

    // ---- Cualquier otro error no controlado ----
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        ErrorResponse body = new ErrorResponse(
                OffsetDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "INTERNAL_ERROR",
                "Ocurrio un error inesperado",
                request.getRequestURI(),
                null
        );
        return ResponseEntity.internalServerError().body(body);
    }
}
