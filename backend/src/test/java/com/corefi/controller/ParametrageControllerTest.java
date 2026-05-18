package com.corefi.controller;

import com.corefi.dto.request.parametrage.ParametrageUpdateRequest;
import com.corefi.dto.response.parametrage.ParametrageResponse;
import com.corefi.service.interfaces.IFileStorageService;
import com.corefi.service.interfaces.IParametrageService;
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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParametrageController.class)
@AutoConfigureMockMvc(addFilters = false)
class ParametrageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IParametrageService parametrageService;

    @MockBean
    private IFileStorageService fileStorageService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private ParametrageResponse response;

    @BeforeEach
    void setUp() {
        response = new ParametrageResponse();
        response.setCle("TEST_KEY");
        response.setValeur("TEST_VALUE");
    }

    @Test
    @DisplayName("Doit lister tous les paramètres")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void getAll_nominal_returnsList() throws Exception {
        // given
        given(parametrageService.getAll()).willReturn(List.of(response));

        // when & then
        mockMvc.perform(get("/api/parametrage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].cle").value("TEST_KEY"));
    }

    @Test
    @DisplayName("Doit mettre à jour un paramètre")
    @WithMockUser(authorities = "ADMINISTRATEUR")
    void update_existingKey_returnsUpdatedParam() throws Exception {
        // given
        ParametrageUpdateRequest request = new ParametrageUpdateRequest();
        request.setValeur("NEW_VALUE");
        given(parametrageService.update(eq("TEST_KEY"), any(ParametrageUpdateRequest.class))).willReturn(response);

        // when & then
        mockMvc.perform(put("/api/parametrage/TEST_KEY")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cle").value("TEST_KEY"));
    }
}
