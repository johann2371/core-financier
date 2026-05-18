package com.corefi.controller;

import com.corefi.dto.response.ocr.FactureExtractionResult;
import com.corefi.service.interfaces.IOcrService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OcrController.class)
@AutoConfigureMockMvc(addFilters = false)
class OcrControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IOcrService ocrService;

    @MockBean
    private com.corefi.security.JwtTokenProvider jwtTokenProvider;

    @MockBean
    private com.corefi.security.UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("Doit extraire les données d'une facture via OCR Mindee")
    @WithMockUser(authorities = "COMPTABLE")
    void extraireFacture_validFile_returnsExtractionResult() throws Exception {
        FactureExtractionResult result = FactureExtractionResult.builder()
                .numeroFacture("FAC-2024-100")
                .nomFournisseur("Fournisseur ABC")
                .montantTtc(BigDecimal.valueOf(150000))
                .build();

        given(ocrService.extraireDonnees(any())).willReturn(result);

        MockMultipartFile file = new MockMultipartFile(
                "file", "facture.pdf", "application/pdf", "contenu PDF".getBytes());

        mockMvc.perform(multipart("/api/ocr/factures")
                .file(file)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroFacture").value("FAC-2024-100"))
                .andExpect(jsonPath("$.nomFournisseur").value("Fournisseur ABC"))
                .andExpect(jsonPath("$.montantTtc").value(150000));
    }

    @Test
    @DisplayName("Doit extraire les données d'une facture image via OCR")
    @WithMockUser(authorities = "RESPONSABLE_FINANCIER")
    void extraireFacture_imageFile_returnsExtractionResult() throws Exception {
        FactureExtractionResult result = FactureExtractionResult.builder()
                .numeroFacture("FAC-IMG-001")
                .nomFournisseur("SODICA")
                .nomClient("Client XYZ")
                .montantTtc(BigDecimal.valueOf(200000))
                .build();

        given(ocrService.extraireDonnees(any())).willReturn(result);

        MockMultipartFile imageFile = new MockMultipartFile(
                "file", "facture.jpg", "image/jpeg", "image content".getBytes());

        mockMvc.perform(multipart("/api/ocr/factures")
                .file(imageFile)
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomFournisseur").value("SODICA"))
                .andExpect(jsonPath("$.nomClient").value("Client XYZ"));
    }
}
