package com.corefi.service.impl;

import com.corefi.entity.Facture;
import com.corefi.entity.Parametrage;
import com.corefi.entity.Tiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PdfServiceImplTest {

    @Mock
    private FactureRepository factureRepository;
    @Mock
    private EncaissementRepository encaissementRepository;
    @Mock
    private DecaissementRepository decaissementRepository;
    @Mock
    private ParametrageRepository parametrageRepository;
    @Mock
    private SessionCaisseRepository sessionCaisseRepository;
    @Mock
    private TiersRepository tiersRepository;

    @InjectMocks
    private PdfServiceImpl pdfService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pdfService, "uploadDir", "uploads");
    }

    @Test
    @DisplayName("Doit générer un PDF de facture avec succès")
    void genererFacturePdf_nominal_returnsByteArray() {
        // given
        Facture facture = new Facture();
        facture.setId(1L);
        facture.setNumero("FAC-001");
        facture.setMontantHt(BigDecimal.valueOf(1000));
        facture.setMontantTtc(BigDecimal.valueOf(1192.5));
        facture.setLignes(new ArrayList<>());
        
        Tiers client = new Tiers();
        client.setRaisonSociale("Client Test");
        facture.setTiers(client);

        given(factureRepository.findByIdWithDetails(1L)).willReturn(Optional.of(facture));
        given(parametrageRepository.findByCle("INFO_SOCIETE_NOM")).willReturn(Optional.empty());
        given(parametrageRepository.findByCle("INVOICE_LOGO_URL")).willReturn(Optional.empty());

        // when
        byte[] pdfContent = pdfService.genererFacturePdf(1L);

        // then
        assertThat(pdfContent).isNotEmpty();
    }

    @Test
    @DisplayName("Doit lever une exception si la facture n'existe pas")
    void genererFacturePdf_inexistentFacture_throwsResourceNotFoundException() {
        // given
        given(factureRepository.findByIdWithDetails(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> pdfService.genererFacturePdf(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Facture introuvable");
    }

    @Test
    @DisplayName("Doit utiliser le nom de la société configuré dans le PDF")
    void getNomSociete_configured_returnsValue() {
        // given
        Facture facture = new Facture();
        facture.setId(1L);
        facture.setLignes(new ArrayList<>());
        given(factureRepository.findByIdWithDetails(1L)).willReturn(Optional.of(facture));
        
        Parametrage p = new Parametrage();
        p.setValeur("Ma Société");
        given(parametrageRepository.findByCle("INFO_SOCIETE_NOM")).willReturn(Optional.of(p));
        given(parametrageRepository.findByCle("INVOICE_LOGO_URL")).willReturn(Optional.empty());

        // when
        byte[] pdfContent = pdfService.genererFacturePdf(1L);

        // then
        assertThat(pdfContent).isNotEmpty();
    }
}
