package ec.edu.ups.icc.proyecto.domain.registration.services;

import ec.edu.ups.icc.proyecto.common.exception.BusinessRuleException;
import ec.edu.ups.icc.proyecto.common.exception.ForbiddenOperationException;
import ec.edu.ups.icc.proyecto.common.exception.ResourceNotFoundException;
import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import ec.edu.ups.icc.proyecto.domain.event.model.EventStatus;
import ec.edu.ups.icc.proyecto.domain.event.repository.EventRepository;
import ec.edu.ups.icc.proyecto.domain.registration.dto.RegistrationMapper;
import ec.edu.ups.icc.proyecto.domain.registration.dto.RegistrationResponse;
import ec.edu.ups.icc.proyecto.domain.registration.model.Registration;
import ec.edu.ups.icc.proyecto.domain.registration.model.RegistrationStatus;
import ec.edu.ups.icc.proyecto.domain.registration.repository.RegistrationRepository;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import ec.edu.ups.icc.proyecto.domain.user.repository.UserRepository;
import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.EnumSet;

/**
 * Reglas de negocio clave:
 * - Un participante NO puede tener dos inscripciones activas (PENDING o CONFIRMED)
 *   en el mismo evento.
 * - No se puede solicitar inscripcion fuera de la ventana de inscripciones,
 *   ni en un evento no publicado, finalizado o cancelado.
 * - El cupo (available_capacity) SOLO se descuenta al CONFIRMAR (no al solicitar),
 *   y se libera al cancelar una inscripcion que ya estaba CONFIRMED.
 * - Confirmar/rechazar y decrementar/incrementar el cupo ocurre dentro de UNA
 *   transaccion con bloqueo pesimista sobre el evento (lockEventById), para
 *   evitar condiciones de carrera entre solicitudes concurrentes.
 */
@Service
@Transactional
public class RegistrationServiceImpl implements RegistrationService {

    private static final EnumSet<RegistrationStatus> ACTIVE_STATUSES =
            EnumSet.of(RegistrationStatus.PENDING, RegistrationStatus.CONFIRMED);

    private final RegistrationRepository registrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final RegistrationMapper registrationMapper;

    public RegistrationServiceImpl(RegistrationRepository registrationRepository, EventRepository eventRepository,
                                    UserRepository userRepository, RegistrationMapper registrationMapper) {
        this.registrationRepository = registrationRepository;
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.registrationMapper = registrationMapper;
    }

    @Override
    public RegistrationResponse register(Long eventId) {
        Long participantId = currentUserId();

        Event event = eventRepository.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento " + eventId + " no encontrado"));

        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessRuleException("Solo se puede solicitar inscripcion a un evento publicado");
        }

        OffsetDateTime now = OffsetDateTime.now();
        if (now.isBefore(event.getRegistrationStartAt()) || now.isAfter(event.getRegistrationEndAt())) {
            throw new BusinessRuleException("El periodo de inscripciones para este evento no esta abierto");
        }

        if (registrationRepository.existsByEvent_IdAndParticipant_IdAndStatusIn(eventId, participantId, ACTIVE_STATUSES)) {
            throw new BusinessRuleException("Ya tiene una solicitud de inscripcion activa para este evento");
        }

        User participant = userRepository.findById(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Registration registration = new Registration();
        registration.setEvent(event);
        registration.setParticipant(participant);
        registration.setStatus(RegistrationStatus.PENDING);

        return registrationMapper.toResponse(registrationRepository.save(registration));
    }

    @Override
    public RegistrationResponse confirm(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion " + registrationId + " no encontrada"));

        checkEventOwnershipOrAdmin(registration.getEvent().getId());

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Solo una inscripcion PENDING puede confirmarse");
        }

        // Bloqueo pesimista: serializa confirmaciones concurrentes sobre el mismo evento.
        Event event = eventRepository.lockByIdNotDeleted(registration.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));

        if (event.getAvailableCapacity() <= 0) {
            throw new BusinessRuleException("No hay cupos disponibles para este evento");
        }

        event.setAvailableCapacity(event.getAvailableCapacity() - 1);
        eventRepository.save(event);

        registration.setStatus(RegistrationStatus.CONFIRMED);
        registration.setConfirmedAt(OffsetDateTime.now());
        registration.setStatusUpdatedAt(OffsetDateTime.now());

        return registrationMapper.toResponse(registrationRepository.save(registration));
    }

    @Override
    public RegistrationResponse reject(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion " + registrationId + " no encontrada"));

        checkEventOwnershipOrAdmin(registration.getEvent().getId());

        if (registration.getStatus() != RegistrationStatus.PENDING) {
            throw new BusinessRuleException("Solo una inscripcion PENDING puede rechazarse");
        }

        registration.setStatus(RegistrationStatus.REJECTED);
        registration.setStatusUpdatedAt(OffsetDateTime.now());

        return registrationMapper.toResponse(registrationRepository.save(registration));
    }

    @Override
    public void cancel(Long registrationId) {
        Registration registration = registrationRepository.findById(registrationId)
                .orElseThrow(() -> new ResourceNotFoundException("Inscripcion " + registrationId + " no encontrada"));

        boolean isOwner = registration.getParticipant().getId().equals(currentUserIdOrNull());
        if (!isOwner && !isAdmin()) {
            throw new ForbiddenOperationException("Solo el participante propietario puede cancelar esta inscripcion");
        }

        if (registration.getStatus() == RegistrationStatus.CANCELLED
                || registration.getStatus() == RegistrationStatus.REJECTED) {
            throw new BusinessRuleException("La inscripcion ya se encuentra cancelada o rechazada");
        }

        boolean wasConfirmed = registration.getStatus() == RegistrationStatus.CONFIRMED;

        if (wasConfirmed) {
            // Bloqueo pesimista: libera el cupo de forma segura ante operaciones concurrentes.
            Event event = eventRepository.lockByIdNotDeleted(registration.getEvent().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Evento no encontrado"));
            event.setAvailableCapacity(Math.min(event.getAvailableCapacity() + 1, event.getCapacity()));
            eventRepository.save(event);
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registration.setCancelledAt(OffsetDateTime.now());
        registration.setStatusUpdatedAt(OffsetDateTime.now());
        registrationRepository.save(registration);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponse> findMine(Pageable pageable) {
        return registrationRepository.findByParticipant_Id(currentUserId(), pageable)
                .map(registrationMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RegistrationResponse> findByEvent(Long eventId, RegistrationStatus status, Pageable pageable) {
        checkEventOwnershipOrAdmin(eventId);
        Page<Registration> page = status != null
                ? registrationRepository.findByEvent_IdAndStatus(eventId, status, pageable)
                : registrationRepository.findByEvent_Id(eventId, pageable);
        return page.map(registrationMapper::toResponse);
    }

    // ---------------- Helpers ----------------

    private void checkEventOwnershipOrAdmin(Long eventId) {
        Event event = eventRepository.findByIdAndDeletedFalse(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Evento " + eventId + " no encontrado"));
        if (!isAdmin() && !event.getOrganizer().getId().equals(currentUserId())) {
            throw new ForbiddenOperationException("No puede administrar inscripciones de un evento que no le pertenece");
        }
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
