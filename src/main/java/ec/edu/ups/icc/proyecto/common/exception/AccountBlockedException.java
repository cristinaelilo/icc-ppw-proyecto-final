package ec.edu.ups.icc.proyecto.common.exception;

import org.springframework.http.HttpStatus;

/** Cuenta bloqueada administrativamente (users.status = BLOCKED) o bloqueo temporal por intentos fallidos. */
public class AccountBlockedException extends ApiException {
    public AccountBlockedException(String message) {
        super(HttpStatus.FORBIDDEN, "ACCOUNT_BLOCKED", message);
    }
}
