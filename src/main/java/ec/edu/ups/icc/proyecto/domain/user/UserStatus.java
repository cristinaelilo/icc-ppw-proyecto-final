package ec.edu.ups.icc.proyecto.domain.user;

/** Estado administrativo permanente de la cuenta (distinto del bloqueo temporal de login en Redis). */
public enum UserStatus {
    ACTIVE, BLOCKED
}
