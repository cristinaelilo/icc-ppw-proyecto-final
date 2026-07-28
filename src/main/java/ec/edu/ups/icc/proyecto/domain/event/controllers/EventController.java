package ec.edu.ups.icc.proyecto.domain.event.controllers;

import ec.edu.ups.icc.proyecto.domain.event.dto.EventRequest;
import ec.edu.ups.icc.proyecto.domain.event.dto.EventResponse;
import ec.edu.ups.icc.proyecto.domain.event.model.EventModality;
import ec.edu.ups.icc.proyecto.domain.event.model.EventStatus;
import ec.edu.ups.icc.proyecto.domain.event.services.EventService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    /** Catalogo: publico ve solo PUBLISHED; un ADMIN puede filtrar por cualquier status. */
    @GetMapping
    public Page<EventResponse> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) EventModality modality,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
            Pageable pageable) {
        return eventService.searchPublic(search, categoryId, modality, from, to, status, pageable);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public Page<EventResponse> findMine(Pageable pageable) {
        return eventService.findMine(pageable);
    }

    @GetMapping("/{id}")
    public EventResponse findById(@PathVariable Long id) {
        return eventService.findById(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<EventResponse> create(@Valid @RequestBody EventRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(eventService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse update(@PathVariable Long id, @Valid @RequestBody EventRequest request) {
        return eventService.update(id, request);
    }

    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse publish(@PathVariable Long id) {
        return eventService.publish(id);
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse cancel(@PathVariable Long id) {
        return eventService.cancel(id);
    }

    @PatchMapping("/{id}/finish")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public EventResponse finish(@PathVariable Long id) {
        return eventService.finish(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ORGANIZER','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
