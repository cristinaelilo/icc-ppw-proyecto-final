package ec.edu.ups.icc.proyecto.domain.registration.repository;

import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import ec.edu.ups.icc.proyecto.domain.registration.model.Registration;
import ec.edu.ups.icc.proyecto.domain.registration.model.RegistrationStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByEvent_IdAndParticipant_Id(Long eventId, Long participantId);

    boolean existsByEvent_IdAndParticipant_IdAndStatusIn(Long eventId, Long participantId,
                                                          java.util.Collection<RegistrationStatus> statuses);

    Page<Registration> findByParticipant_Id(Long participantId, Pageable pageable);

    Page<Registration> findByEvent_Id(Long eventId, Pageable pageable);

    Page<Registration> findByEvent_IdAndStatus(Long eventId, RegistrationStatus status, Pageable pageable);

    /** Bloqueo pesimista sobre el evento para decrementar available_capacity sin condiciones de carrera. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :eventId and e.deleted = false")
    Optional<Event> lockEventById(@Param("eventId") Long eventId);
}
