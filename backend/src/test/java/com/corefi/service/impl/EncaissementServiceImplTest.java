package com.corefi.service.impl;

import com.corefi.dto.request.encaissement.AffectationRequest;
import com.corefi.entity.*;
import com.corefi.enums.StatutFacture;
import com.corefi.enums.StatutEncaissement;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.service.interfaces.INotificationService;
import com.corefi.mapper.EncaissementMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class EncaissementServiceImplTest {

    @Mock
    private EncaissementRepository encaissementRepository;
    @Mock
    private TiersRepository tiersRepository;
    @Mock
    private FactureRepository factureRepository;
    @Mock
    private CompteFinancierRepository compteFinancierRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private AffectationPaiementRepository affectationPaiementRepository;
    @Mock
    private EncaissementMapper encaissementMapper;
    @Mock
    private IJournalAuditService journalAuditService;
    @Mock
    private INotificationService notificationService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private EncaissementServiceImpl encaissementService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContext;
    private Encaissement encaissement;
    private Tiers client;

    @BeforeEach
    void setUp() {
        mockedSecurityContext = mockStatic(SecurityContextHolder.class);
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("test@test.com");

        client = new Tiers();
        client.setId(1L);
        client.setRaisonSociale("Client Test");

        encaissement = new Encaissement();
        encaissement.setId(1L);
        encaissement.setMontant(BigDecimal.valueOf(10000));
        encaissement.setStatut(StatutEncaissement.VALIDEE);
        encaissement.setClient(client);
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContext.close();
    }

    @Test
    @DisplayName("Doit lever une exception si la facture n'appartient pas au client")
    void affecter_clientMismatch_throwsWorkflowException() {
        // given
        Facture facture = new Facture();
        facture.setId(10L);
        facture.setNumero("FAC-10");
        Tiers autreClient = new Tiers();
        autreClient.setId(2L);
        facture.setTiers(autreClient);

        AffectationRequest request = new AffectationRequest();
        request.setFactureId(10L);
        request.setMontantAffecte(BigDecimal.valueOf(5000));

        given(encaissementRepository.findById(1L)).willReturn(Optional.of(encaissement));
        given(factureRepository.findById(10L)).willReturn(Optional.of(facture));

        // when & then
        assertThatThrownBy(() -> encaissementService.affecter(1L, List.of(request)))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("n'appartient pas au client");
    }

    @Test
    @DisplayName("Doit lever une exception si le montant affecté dépasse le montant de l'encaissement")
    void affecter_amountExceeded_throwsWorkflowException() {
        // given
        Facture facture = new Facture();
        facture.setId(10L);
        facture.setTiers(client);
        facture.setStatut(StatutFacture.EN_ATTENTE_PAIEMENT);
        facture.setMontantTtc(BigDecimal.valueOf(20000));

        AffectationRequest request = new AffectationRequest();
        request.setFactureId(10L);
        request.setMontantAffecte(BigDecimal.valueOf(15000)); // > 10000 (montant encaissement)

        given(encaissementRepository.findById(1L)).willReturn(Optional.of(encaissement));
        given(factureRepository.findById(10L)).willReturn(Optional.of(facture));
        given(utilisateurRepository.findByEmail("test@test.com")).willReturn(Optional.of(new Utilisateur()));

        // when & then
        assertThatThrownBy(() -> encaissementService.affecter(1L, List.of(request)))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("dépasse le montant de l'encaissement");
    }
}
