package ec.edu.ups.icc.proyecto.domain.session.repository;

import ec.edu.ups.icc.proyecto.domain.session.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByEvent_IdOrderByStartAtAsc(Long eventId);

    boolean existsByEvent_IdAndTitleAndStartAt(Long eventId, String title, OffsetDateTime startAt);
}

