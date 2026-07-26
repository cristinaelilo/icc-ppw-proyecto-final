package ec.edu.ups.icc.proyecto.common.exception;

import org.springframework.http.HttpStatus;

/** El usuario autenticado no es propietario del recurso (ej: organizador que no es dueno del evento). */
public class ForbiddenOperationException extends ApiException {
    public ForbiddenOperationException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCESS_FORBIDDEN", message);
    }
}
