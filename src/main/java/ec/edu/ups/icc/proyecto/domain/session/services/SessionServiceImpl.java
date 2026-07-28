package ec.edu.ups.icc.proyecto.domain.session.services;

import ec.edu.ups.icc.proyecto.common.exception.BusinessRuleException;
import ec.edu.ups.icc.proyecto.common.exception.DuplicateResourceException;
import ec.edu.ups.icc.proyecto.common.exception.ForbiddenOperationException;
import ec.edu.ups.icc.proyecto.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import ec.edu.ups.icc.proyecto.domain.event.model.EventStatus;
import ec.edu.ups.icc.proyecto.domain.event.repository.EventRepository;
import ec.edu.ups.icc.proyecto.domain.session.dto.SessionMapper;
import ec.edu.ups.icc.proyecto.domain.session.dto.SessionRequest;
import ec.edu.ups.icc.proyecto.domain.session.dto.SessionResponse;
import ec.edu.ups.icc.proyecto.domain.session.model.Session;
import ec.edu.ups.icc.proyecto.domain.session.repository.SessionRepository;
import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class SessionServiceImpl implements SessionService {

    private final SessionRepository sessionRepository;
    private final EventRepository eventRepository;
    private final SessionMapper sessionMapper;

    public SessionServiceImpl(SessionRepository sessionRepository, EventRepository eventRepository,
                               SessionMapper sessionMapper) {
        this.sessionRepository = sessionRepository;
        this.eventRepository = eventRepository;
        this.sessionMapper = sessionMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionResponse> findByEvent(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento " + eventId + " no encontrado"));

        boolean isOwnerOrAdmin = isAdmin() || event.getOrganizer().getId().equals(currentUserIdOrNull());
        if (event.getStatus() != EventStatus.PUBLISHED && !isOwnerOrAdmin) {
            throw new ResourceNotFoundException("Evento " + eventId + " no encontrado");
        }

        return sessionRepository.findByEvent_IdOrderByStartAtAsc(eventId).stream()
                .map(sessionMapper::toResponse).toList();
    }

    @Override
    public SessionResponse create(Long eventId, SessionRequest request) {
        Event event = getEventAndCheckOwnership(eventId);
        validateBusinessRules(event, request, null);

        Session session = new Session();
        session.setEvent(event);
        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setStartAt(request.startAt());
        session.setEndAt(request.endAt());
        session.setLocation(request.location());
        session.setVirtualUrl(request.virtualUrl());

        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    public SessionResponse update(Long eventId, Long sessionId, SessionRequest request) {
        Event event = getEventAndCheckOwnership(eventId);
        Session session = getSessionOfEvent(eventId, sessionId);
        validateBusinessRules(event, request, sessionId);

        session.setTitle(request.title());
        session.setDescription(request.description());
        session.setStartAt(request.startAt());
        session.setEndAt(request.endAt());
        session.setLocation(request.location());
        session.setVirtualUrl(request.virtualUrl());

        return sessionMapper.toResponse(sessionRepository.save(session));
    }

    @Override
    public void delete(Long eventId, Long sessionId) {
        getEventAndCheckOwnership(eventId);
        Session session = getSessionOfEvent(eventId, sessionId);
        sessionRepository.delete(session);
    }

    // ---------------- Helpers ----------------

    private void validateBusinessRules(Event event, SessionRequest request, Long ignoreSessionId) {
        if (!request.startAt().isBefore(request.endAt())) {
            throw new BusinessRuleException("La hora de fin debe ser posterior a la hora de inicio");
        }
        if (request.startAt().isBefore(event.getStartAt()) || request.endAt().isAfter(event.getEndAt())) {
            throw new BusinessRuleException("La sesion debe estar dentro del rango de fechas del evento");
        }
        boolean duplicate = sessionRepository.existsByEvent_IdAndTitleAndStartAt(
                event.getId(), request.title(), request.startAt());
        if (duplicate) {
            // Al actualizar, si el duplicado encontrado es la propia sesion que se edita, se permite.
            if (ignoreSessionId == null) {
                throw new DuplicateResourceException(
                        "Ya existe una sesion con ese titulo y hora de inicio para este evento");
            }
        }
    }

    private Session getSessionOfEvent(Long eventId, Long sessionId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getEvent().getId().equals(eventId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Sesion " + sessionId + " no encontrada para el evento " + eventId));
    }

    private Event getEventAndCheckOwnership(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento " + eventId + " no encontrado"));
        if (!isAdmin() && !event.getOrganizer().getId().equals(currentUserId())) {
            throw new ForbiddenOperationException("No puede administrar sesiones de un evento que no le pertenece");
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
