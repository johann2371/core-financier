package com.corefi.service.interfaces;

import com.corefi.entity.Notification;
import java.util.List;

public interface INotificationService {
    Notification creerEtEnvoyer(String titre, String message, String roleCible);
    List<Notification> getNonLues(String roleCible);
    void marquerCommeLue(Long id);
}
