package com.example.megrine.repository;

import com.example.megrine.model.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {
    List<ActivityLog> findTop50ByOrderByCreatedAtDesc();
    List<ActivityLog> findByUsernameOrderByCreatedAtDesc(String username);
    List<ActivityLog> findByEntityOrderByCreatedAtDesc(String entity);
    List<ActivityLog> findByActionTypeOrderByCreatedAtDesc(ActivityLog.ActionType actionType);

    @Query("SELECT a.username, COUNT(a) FROM ActivityLog a GROUP BY a.username ORDER BY COUNT(a) DESC")
    List<Object[]> countByUser();

    @Query("SELECT a.entity, COUNT(a) FROM ActivityLog a GROUP BY a.entity ORDER BY COUNT(a) DESC")
    List<Object[]> countByEntity();

    long countByActionType(ActivityLog.ActionType type);
}
