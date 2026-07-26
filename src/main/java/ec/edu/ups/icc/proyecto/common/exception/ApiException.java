package ec.edu.ups.icc.proyecto.common.exception;

import org.springframework.http.HttpStatus;

/** Excepcion base para todos los errores de negocio manejados por la API. */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String internalCode;

    protected ApiException(HttpStatus status, String internalCode, String message) {
        super(message);
        this.status = status;
        this.internalCode = internalCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getInternalCode() {
        return internalCode;
    }
}
