package com.corefi.controller;

import com.corefi.dto.request.encaissement.EncaissementCreateRequest;
import com.corefi.dto.response.encaissement.EncaissementResponse;
import com.corefi.service.interfaces.IEncaissementService;
import com.corefi.service.interfaces.IPdfService;
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

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EncaissementController.class)
@AutoConfigureMockMvc(addFilters = false)
class EncaissementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IEncaissementService encaissementService;

    @MockBean
    private IPdfService pdfService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private EncaissementResponse response;

    @BeforeEach
    void setUp() {
        response = new EncaissementResponse();
        response.setId(1L);
        response.setNumero("ENC-2024-001");
        response.setMontant(BigDecimal.valueOf(50000));
    }

    @Test
    @DisplayName("Doit lister tous les encaissements")
    @WithMockUser(authorities = "COMPTABLE")
    void findAll_nominal_returnsList() throws Exception {
        given(encaissementService.findAll()).willReturn(List.of(response));

        mockMvc.perform(get("/api/encaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("ENC-2024-001"));
    }

    @Test
    @DisplayName("Doit retourner un encaissement par ID")
    @WithMockUser(authorities = "COMPTABLE")
    void findById_existingId_returnsEncaissement() throws Exception {
        given(encaissementService.findById(1L)).willReturn(response);

        mockMvc.perform(get("/api/encaissements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.montant").value(50000));
    }

    @Test
    @DisplayName("Doit créer un encaissement")
    @WithMockUser(authorities = "CAISSIER")
    void creer_validRequest_returnsCreatedEncaissement() throws Exception {
        EncaissementCreateRequest request = new EncaissementCreateRequest();
        request.setClientId(1L);
        request.setMontant(BigDecimal.valueOf(50000));
        request.setMoyenPaiement("ESPECES");
        request.setCompteFinancierId(1L);

        given(encaissementService.creer(any(EncaissementCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/encaissements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("ENC-2024-001"));
    }

    @Test
    @DisplayName("Doit générer le reçu PDF d'un encaissement")
    @WithMockUser(authorities = "COMPTABLE")
    void genererRecuPdf_existingId_returnsPdf() throws Exception {
        byte[] pdfContent = "PDF CONTENT".getBytes();
        given(pdfService.genererRecuEncaissementPdf(1L)).willReturn(pdfContent);

        mockMvc.perform(get("/api/encaissements/1/recu/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
