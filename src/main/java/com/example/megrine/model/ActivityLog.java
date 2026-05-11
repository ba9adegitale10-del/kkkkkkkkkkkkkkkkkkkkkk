package com.example.megrine.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "activity_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;      // Qui a fait l'action
    private String action;        // Ajout, Modification, Suppression
    private String entity;        // Famille, Benevole, Don, Stock...
    private String entityName;    // Nom de l'élément
    private String details;       // Détails supplémentaires
    private String ipAddress;
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private ActionType actionType = ActionType.CREATE;

    public enum ActionType {
        CREATE, UPDATE, DELETE, LOGIN, LOGOUT, VIEW
    }
}
