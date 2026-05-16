package com.example.megrine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String role; // ROLE_ADMIN, ROLE_USER, ROLE_MEMBER

    private String fullName;
    private String email;
    private boolean enabled = true;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = true)
    private Volunteer volunteer;

    // Permissions granulaires (CSV): "VOLUNTEERS,FAMILIES,DONATIONS,STOCK,EVENTS,TRAINING,MEMBER"
    // NULL ou vide = acces complet selon le role
    @Column(length = 500)
    private String permissions = "";

    // Helpers
    public boolean canAccess(String section) {
        if (!enabled) return false;
        if (role != null && role.equals("ROLE_ADMIN")) return true;
        if (permissions == null || permissions.isBlank()) {
            // Par defaut: ROLE_USER peut tout voir sauf admin
            return true;
        }
        return permissions.contains(section.toUpperCase());
    }

    public boolean hasPermission(String section) {
        if (permissions == null || permissions.isBlank()) return true;
        return permissions.contains(section.toUpperCase());
    }
}
