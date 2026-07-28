package ec.edu.ups.icc.proyecto.domain.user.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "roles")
public class Role {

    public static final String ADMIN = "ADMIN";
    public static final String ORGANIZER = "ORGANIZER";
    public static final String PARTICIPANT = "PARTICIPANT";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String name;

    @Column(nullable = false, length = 150)
    private String description;

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
