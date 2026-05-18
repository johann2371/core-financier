package com.corefi.controller;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.request.facture.LigneFactureRequest;
import com.corefi.dto.response.facture.FactureResponse;
import com.corefi.service.interfaces.IFactureService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FactureController.class)
@AutoConfigureMockMvc(addFilters = false)
class FactureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IFactureService factureService;

    @MockBean
    private IPdfService pdfService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private FactureResponse response;

    @BeforeEach
    void setUp() {
        response = new FactureResponse();
        response.setId(1L);
        response.setNumero("FAC-2024-001");
    }

    @Test
    @DisplayName("Doit lister toutes les factures")
    @WithMockUser(authorities = "COMPTABLE")
    void findAll_nominal_returnsList() throws Exception {
        // given
        given(factureService.findAll()).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/factures"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numero").value("FAC-2024-001"));
    }

    @Test
    @DisplayName("Doit créer une facture")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void creer_validRequest_returnsCreatedFacture() throws Exception {
        // given
        FactureCreateRequest request = new FactureCreateRequest();
        request.setTiersId(1L);
        request.setType("VENTE");
        
        LigneFactureRequest ligne = new LigneFactureRequest();
        ligne.setDesignation("Designation");
        ligne.setQuantite(BigDecimal.ONE);
        ligne.setPrixUnitaire(BigDecimal.valueOf(100));
        request.setLignes(Collections.singletonList(ligne));

        given(factureService.creer(any(FactureCreateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/api/factures")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numero").value("FAC-2024-001"));
    }

    @Test
    @DisplayName("Doit valider une facture")
    @WithMockUser(authorities = "RESPONSABLE_FINANCIER")
    void valider_existingId_returnsValidatedFacture() throws Exception {
        // given
        given(factureService.valider(1L)).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/factures/1/valider")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Doit générer le PDF de la facture")
    @WithMockUser(authorities = "CAISSIER")
    void genererPdf_existingId_returnsPdfBytes() throws Exception {
        // given
        byte[] pdfContent = "PDF CONTENT".getBytes();
        given(pdfService.genererFacturePdf(1L)).willReturn(pdfContent);

        // when & then
        mockMvc.perform(get("/api/factures/1/pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(content().bytes(pdfContent));
    }
}
