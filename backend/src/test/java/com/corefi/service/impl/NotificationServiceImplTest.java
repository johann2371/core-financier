package com.corefi.service.impl;

import com.corefi.entity.Notification;
import com.corefi.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationServiceImpl notificationService;

    private Notification notification;

    @BeforeEach
    void setUp() {
        notification = new Notification();
        notification.setId(1L);
        notification.setMessage("Décaissement en attente de validation");
        notification.setRoleCible("RESPONSABLE_FINANCIER");
        notification.setLue(false);
    }

    @Test
    @DisplayName("Doit retourner les notifications non lues pour un rôle donné")
    void getNonLues_existingRole_returnsNonLues() {
        given(notificationRepository.findByRoleCibleAndLueFalseOrderByDateCreationDesc("RESPONSABLE_FINANCIER"))
                .willReturn(List.of(notification));

        List<Notification> result = notificationService.getNonLues("RESPONSABLE_FINANCIER");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMessage()).contains("Décaissement");
    }

    @Test
    @DisplayName("Doit marquer une notification comme lue")
    void marquerCommeLue_existingId_updatesNotification() {
        given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

        notificationService.marquerCommeLue(1L);

        assertThat(notification.isLue()).isTrue();
        verify(notificationRepository).save(notification);
    }
}
