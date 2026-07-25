package ec.edu.ups.icc.proyecto.domain.registration;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegistrationRepository extends JpaRepository<Registration, Long> {

    Optional<Registration> findByEvent_IdAndParticipant_Id(Long eventId, Long participantId);

    boolean existsByEvent_IdAndParticipant_IdAndStatusIn(Long eventId, Long participantId,
                                                          java.util.Collection<RegistrationStatus> statuses);

    Page<Registration> findByParticipant_Id(Long participantId, Pageable pageable);

    Page<Registration> findByEvent_Id(Long eventId, Pageable pageable);

    Page<Registration> findByEvent_IdAndStatus(Long eventId, RegistrationStatus status, Pageable pageable);
}
