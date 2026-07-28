package ec.edu.ups.icc.proyecto.domain.event.services;

import ec.edu.ups.icc.proyecto.domain.event.dto.EventRequest;
import ec.edu.ups.icc.proyecto.domain.event.dto.EventResponse;
import ec.edu.ups.icc.proyecto.domain.event.model.EventModality;
import ec.edu.ups.icc.proyecto.domain.event.model.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;

public interface EventService {

    /** Catalogo publico: solo eventos PUBLISHED y no eliminados (salvo que quien consulte sea ADMIN). */
    Page<EventResponse> searchPublic(String search, Long categoryId, EventModality modality,
                                      OffsetDateTime from, OffsetDateTime to, EventStatus status,
                                      Pageable pageable);

    /** Eventos propios del organizador autenticado, en cualquier estado. */
    Page<EventResponse> findMine(Pageable pageable);

    EventResponse findById(Long id);

    EventResponse create(EventRequest request);

    EventResponse update(Long id, EventRequest request);

    EventResponse publish(Long id);

    EventResponse cancel(Long id);

    EventResponse finish(Long id);

    void delete(Long id);
}
