package ec.edu.ups.icc.proyecto.domain.registration.services;

import ec.edu.ups.icc.proyecto.common.exception.BusinessRuleException;
import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import ec.edu.ups.icc.proyecto.domain.event.model.EventStatus;
import ec.edu.ups.icc.proyecto.domain.event.repository.EventRepository;
import ec.edu.ups.icc.proyecto.domain.registration.model.Registration;
import ec.edu.ups.icc.proyecto.domain.registration.model.RegistrationStatus;
import ec.edu.ups.icc.proyecto.domain.registration.repository.RegistrationRepository;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import ec.edu.ups.icc.proyecto.domain.user.repository.UserRepository;
import ec.edu.ups.icc.proyecto.security.UserPrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas de las reglas de negocio mas sensibles de inscripciones:
 * - No se puede solicitar inscripcion a un evento no publicado.
 * - No se puede solicitar fuera de la ventana de inscripciones.
 * - No se puede tener dos inscripciones activas (PENDING/CONFIRMED) en el mismo evento.
 * - No se puede confirmar una inscripcion si no hay cupos disponibles.
 */
@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock private RegistrationRepository registrationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private ec.edu.ups.icc.proyecto.domain.registration.dto.RegistrationMapper registrationMapper;

    private RegistrationServiceImpl registrationService;

    private static final Long PARTICIPANT_ID = 10L;
    private static final Long EVENT_ID = 1L;

    @BeforeEach
    void setUp() {
        registrationService = new RegistrationServiceImpl(registrationRepository, eventRepository,
                userRepository, registrationMapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String role) {
        UserPrincipal principal = UserPrincipal.from(userId, "user" + userId + "@ups.edu.ec", "hash", List.of(role));
        var auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void register_deberiaFallarSiElEventoNoEstaPublicado() {
        authenticateAs(PARTICIPANT_ID, "PARTICIPANT");

        Event event = mock(Event.class);
        when(event.getStatus()).thenReturn(EventStatus.DRAFT);
        when(eventRepository.findByIdAndDeletedFalse(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.register(EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("publicado");
    }

    @Test
    void register_deberiaFallarSiLaVentanaDeInscripcionNoEstaAbierta() {
        authenticateAs(PARTICIPANT_ID, "PARTICIPANT");

        Event event = mock(Event.class);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getRegistrationStartAt()).thenReturn(OffsetDateTime.now().plusDays(5));
        lenient().when(event.getRegistrationEndAt()).thenReturn(OffsetDateTime.now().plusDays(10));
        when(eventRepository.findByIdAndDeletedFalse(EVENT_ID)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.register(EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("periodo de inscripciones");
    }

    @Test
    void register_deberiaFallarSiYaTieneUnaInscripcionActiva() {
        authenticateAs(PARTICIPANT_ID, "PARTICIPANT");

        Event event = mock(Event.class);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getRegistrationStartAt()).thenReturn(OffsetDateTime.now().minusDays(1));
        when(event.getRegistrationEndAt()).thenReturn(OffsetDateTime.now().plusDays(10));
        when(eventRepository.findByIdAndDeletedFalse(EVENT_ID)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEvent_IdAndParticipant_IdAndStatusIn(eq(EVENT_ID), eq(PARTICIPANT_ID), anyCollection()))
                .thenReturn(true);

        assertThatThrownBy(() -> registrationService.register(EVENT_ID))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("solicitud de inscripcion activa");

        verify(registrationRepository, never()).save(any());
    }

    @Test
    void register_deberiaCrearSolicitudPendienteSiTodoEsValido() {
        authenticateAs(PARTICIPANT_ID, "PARTICIPANT");

        Event event = mock(Event.class);
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getRegistrationStartAt()).thenReturn(OffsetDateTime.now().minusDays(1));
        when(event.getRegistrationEndAt()).thenReturn(OffsetDateTime.now().plusDays(10));
        when(eventRepository.findByIdAndDeletedFalse(EVENT_ID)).thenReturn(Optional.of(event));
        when(registrationRepository.existsByEvent_IdAndParticipant_IdAndStatusIn(eq(EVENT_ID), eq(PARTICIPANT_ID), anyCollection()))
                .thenReturn(false);

        User participant = new User();
        when(userRepository.findById(PARTICIPANT_ID)).thenReturn(Optional.of(participant));
        when(registrationRepository.save(any(Registration.class))).thenAnswer(inv -> inv.getArgument(0));

        registrationService.register(EVENT_ID);

        verify(registrationRepository).save(argThat(r -> r.getStatus() == RegistrationStatus.PENDING));
    }

    @Test
    void confirm_deberiaFallarSiNoHayCuposDisponibles() {
        authenticateAs(1L, "ADMIN");

        Event event = mock(Event.class);
        when(event.getId()).thenReturn(EVENT_ID);

        Registration registration = mock(Registration.class);
        when(registration.getStatus()).thenReturn(RegistrationStatus.PENDING);
        when(registration.getEvent()).thenReturn(event);

        when(registrationRepository.findById(5L)).thenReturn(Optional.of(registration));
        when(eventRepository.findByIdAndDeletedFalse(EVENT_ID)).thenReturn(Optional.of(event));

        Event lockedEvent = mock(Event.class);
        when(lockedEvent.getAvailableCapacity()).thenReturn(0);
        when(eventRepository.lockByIdNotDeleted(EVENT_ID)).thenReturn(Optional.of(lockedEvent));

        assertThatThrownBy(() -> registrationService.confirm(5L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cupos");

        verify(registrationRepository, never()).save(any());
    }
}