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

    // ROLE_ADMIN, ROLE_USER, ROLE_MEMBER
    @Column(nullable = false)
    private String role;

    private String fullName;
    private String email;
    private boolean enabled = true;

    // Lien optionnel vers le profil benevole
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "volunteer_id", nullable = true)
    private Volunteer volunteer;
}
