package ec.edu.ups.icc.proyecto.domain.event.services;

import ec.edu.ups.icc.proyecto.common.exception.BusinessRuleException;
import ec.edu.ups.icc.proyecto.common.exception.ForbiddenOperationException;
import ec.edu.ups.icc.proyecto.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.proyecto.domain.category.model.Category;
import ec.edu.ups.icc.proyecto.domain.category.repository.CategoryRepository;
import ec.edu.ups.icc.proyecto.domain.event.dto.EventMapper;
import ec.edu.ups.icc.proyecto.domain.event.dto.EventRequest;
import ec.edu.ups.icc.proyecto.domain.event.dto.EventResponse;
import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import ec.edu.ups.icc.proyecto.domain.event.model.EventModality;
import ec.edu.ups.icc.proyecto.domain.event.model.EventStatus;
import ec.edu.ups.icc.proyecto.domain.event.repository.EventRepository;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import ec.edu.ups.icc.proyecto.domain.user.repository.UserRepository;
import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;
    private final EventMapper eventMapper;

    public EventServiceImpl(EventRepository eventRepository, CategoryRepository categoryRepository,
                             UserRepository userRepository, EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.categoryRepository = categoryRepository;
        this.userRepository = userRepository;
        this.eventMapper = eventMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> searchPublic(String search, Long categoryId, EventModality modality,
                                             OffsetDateTime from, OffsetDateTime to, EventStatus status,
                                             Pageable pageable) {
        boolean admin = isAdmin();

        Specification<Event> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("deleted")));

            if (admin && status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            } else if (!admin) {
                // Quien no es ADMIN solo puede ver el catalogo publico (eventos ya publicados).
                predicates.add(cb.equal(root.get("status"), EventStatus.PUBLISHED));
            }
            if (search != null && !search.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%"));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (modality != null) {
                predicates.add(cb.equal(root.get("modality"), modality));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("startAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("endAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return eventRepository.findAll(spec, pageable).map(eventMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventResponse> findMine(Pageable pageable) {
        Long organizerId = currentUserId();
        Specification<Event> spec = (root, query, cb) -> cb.and(
                cb.isFalse(root.get("deleted")),
                cb.equal(root.get("organizer").get("id"), organizerId)
        );
        return eventRepository.findAll(spec, pageable).map(eventMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public EventResponse findById(Long id) {
        Event event = eventRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento " + id + " no encontrado"));

        boolean isOwnerOrAdmin = isAdmin() || event.getOrganizer().getId().equals(currentUserIdOrNull());
        if (event.getStatus() != EventStatus.PUBLISHED && !isOwnerOrAdmin) {
            // No se revela la existencia de eventos no publicados a terceros.
            throw new ResourceNotFoundException("Evento " + id + " no encontrado");
        }
        return eventMapper.toResponse(event);
    }

    @Override
    public EventResponse create(EventRequest request) {
        validateDates(request);
        validateModalityData(request.modality(), request.location(), request.virtualUrl());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria " + request.categoryId() + " no encontrada"));
        if (!category.isActive()) {
            throw new BusinessRuleException("No se puede crear un evento en una categoria inactiva");
        }

        User organizer = userRepository.findById(currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Event event = new Event();
        applyRequest(event, request, category);
        event.setOrganizer(organizer);
        event.setStatus(EventStatus.DRAFT);
        event.setAvailableCapacity(request.capacity());
        event.setDeleted(false);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse update(Long id, EventRequest request) {
        Event event = getEventAndCheckOwnership(id);

        if (event.getStatus() == EventStatus.FINISHED || event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("No se puede modificar un evento finalizado o cancelado");
        }

        validateDates(request);
        validateModalityData(request.modality(), request.location(), request.virtualUrl());

        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria " + request.categoryId() + " no encontrada"));
        if (!category.isActive()) {
            throw new BusinessRuleException("No se puede asignar una categoria inactiva");
        }

        // Preserva los cupos ya ocupados al cambiar la capacidad total.
        int usedSlots = event.getCapacity() - event.getAvailableCapacity();
        if (request.capacity() < usedSlots) {
            throw new BusinessRuleException(
                    "No se puede reducir el cupo por debajo de las inscripciones ya confirmadas (" + usedSlots + ")");
        }

        applyRequest(event, request, category);
        event.setAvailableCapacity(request.capacity() - usedSlots);

        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse publish(Long id) {
        Event event = getEventAndCheckOwnership(id);
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessRuleException("Solo un evento en borrador puede publicarse");
        }
        event.setStatus(EventStatus.PUBLISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse cancel(Long id) {
        Event event = getEventAndCheckOwnership(id);
        if (event.getStatus() == EventStatus.FINISHED || event.getStatus() == EventStatus.CANCELLED) {
            throw new BusinessRuleException("El evento ya esta finalizado o cancelado");
        }
        event.setStatus(EventStatus.CANCELLED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public EventResponse finish(Long id) {
        Event event = getEventAndCheckOwnership(id);
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Solo un evento publicado puede marcarse como finalizado");
        }
        event.setStatus(EventStatus.FINISHED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Override
    public void delete(Long id) {
        Event event = getEventAndCheckOwnership(id);
        // Regla de negocio: un evento PUBLICADO o FINALIZADO nunca se elimina fisicamente
        // (garantiza que jamas se borre un evento publicado con inscritos). Debe cancelarse
        // primero (transicion PUBLISHED -> CANCELLED) para luego poder eliminarlo (logicamente).
        if (event.getStatus() == EventStatus.PUBLISHED || event.getStatus() == EventStatus.FINISHED) {
            throw new BusinessRuleException(
                    "No se puede eliminar un evento publicado o finalizado. Cancelelo primero si aplica.");
        }
        event.setDeleted(true);
        eventRepository.save(event);
    }

    // ---------------- Helpers ----------------

    private void applyRequest(Event event, EventRequest request, Category category) {
        event.setTitle(request.title());
        event.setDescription(request.description());
        event.setModality(request.modality());
        event.setLocation(request.location());
        event.setVirtualUrl(request.virtualUrl());
        event.setCapacity(request.capacity());
        event.setRegistrationStartAt(request.registrationStartAt());
        event.setRegistrationEndAt(request.registrationEndAt());
        event.setStartAt(request.startAt());
        event.setEndAt(request.endAt());
        event.setCategory(category);
    }

    private void validateDates(EventRequest request) {
        if (!request.registrationStartAt().isBefore(request.registrationEndAt())) {
            throw new BusinessRuleException("El inicio de inscripciones debe ser anterior a su fin");
        }
        if (request.registrationEndAt().isAfter(request.startAt())) {
            throw new BusinessRuleException("El fin de inscripciones no puede ser posterior al inicio del evento");
        }
        if (!request.startAt().isBefore(request.endAt())) {
            throw new BusinessRuleException("La fecha de inicio del evento debe ser anterior a la de fin");
        }
    }

    private void validateModalityData(EventModality modality, String location, String virtualUrl) {
        boolean hasLocation = location != null && !location.isBlank();
        boolean hasVirtualUrl = virtualUrl != null && !virtualUrl.isBlank();

        boolean valid = switch (modality) {
            case PRESENTIAL -> hasLocation && !hasVirtualUrl;
            case VIRTUAL -> !hasLocation && hasVirtualUrl;
            case HYBRID -> hasLocation && hasVirtualUrl;
        };

        if (!valid) {
            throw new BusinessRuleException(switch (modality) {
                case PRESENTIAL -> "Un evento presencial requiere 'location' y no debe tener 'virtualUrl'";
                case VIRTUAL -> "Un evento virtual requiere 'virtualUrl' y no debe tener 'location'";
                case HYBRID -> "Un evento hibrido requiere tanto 'location' como 'virtualUrl'";
            });
        }
    }

    private Event getEventAndCheckOwnership(Long id) {
        Event event = eventRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento " + id + " no encontrado"));
        if (!isAdmin() && !event.getOrganizer().getId().equals(currentUserId())) {
            throw new ForbiddenOperationException("No puede administrar un evento que no le pertenece");
        }
        return event;
    }

    private Long currentUserId() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return principal.getId();
    }

    private Long currentUserIdOrNull() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal.getId();
    }

    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
