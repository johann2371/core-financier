package com.corefi.controller;

import com.corefi.dto.response.sessioncaisse.SessionCaisseResponse;
import com.corefi.service.interfaces.ISessionCaisseService;
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

import java.util.List;
import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SessionCaisseController.class)
@AutoConfigureMockMvc(addFilters = false)
class SessionCaisseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ISessionCaisseService sessionCaisseService;

    @MockBean
    private IPdfService pdfService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private SessionCaisseResponse response;

    @BeforeEach
    void setUp() {
        response = new SessionCaisseResponse();
        response.setId(1L);
    }

    @Test
    @DisplayName("Doit retourner la session active du caissier")
    @WithMockUser(authorities = "CAISSIER")
    void getActive_existingSession_returnsSession() throws Exception {
        given(sessionCaisseService.getSessionActiveCurrentCaissier()).willReturn(Optional.of(response));

        mockMvc.perform(get("/api/sessions-caisse/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Doit retourner 204 si aucune session active")
    @WithMockUser(authorities = "CAISSIER")
    void getActive_noSession_returnsNoContent() throws Exception {
        given(sessionCaisseService.getSessionActiveCurrentCaissier()).willReturn(Optional.empty());

        mockMvc.perform(get("/api/sessions-caisse/active"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Doit retourner l'historique global des sessions")
    @WithMockUser(authorities = "RESPONSABLE_FINANCIER")
    void getHistoriqueGlobal_nominal_returnsList() throws Exception {
        given(sessionCaisseService.getAllHistoriqueSessions()).willReturn(List.of(response));

        mockMvc.perform(get("/api/sessions-caisse/historique-global"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("Doit retourner une session par ID")
    @WithMockUser(authorities = "CAISSIER")
    void getById_existingId_returnsSession() throws Exception {
        given(sessionCaisseService.getById(1L)).willReturn(response);

        mockMvc.perform(get("/api/sessions-caisse/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @DisplayName("Doit générer le rapport PDF de clôture de caisse")
    @WithMockUser(authorities = "CAISSIER")
    void genererRapportPdf_existingId_returnsPdf() throws Exception {
        byte[] pdfContent = "RAPPORT PDF".getBytes();
        given(pdfService.genererRapportCloturePdf(1L)).willReturn(pdfContent);

        mockMvc.perform(get("/api/sessions-caisse/1/rapport-pdf"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
