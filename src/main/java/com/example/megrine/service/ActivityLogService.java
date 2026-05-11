package com.example.megrine.service;

import com.example.megrine.model.ActivityLog;
import com.example.megrine.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;

@Service
public class ActivityLogService {

    @Autowired
    private ActivityLogRepository logRepo;

    public void log(String action, ActivityLog.ActionType type, String entity,
                    String entityName, String details) {
        try {
            ActivityLog log = new ActivityLog();
            log.setAction(action);
            log.setActionType(type);
            log.setEntity(entity);
            log.setEntityName(entityName);
            log.setDetails(details);
            log.setCreatedAt(LocalDateTime.now());

            // Get current user
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                log.setUsername(auth.getName());
            } else {
                log.setUsername("Systeme");
            }

            logRepo.save(log);
        } catch (Exception e) {
            // Silent fail - logs ne doivent pas bloquer l'app
        }
    }

    public void logCreate(String entity, String entityName) {
        log("Ajout de " + entityName, ActivityLog.ActionType.CREATE, entity, entityName, null);
    }

    public void logUpdate(String entity, String entityName) {
        log("Modification de " + entityName, ActivityLog.ActionType.UPDATE, entity, entityName, null);
    }

    public void logDelete(String entity, String entityName) {
        log("Suppression de " + entityName, ActivityLog.ActionType.DELETE, entity, entityName, null);
    }

    public void logLogin(String username) {
        ActivityLog log = new ActivityLog();
        log.setUsername(username);
        log.setAction("Connexion au systeme");
        log.setActionType(ActivityLog.ActionType.LOGIN);
        log.setEntity("Systeme");
        log.setEntityName("Session");
        log.setCreatedAt(LocalDateTime.now());
        logRepo.save(log);
    }
}
