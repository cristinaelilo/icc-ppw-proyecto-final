package ec.edu.ups.icc.proyecto.domain.registration.services;

import ec.edu.ups.icc.proyecto.domain.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.proyecto.domain.registration.model.RegistrationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RegistrationService {

    /** El participante autenticado solicita inscribirse (queda en PENDING). */
    RegistrationResponse register(Long eventId);

    /** El organizador dueno del evento (o ADMIN) aprueba una solicitud PENDING; consume un cupo. */
    RegistrationResponse confirm(Long registrationId);

    /** El organizador dueno del evento (o ADMIN) rechaza una solicitud PENDING; no consume cupo. */
    RegistrationResponse reject(Long registrationId);

    /** El participante dueno (o ADMIN) cancela su inscripcion; si estaba CONFIRMED, libera el cupo. */
    void cancel(Long registrationId);

    Page<RegistrationResponse> findMine(Pageable pageable);

    Page<RegistrationResponse> findByEvent(Long eventId, RegistrationStatus status, Pageable pageable);
}
