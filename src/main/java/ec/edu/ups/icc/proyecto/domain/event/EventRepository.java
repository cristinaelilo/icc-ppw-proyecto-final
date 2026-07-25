package ec.edu.ups.icc.proyecto.domain.event;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    /** Bloqueo pesimista sobre la fila del evento para decrementar available_capacity sin condiciones de carrera. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id and e.deleted = false")
    Optional<Event> lockByIdNotDeleted(@Param("id") Long id);

    Optional<Event> findByIdAndDeletedFalse(Long id);
}
