package com.corefi.controller;

import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.dto.request.utilisateur.ProfileUpdateRequest;
import com.corefi.dto.response.utilisateur.UtilisateurResponse;
import com.corefi.service.interfaces.IUtilisateurService;
import com.corefi.service.interfaces.IFileStorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UtilisateurController.class)
@AutoConfigureMockMvc(addFilters = false)
class UtilisateurControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IUtilisateurService utilisateurService;

    @MockBean
    private IFileStorageService fileStorageService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private UtilisateurResponse response;

    @BeforeEach
    void setUp() {
        response = new UtilisateurResponse();
        response.setId(1L);
        response.setNom("Gouaffo");
        response.setPrenom("Johann");
        response.setEmail("admin@sodica.cm");
        response.setRole("ADMINISTRATEUR");
    }

    @Test
    @DisplayName("Doit lister tous les utilisateurs (ADMIN)")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void findAll_asAdmin_returnsList() throws Exception {
        given(utilisateurService.findAll()).willReturn(List.of(response));

        mockMvc.perform(get("/api/utilisateurs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@sodica.cm"));
    }

    @Test
    @DisplayName("Doit retourner un utilisateur par ID")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void findById_existingId_returnsUtilisateur() throws Exception {
        given(utilisateurService.findById(1L)).willReturn(response);

        mockMvc.perform(get("/api/utilisateurs/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nom").value("Gouaffo"))
                .andExpect(jsonPath("$.prenom").value("Johann"));
    }

    @Test
    @DisplayName("Doit créer un utilisateur")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void creer_validRequest_returnsCreatedUtilisateur() throws Exception {
        UtilisateurCreateRequest request = new UtilisateurCreateRequest();
        request.setNom("Dupont");
        request.setPrenom("Marie");
        request.setEmail("marie@sodica.cm");
        request.setRole("COMPTABLE");
        request.setPassword("P@ssw0rd");

        given(utilisateurService.creer(any(UtilisateurCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/utilisateurs")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Doit mettre à jour un utilisateur")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void mettreAJour_validRequest_returnsUpdated() throws Exception {
        UtilisateurCreateRequest request = new UtilisateurCreateRequest();
        request.setNom("Gouaffo Modifié");
        request.setPrenom("Johann");
        request.setEmail("admin@sodica.cm");
        request.setRole("ADMINISTRATEUR");

        given(utilisateurService.mettreAJour(eq(1L), any(UtilisateurCreateRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/utilisateurs/1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Doit désactiver un utilisateur")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void desactiver_existingId_returnsNoContent() throws Exception {
        doNothing().when(utilisateurService).desactiver(1L);

        mockMvc.perform(delete("/api/utilisateurs/1")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Doit réactiver un utilisateur")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void reactiver_existingId_returnsNoContent() throws Exception {
        doNothing().when(utilisateurService).reactiver(1L);

        mockMvc.perform(put("/api/utilisateurs/1/reactiver")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
