package ec.edu.ups.icc.proyecto.domain.event.repository;

import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id and e.deleted = false")
    Optional<Event> lockByIdNotDeleted(@Param("id") Long id);

    Optional<Event> findByIdAndDeletedFalse(Long id);
}
