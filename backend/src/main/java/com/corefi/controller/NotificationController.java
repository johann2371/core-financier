package com.corefi.controller;

import com.corefi.entity.Notification;
import com.corefi.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final INotificationService notificationService;

    @GetMapping("/role/{role}/non-lues")
    public ResponseEntity<List<Notification>> getNonLues(@PathVariable String role) {
        return ResponseEntity.ok(notificationService.getNonLues(role));
    }

    @PutMapping("/{id}/lue")
    public ResponseEntity<Void> marquerCommeLue(@PathVariable Long id) {
        notificationService.marquerCommeLue(id);
        return ResponseEntity.ok().build();
    }
}
