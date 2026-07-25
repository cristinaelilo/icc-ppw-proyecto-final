package ec.edu.ups.icc.proyecto.domain.event;

import ec.edu.ups.icc.proyecto.domain.category.Category;
import ec.edu.ups.icc.proyecto.domain.user.User;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "events")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventModality modality;

    /** Requerido si modality es PRESENTIAL o HYBRID (constraint chk_events_modality_data en BD). */
    @Column(length = 200)
    private String location;

    /** Requerido si modality es VIRTUAL o HYBRID (constraint chk_events_modality_data en BD). */
    @Column(name = "virtual_url", length = 500)
    private String virtualUrl;

    @Column(nullable = false)
    private Integer capacity;

    /**
     * Cupos todavia disponibles. SOLO las inscripciones CONFIRMED lo consumen.
     * Se decrementa/incrementa manualmente dentro de transacciones con bloqueo,
     * NUNCA se recalcula con un COUNT (la columna es la fuente de verdad en BD).
     */
    @Column(name = "available_capacity", nullable = false)
    private Integer availableCapacity;

    @Column(name = "registration_start_at", nullable = false)
    private OffsetDateTime registrationStartAt;

    @Column(name = "registration_end_at", nullable = false)
    private OffsetDateTime registrationEndAt;

    @Column(name = "start_at", nullable = false)
    private OffsetDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private OffsetDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EventStatus status = EventStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organizer_id")
    private User organizer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    /** Eliminacion logica: un evento nunca se borra fisicamente. */
    @Column(nullable = false)
    private boolean deleted = false;

    /** Control de concurrencia optimista (ademas del bloqueo pesimista al inscribirse). */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime updatedAt;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public EventModality getModality() { return modality; }
    public void setModality(EventModality modality) { this.modality = modality; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getVirtualUrl() { return virtualUrl; }
    public void setVirtualUrl(String virtualUrl) { this.virtualUrl = virtualUrl; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getAvailableCapacity() { return availableCapacity; }
    public void setAvailableCapacity(Integer availableCapacity) { this.availableCapacity = availableCapacity; }
    public OffsetDateTime getRegistrationStartAt() { return registrationStartAt; }
    public void setRegistrationStartAt(OffsetDateTime registrationStartAt) { this.registrationStartAt = registrationStartAt; }
    public OffsetDateTime getRegistrationEndAt() { return registrationEndAt; }
    public void setRegistrationEndAt(OffsetDateTime registrationEndAt) { this.registrationEndAt = registrationEndAt; }
    public OffsetDateTime getStartAt() { return startAt; }
    public void setStartAt(OffsetDateTime startAt) { this.startAt = startAt; }
    public OffsetDateTime getEndAt() { return endAt; }
    public void setEndAt(OffsetDateTime endAt) { this.endAt = endAt; }
    public EventStatus getStatus() { return status; }
    public void setStatus(EventStatus status) { this.status = status; }
    public User getOrganizer() { return organizer; }
    public void setOrganizer(User organizer) { this.organizer = organizer; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public boolean isDeleted() { return deleted; }
    public void setDeleted(boolean deleted) { this.deleted = deleted; }
    public Long getVersion() { return version; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
