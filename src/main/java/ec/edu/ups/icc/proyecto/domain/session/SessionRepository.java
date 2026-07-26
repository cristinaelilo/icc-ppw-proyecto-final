package ec.edu.ups.icc.proyecto.domain.session;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SessionRepository extends JpaRepository<Session, Long> {
    List<Session> findByEvent_IdOrderByStartAtAsc(Long eventId);
}
