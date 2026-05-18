package com.corefi.controller;

import com.corefi.entity.JournalAudit;
import com.corefi.entity.Utilisateur;
import com.corefi.service.interfaces.IJournalAuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(JournalAuditController.class)
@AutoConfigureMockMvc(addFilters = false)
class JournalAuditControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IJournalAuditService journalAuditService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Doit retourner l'historique d'audit paginé")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void findAll_nominal_returnsPage() throws Exception {
        JournalAudit audit = new JournalAudit();
        audit.setId(1L);
        audit.setAction("CREATION_FACTURE");
        Utilisateur user = new Utilisateur();
        user.setEmail("admin@sodica.cm");
        audit.setUtilisateur(user);
        audit.setDateAction(LocalDateTime.now());

        given(journalAuditService.findAll(any(), any(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(audit)));

        mockMvc.perform(get("/api/journal-audit"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].action").value("CREATION_FACTURE"));
    }
}
