package com.corefi.controller;

import com.corefi.dto.response.tableaubord.TableauBordResponse;
import com.corefi.service.interfaces.ITableauBordService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TableauBordController.class)
@AutoConfigureMockMvc(addFilters = false)
class TableauBordControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ITableauBordService tableauBordService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Doit retourner les données du tableau de bord")
    @WithMockUser(authorities = "PDG")
    void getTableauBord_nominal_returnsData() throws Exception {
        TableauBordResponse response = new TableauBordResponse();
        response.setEncaissementsMoisActuel(BigDecimal.valueOf(1500000));
        response.setDecaissementsMoisActuel(BigDecimal.valueOf(800000));

        given(tableauBordService.getKpis(30)).willReturn(response);

        mockMvc.perform(get("/api/tableau-bord/kpis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.encaissementsMoisActuel").value(1500000))
                .andExpect(jsonPath("$.decaissementsMoisActuel").value(800000));
    }
}
