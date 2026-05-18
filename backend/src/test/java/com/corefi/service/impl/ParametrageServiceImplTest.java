package com.corefi.service.impl;

import com.corefi.dto.request.parametrage.ParametrageUpdateRequest;
import com.corefi.dto.response.parametrage.ParametrageResponse;
import com.corefi.entity.Parametrage;
import com.corefi.repository.ParametrageRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class ParametrageServiceImplTest {

    @Mock
    private ParametrageRepository parametrageRepository;

    @Mock
    private IJournalAuditService journalAuditService;

    @InjectMocks
    private ParametrageServiceImpl parametrageService;

    private Parametrage parametrage;
    private ParametrageUpdateRequest updateRequest;

    @BeforeEach
    void setUp() {
        parametrage = new Parametrage(1L, "APP_NAME", "CoreFinancier", "Nom de l'application", null);
        updateRequest = new ParametrageUpdateRequest();
        updateRequest.setValeur("NewName");
        updateRequest.setDescription("New Description");
    }

    @Test
    @DisplayName("Doit retourner tous les paramètres")
    void getAll_nominal_returnsList() {
        // given
        given(parametrageRepository.findAll()).willReturn(List.of(parametrage));

        // when
        List<ParametrageResponse> results = parametrageService.getAll();

        // then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCle()).isEqualTo("APP_NAME");
        then(parametrageRepository).should().findAll();
    }

    @Test
    @DisplayName("Doit retourner une liste vide si aucun paramètre n'existe")
    void getAll_empty_returnsEmptyList() {
        // given
        given(parametrageRepository.findAll()).willReturn(Collections.emptyList());

        // when
        List<ParametrageResponse> results = parametrageService.getAll();

        // then
        assertThat(results).isEmpty();
    }

    @Test
    @DisplayName("Doit mettre à jour un paramètre existant")
    void update_existingCle_updatesAndReturnsResponse() {
        // given
        given(parametrageRepository.findByCle("APP_NAME")).willReturn(Optional.of(parametrage));
        given(parametrageRepository.save(any(Parametrage.class))).willReturn(parametrage);

        // when
        ParametrageResponse result = parametrageService.update("APP_NAME", updateRequest);

        // then
        assertThat(result.getValeur()).isEqualTo("NewName");
        then(parametrageRepository).should().save(any(Parametrage.class));
        then(journalAuditService).should().enregistrer(eq("UPDATE"), eq("Parametrage"), eq(1L), anyString(), anyString(), eq(null));
    }

    @Test
    @DisplayName("Doit créer un nouveau paramètre s'il n'existe pas lors de l'update")
    void update_nonExistentCle_createsAndReturnsResponse() {
        // given
        given(parametrageRepository.findByCle("NEW_KEY")).willReturn(Optional.empty());
        Parametrage newParam = new Parametrage(2L, "NEW_KEY", "NewValue", "Desc", null);
        given(parametrageRepository.save(any(Parametrage.class))).willReturn(newParam);

        ParametrageUpdateRequest req = new ParametrageUpdateRequest();
        req.setValeur("NewValue");

        // when
        ParametrageResponse result = parametrageService.update("NEW_KEY", req);

        // then
        assertThat(result.getCle()).isEqualTo("NEW_KEY");
        then(parametrageRepository).should().save(any(Parametrage.class));
    }

    @Test
    @DisplayName("Doit formater l'URL si la clé contient LOGO_URL et est un chemin relatif")
    void toResponse_logoUrlRelativePath_formatsUrl() {
        // given
        Parametrage logoParam = new Parametrage(3L, "INVOICE_LOGO_URL", "logos/test.png", "Logo", null);
        given(parametrageRepository.findAll()).willReturn(List.of(logoParam));

        // when
        List<ParametrageResponse> results = parametrageService.getAll();

        // then
        assertThat(results.get(0).getValeur()).isEqualTo("/api/uploads/logos/test.png");
    }

    @Test
    @DisplayName("Ne doit pas formater l'URL si elle commence par http")
    void toResponse_logoUrlHttp_doesNotFormat() {
        // given
        Parametrage logoParam = new Parametrage(3L, "INVOICE_LOGO_URL", "http://example.com/logo.png", "Logo", null);
        given(parametrageRepository.findAll()).willReturn(List.of(logoParam));

        // when
        List<ParametrageResponse> results = parametrageService.getAll();

        // then
        assertThat(results.get(0).getValeur()).isEqualTo("http://example.com/logo.png");
    }

    private String anyString() {
        return any(String.class);
    }
}
