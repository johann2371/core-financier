package com.corefi.controller;

import com.corefi.service.interfaces.IRapportService;
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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RapportController.class)
@AutoConfigureMockMvc(addFilters = false)
class RapportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IRapportService rapportService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Doit retourner le bilan mensuel en JSON")
    @WithMockUser(authorities = "COMPTABLE")
    void getBilanMensuel_validParams_returnsMap() throws Exception {
        Map<String, Object> bilan = new HashMap<>();
        bilan.put("totalEncaissements", BigDecimal.valueOf(500000));
        bilan.put("totalDecaissements", BigDecimal.valueOf(300000));
        bilan.put("soldeNet", BigDecimal.valueOf(200000));

        given(rapportService.genererBilanMensuel(5, 2026)).willReturn(bilan);

        mockMvc.perform(get("/api/rapports/bilan-mensuel")
                .param("mois", "5")
                .param("annee", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalEncaissements").value(500000))
                .andExpect(jsonPath("$.soldeNet").value(200000));
    }

    @Test
    @DisplayName("Doit générer le bilan mensuel en PDF")
    @WithMockUser(authorities = "PDG")
    void getBilanMensuelPdf_validParams_returnsPdf() throws Exception {
        byte[] pdfContent = "BILAN PDF".getBytes();
        given(rapportService.genererBilanMensuelPdf(5, 2026)).willReturn(pdfContent);

        mockMvc.perform(get("/api/rapports/bilan-mensuel/pdf")
                .param("mois", "5")
                .param("annee", "2026"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
