package ec.edu.ups.icc.proyecto.common.exception;

import org.springframework.http.HttpStatus;

/** Violacion de una regla de negocio: evento sin cupo, inscripcion duplicada, transicion de estado invalida, etc. */
public class BusinessRuleException extends ApiException {
    public BusinessRuleException(String message) {
        super(HttpStatus.UNPROCESSABLE_ENTITY, "BUSINESS_RULE_VIOLATION", message);
    }
}
