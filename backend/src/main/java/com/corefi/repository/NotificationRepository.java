package com.corefi.repository;

import com.corefi.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByRoleCibleOrderByDateCreationDesc(String roleCible);
    List<Notification> findByRoleCibleAndLueFalseOrderByDateCreationDesc(String roleCible);
}
