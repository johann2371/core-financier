package com.corefi.controller;

import com.corefi.entity.Notification;
import com.corefi.service.interfaces.INotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private INotificationService notificationService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Doit retourner les notifications non lues d'un rôle")
    @WithMockUser(authorities = "COMPTABLE")
    void getNonLues_existingRole_returnsList() throws Exception {
        Notification notif = new Notification();
        notif.setId(1L);
        notif.setMessage("Nouvelle facture à valider");
        notif.setLue(false);

        given(notificationService.getNonLues("COMPTABLE")).willReturn(List.of(notif));

        mockMvc.perform(get("/api/notifications/role/COMPTABLE/non-lues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Nouvelle facture à valider"));
    }

    @Test
    @DisplayName("Doit retourner une liste vide si aucune notification non lue")
    @WithMockUser(authorities = "PDG")
    void getNonLues_noNotifications_returnsEmptyList() throws Exception {
        given(notificationService.getNonLues("PDG")).willReturn(List.of());

        mockMvc.perform(get("/api/notifications/role/PDG/non-lues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @DisplayName("Doit marquer une notification comme lue")
    @WithMockUser(authorities = "COMPTABLE")
    void marquerCommeLue_existingId_returnsOk() throws Exception {
        doNothing().when(notificationService).marquerCommeLue(1L);

        mockMvc.perform(put("/api/notifications/1/lue")
                .with(csrf()))
                .andExpect(status().isOk());
    }
}
