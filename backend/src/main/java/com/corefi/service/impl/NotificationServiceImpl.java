package com.corefi.service.impl;

import com.corefi.entity.Notification;
import com.corefi.entity.Utilisateur;
import com.corefi.enums.Role;
import com.corefi.repository.NotificationRepository;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.service.interfaces.IEmailService;
import com.corefi.service.interfaces.INotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationServiceImpl implements INotificationService {

    private final NotificationRepository notificationRepository;
    private final UtilisateurRepository utilisateurRepository;
    private final IEmailService emailService;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public Notification creerEtEnvoyer(String titre, String message, String roleCible) {
        Notification notification = new Notification();
        notification.setTitre(titre);
        notification.setMessage(message);
        notification.setRoleCible(roleCible);
        
        Notification saved = notificationRepository.save(notification);

        // 1. Diffuser en temps réel via WebSocket (STOMP)
        try {
            messagingTemplate.convertAndSend("/topic/notifications/" + roleCible, saved);
        } catch (Exception e) {
            log.warn("Erreur WebSocket : {}", e.getMessage());
        }

        // 2. Envoyer par Mail à tous les utilisateurs du rôle cible
        try {
            Role roleEnum = Role.valueOf(roleCible);
            List<Utilisateur> destinataires = utilisateurRepository.findByRole(roleEnum);
            for (Utilisateur u : destinataires) {
                if (u.getEmail() != null && !u.getEmail().isEmpty()) {
                    emailService.sendSimpleMessage(u.getEmail(), "SODICA - " + titre, message);
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors de l'envoi des mails de notification: {}", e.getMessage());
        }

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
