package com.corefi.service.impl;

import com.corefi.entity.Notification;
import com.corefi.repository.NotificationRepository;
import com.corefi.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Notification creerEtEnvoyer(String titre, String message, String roleCible) {
        Notification notification = new Notification();
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setRoleCible(roleCible);
        
        Notification saved = notificationRepository.save(notification);

        // Diffuser en temps réel via WebSocket (STOMP)
        messagingTemplate.convertAndSend("/topic/notifications/" + roleCible, saved);

        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> getNonLues(String roleCible) {
        return notificationRepository.findByRoleCibleAndLueFalseOrderByDateCreationDesc(roleCible);
    }

    @Override
    @Transactional
    public void marquerCommeLue(Long id) {
        notificationRepository.findById(id).ifPresent(n -> {
            n.setLue(true);
            notificationRepository.save(n);
        });
    }
}
