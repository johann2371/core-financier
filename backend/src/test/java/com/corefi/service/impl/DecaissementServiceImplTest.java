package com.corefi.service.impl;

import com.corefi.dto.request.decaissement.ExecutionCaissierRequest;
import com.corefi.entity.*;
import com.corefi.enums.MoyenPaiement;
import com.corefi.enums.StatutDecaissement;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.repository.*;
import com.corefi.service.interfaces.IJournalAuditService;
import com.corefi.service.interfaces.INotificationService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class DecaissementServiceImplTest {

    @Mock
    private DecaissementRepository decaissementRepository;
    @Mock
    private TiersRepository tiersRepository;
    @Mock
    private CompteFinancierRepository compteFinancierRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private IJournalAuditService journalAuditService;
    @Mock
    private INotificationService notificationService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private DecaissementServiceImpl decaissementService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContext;
    private Decaissement decaissement;
    private CompteFinancier compte;

    @BeforeEach
    void setUp() {
        mockedSecurityContext = mockStatic(SecurityContextHolder.class);
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("test@test.com");

        decaissement = new Decaissement();
        decaissement.setId(1L);
        decaissement.setMontant(BigDecimal.valueOf(5000));
        decaissement.setStatut(StatutDecaissement.VALIDEE_RF);

        compte = new CompteFinancier();
        compte.setId(1L);
        compte.setNumero("COMPTE-001");
        compte.setSolde(BigDecimal.valueOf(1000)); // Solde insuffisant
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContext.close();
    }

    @Test
    @DisplayName("Doit lever une exception si le solde du compte est insuffisant")
    void executer_insufficientBalance_throwsWorkflowException() {
        // given
        ExecutionCaissierRequest request = new ExecutionCaissierRequest();
        request.setCompteFinancierId(1L);
        request.setMoyenPaiement("ESPECES");

        given(decaissementRepository.findById(1L)).willReturn(Optional.of(decaissement));
        given(compteFinancierRepository.findById(1L)).willReturn(Optional.of(compte));

        // when & then
        assertThatThrownBy(() -> decaissementService.executer(1L, request))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Solde insuffisant");
    }

    @Test
    @DisplayName("Doit lever une exception si le décaissement n'existe pas")
    void executer_nonExistentDecaissement_throwsResourceNotFoundException() {
        // given
        given(decaissementRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> decaissementService.executer(1L, new ExecutionCaissierRequest()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Décaissement introuvable");
    }

    @Test
    @DisplayName("Doit lever une exception si le moyen de paiement est invalide")
    void executer_invalidMoyenPaiement_throwsWorkflowException() {
        // given
        ExecutionCaissierRequest request = new ExecutionCaissierRequest();
        request.setCompteFinancierId(1L);
        request.setMoyenPaiement("INVALID_MOYEN");

        compte.setSolde(BigDecimal.valueOf(10000)); // Solde ok
        given(decaissementRepository.findById(1L)).willReturn(Optional.of(decaissement));
        given(compteFinancierRepository.findById(1L)).willReturn(Optional.of(compte));

        // when & then
        assertThatThrownBy(() -> decaissementService.executer(1L, request))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("Moyen de paiement invalide");
    }
}
