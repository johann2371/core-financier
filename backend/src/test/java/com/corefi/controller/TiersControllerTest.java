package com.corefi.controller;

import com.corefi.dto.request.tiers.TiersCreateRequest;
import com.corefi.dto.response.tiers.TiersResponse;
import com.corefi.service.interfaces.IFileStorageService;
import com.corefi.service.interfaces.IPdfService;
import com.corefi.service.interfaces.ITiersService;
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
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TiersController.class)
@AutoConfigureMockMvc(addFilters = false)
class TiersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ITiersService tiersService;

    @MockBean
    private IFileStorageService fileStorageService;

    @MockBean
    private IPdfService pdfService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private TiersResponse tiersResponse;

    @BeforeEach
    void setUp() {
        tiersResponse = new TiersResponse();
        tiersResponse.setId(1L);
        tiersResponse.setRaisonSociale("Test Tiers");
    }

    @Test
    @DisplayName("Doit lister tous les tiers")
    @WithMockUser(authorities = "COMPTABLE")
    void findAll_nominal_returnsList() throws Exception {
        // given
        given(tiersService.findAll()).willReturn(List.of(tiersResponse));

        // when & then
        mockMvc.perform(get("/api/tiers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].raisonSociale").value("Test Tiers"));
    }

    @Test
    @DisplayName("Doit récupérer un tiers par son ID")
    @WithMockUser(authorities = "COMPTABLE")
    void findById_existingId_returnsTiers() throws Exception {
        // given
        given(tiersService.findById(1L)).willReturn(tiersResponse);

        // when & then
        mockMvc.perform(get("/api/tiers/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raisonSociale").value("Test Tiers"));
    }

    @Test
    @DisplayName("Doit créer un nouveau tiers")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void creer_validRequest_returnsCreatedTiers() throws Exception {
        // given
        TiersCreateRequest request = new TiersCreateRequest();
        request.setRaisonSociale("New Tiers");
        request.setType("CLIENT");
        given(tiersService.creer(any(TiersCreateRequest.class))).willReturn(tiersResponse);

        // when & then
        mockMvc.perform(post("/api/tiers")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.raisonSociale").value("Test Tiers"));
    }

    @Test
    @DisplayName("Doit supprimer un tiers")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void delete_existingId_returnsNoContent() throws Exception {
        // when & then
        mockMvc.perform(delete("/api/tiers/1")
                .with(csrf()))
                .andExpect(status().isNoContent());
    }
}
