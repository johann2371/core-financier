package com.corefi.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RestController
class ExceptionTestController {
    @GetMapping("/test-exception/not-found")
    public void throwNotFound() {
        throw new ResourceNotFoundException("Ressource non trouvée");
    }

    @GetMapping("/test-exception/workflow")
    public void throwWorkflow() {
        throw new WorkflowException("Erreur de workflow");
    }

    @GetMapping("/test-exception/solde")
    public void throwSolde() {
        throw new SoldeInsuffisantException("Solde insuffisant");
    }

    @GetMapping("/test-exception/generic")
    public void throwGeneric() {
        throw new RuntimeException("Erreur inattendue");
    }
}

@WebMvcTest(controllers = ExceptionTestController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.corefi.security.JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Test
    @DisplayName("Doit retourner 404 pour ResourceNotFoundException")
    void handleNotFound_returns404() throws Exception {
        mockMvc.perform(get("/test-exception/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Ressource non trouvée"));
    }

    @Test
    @DisplayName("Doit retourner 400 pour WorkflowException")
    void handleWorkflow_returns400() throws Exception {
        mockMvc.perform(get("/test-exception/workflow"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Erreur de workflow"));
    }

    @Test
    @DisplayName("Doit retourner 400 pour SoldeInsuffisantException")
    void handleSolde_returns400() throws Exception {
        mockMvc.perform(get("/test-exception/solde"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Solde insuffisant"));
    }

    @Test
    @DisplayName("Doit retourner 500 pour les autres exceptions")
    void handleGeneric_returns500() throws Exception {
        mockMvc.perform(get("/test-exception/generic"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Erreur interne : Erreur inattendue"));
    }
}
