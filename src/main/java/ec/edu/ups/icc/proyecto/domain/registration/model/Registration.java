package ec.edu.ups.icc.proyecto.domain.registration.model;

import ec.edu.ups.icc.proyecto.domain.event.model.Event;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "registrations", uniqueConstraints = @UniqueConstraint(columnNames = {"event_id", "participant_id"}))
public class Registration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "registration_code", nullable = false, unique = true)
    private UUID registrationCode = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "participant_id")
    private User participant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RegistrationStatus status = RegistrationStatus.PENDING;

    @Column(name = "registered_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime registeredAt;

    @Column(name = "status_updated_at", nullable = false)
    private OffsetDateTime statusUpdatedAt;

    @Column(name = "confirmed_at")
    private OffsetDateTime confirmedAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @Version
    @Column(nullable = false)
    private Long version;

    @PrePersist
    void prePersist() {
        if (statusUpdatedAt == null) {
            statusUpdatedAt = OffsetDateTime.now();
        }
    }

    public Long getId() { return id; }
    public UUID getRegistrationCode() { return registrationCode; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public User getParticipant() { return participant; }
    public void setParticipant(User participant) { this.participant = participant; }
    public RegistrationStatus getStatus() { return status; }
    public void setStatus(RegistrationStatus status) { this.status = status; }
    public OffsetDateTime getRegisteredAt() { return registeredAt; }
    public OffsetDateTime getStatusUpdatedAt() { return statusUpdatedAt; }
    public void setStatusUpdatedAt(OffsetDateTime statusUpdatedAt) { this.statusUpdatedAt = statusUpdatedAt; }
    public OffsetDateTime getConfirmedAt() { return confirmedAt; }
    public void setConfirmedAt(OffsetDateTime confirmedAt) { this.confirmedAt = confirmedAt; }
    public OffsetDateTime getCancelledAt() { return cancelledAt; }
    public void setCancelledAt(OffsetDateTime cancelledAt) { this.cancelledAt = cancelledAt; }
    public Long getVersion() { return version; }
}
