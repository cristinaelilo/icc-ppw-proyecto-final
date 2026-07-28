package ec.edu.ups.icc.proyecto.domain.event.model;

import ec.edu.ups.icc.proyecto.domain.category.model.Category;
import ec.edu.ups.icc.proyecto.domain.user.model.User;
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

    @Column(length = 200)
    private String location;

    @Column(name = "virtual_url", length = 500)
    private String virtualUrl;

    @Column(nullable = false)
    private Integer capacity;

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

    @Column(nullable = false)
    private boolean deleted = false;

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
