package com.corefi.controller;

import com.corefi.dto.request.decaissement.*;
import com.corefi.dto.response.decaissement.DecaissementResponse;
import com.corefi.repository.DecaissementRepository;
import com.corefi.repository.JustificatifRepository;
import com.corefi.service.interfaces.IDecaissementService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DecaissementController.class)
@AutoConfigureMockMvc(addFilters = false)
class DecaissementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IDecaissementService decaissementService;

    @MockBean
    private IPdfService pdfService;

    @MockBean
    private DecaissementRepository decaissementRepository;

    @MockBean
    private JustificatifRepository justificatifRepository;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private DecaissementResponse response;

    @BeforeEach
    void setUp() {
        response = new DecaissementResponse();
        response.setId(1L);
        response.setNumero("DEC-2024-001");
        response.setMontant(BigDecimal.valueOf(75000));
    }

    @Test
    @DisplayName("Doit lister tous les décaissements")
    @WithMockUser(authorities = "COMPTABLE")
    void findAll_nominal_returnsList() throws Exception {
        given(decaissementService.findAll()).willReturn(List.of(response));

        mockMvc.perform(get("/api/decaissements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("DEC-2024-001"));
    }

    @Test
    @DisplayName("Doit retourner un décaissement par ID")
    @WithMockUser(authorities = "RESPONSABLE_FINANCIER")
    void findById_existingId_returnsDecaissement() throws Exception {
        given(decaissementService.findById(1L)).willReturn(response);

        mockMvc.perform(get("/api/decaissements/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.montant").value(75000));
    }

    @Test
    @DisplayName("Doit créer un décaissement")
    @WithMockUser(authorities = "COMPTABLE")
    void creer_validRequest_returnsCreatedDecaissement() throws Exception {
        DecaissementCreateRequest request = new DecaissementCreateRequest();
        request.setMontant(BigDecimal.valueOf(75000));
        request.setMotif("Achat fournitures");
        request.setBeneficiaire("Fournisseur A");
        request.setMoyenPaiement("VIREMENT");

        given(decaissementService.creer(any(DecaissementCreateRequest.class))).willReturn(response);

        mockMvc.perform(post("/api/decaissements")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("DEC-2024-001"));
    }

    @Test
    @DisplayName("Doit soumettre un décaissement pour validation")
    @WithMockUser(authorities = "COMPTABLE")
    void soumettre_existingId_returnsSoumis() throws Exception {
        given(decaissementService.soumettre(1L)).willReturn(response);

        mockMvc.perform(put("/api/decaissements/1/soumettre")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Doit valider un décaissement par le responsable financier")
    @WithMockUser(authorities = "RESPONSABLE_FINANCIER")
    void validerRF_approve_returnsValidated() throws Exception {
        ValidationRFRequest request = new ValidationRFRequest();
        request.setRejeter(false);
        request.setMotif("Approuvé");

        given(decaissementService.validerRF(eq(1L), any(ValidationRFRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/decaissements/1/valider-rf")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Doit rejeter un décaissement par le responsable financier")
    @WithMockUser(authorities = "RESPONSABLE_FINANCIER")
    void validerRF_reject_returnsRejected() throws Exception {
        ValidationRFRequest request = new ValidationRFRequest();
        request.setRejeter(true);
        request.setMotif("Budget insuffisant");

        given(decaissementService.rejeterRF(eq(1L), any(ValidationRFRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/decaissements/1/valider-rf")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Doit approuver un décaissement par le PDG")
    @WithMockUser(authorities = "PDG")
    void approuverPDG_approve_returnsApproved() throws Exception {
        ApprobationPDGRequest request = new ApprobationPDGRequest();
        request.setRejeter(false);
        request.setMotif("Validé par le PDG");

        given(decaissementService.approuverPDG(eq(1L), any(ApprobationPDGRequest.class))).willReturn(response);

        mockMvc.perform(put("/api/decaissements/1/approuver-pdg")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Doit générer le reçu PDF d'un décaissement")
    @WithMockUser(authorities = "COMPTABLE")
    void genererRecuPdf_existingId_returnsPdf() throws Exception {
        byte[] pdfContent = "PDF CONTENT".getBytes();
        given(pdfService.genererRecuDecaissementPdf(1L)).willReturn(pdfContent);

        mockMvc.perform(get("/api/decaissements/1/recu/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
