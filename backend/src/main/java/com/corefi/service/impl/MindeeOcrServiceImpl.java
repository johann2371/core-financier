package com.corefi.service.impl;

import com.corefi.dto.response.ocr.FactureExtractionResult;
import com.corefi.exception.WorkflowException;
import com.corefi.service.interfaces.IOcrService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class MindeeOcrServiceImpl implements IOcrService {

    @Value("${mindee.api.key}")
    private String mindeeApiKey;

    private static final String MINDEE_INVOICE_URL = "https://api.mindee.net/v1/products/mindee/invoices/v4/predict";

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Override
    public FactureExtractionResult extraireDonnees(MultipartFile fichier) {
        if (mindeeApiKey == null || mindeeApiKey.isBlank() || mindeeApiKey.contains("votre_cle")) {
            log.warn("La clé API Mindee n'est pas configurée correctement. Renvoi de données factices pour le test.");
            // Fallback mock pour faciliter le test sans clé API valide
            return FactureExtractionResult.builder()
                    .nomFournisseur("Fournisseur MOCK (Pas de clé API)")
                    .montantTtc(new BigDecimal("150000"))
                    .dateFacture(LocalDate.now())
                    .numeroFacture("FAC-MOCK-123")
                    .build();
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);
            headers.set("Authorization", "Token " + mindeeApiKey);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // On wrappe le byte array dans un ByteArrayResource nommé pour que RestTemplate l'envoie comme fichier
            ByteArrayResource fileAsResource = new ByteArrayResource(fichier.getBytes()) {
                @Override
                public String getFilename() {
                    return fichier.getOriginalFilename() != null ? fichier.getOriginalFilename() : "facture.pdf";
                }
            };
            body.add("document", fileAsResource);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.info("Appel à l'API Mindee pour le fichier: {}", fileAsResource.getFilename());
            ResponseEntity<String> response = restTemplate.postForEntity(MINDEE_INVOICE_URL, requestEntity, String.class);

            return parseResponse(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("Erreur API Mindee : {}", e.getResponseBodyAsString(), e);
            throw new WorkflowException("Erreur API Mindee (" + e.getStatusCode() + ") : " + e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Erreur lors du traitement OCR", e);
            throw new WorkflowException("Erreur de traitement (Parsing) : " + e.getMessage());
        }
    }

    private FactureExtractionResult parseResponse(String jsonResponse) throws Exception {
        JsonNode root = objectMapper.readTree(jsonResponse);
        JsonNode prediction = root.path("document").path("inference").path("prediction");

        String fournisseur = extractString(prediction, "supplier_name");
        String client = extractString(prediction, "customer_name");
        String numero = extractString(prediction, "invoice_number");
        BigDecimal totalTtc = extractBigDecimal(prediction, "total_amount");
        BigDecimal totalNet = extractBigDecimal(prediction, "total_net");
        BigDecimal totalTax = extractBigDecimal(prediction, "total_tax");
        LocalDate date = extractDate(prediction, "date");

        return FactureExtractionResult.builder()
                .nomFournisseur(fournisseur)
                .nomClient(client)
                .numeroFacture(numero)
                .montantTtc(totalTtc)
                .montantHt(totalNet)
                .tva(totalTax)
                .dateFacture(date)
                .build();
    }

    private String extractString(JsonNode prediction, String fieldName) {
        JsonNode node = prediction.path(fieldName).path("value");
        return node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    private BigDecimal extractBigDecimal(JsonNode prediction, String fieldName) {
        JsonNode node = prediction.path(fieldName).path("value");
        return node.isMissingNode() || node.isNull() ? null : new BigDecimal(node.asText());
    }

    private LocalDate extractDate(JsonNode prediction, String fieldName) {
        JsonNode node = prediction.path(fieldName).path("value");
        if (node.isMissingNode() || node.isNull()) return null;
        try {
            return LocalDate.parse(node.asText());
        } catch (Exception e) {
            return null;
        }
    }
}
