package com.example.megrine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "volunteers")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Volunteer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String firstName;

    @Column(nullable = false)
    private String lastName;

    private String email;
    private String phone;
    private String bloodType;
    private String address;
    private LocalDate joinDate;
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    private VolunteerStatus status = VolunteerStatus.ACTIVE;

    public enum VolunteerStatus {
        ACTIVE, INACTIVE, PENDING
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
