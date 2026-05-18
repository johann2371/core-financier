package com.corefi.service.impl;

import com.corefi.dto.request.facture.FactureCreateRequest;
import com.corefi.dto.response.facture.FactureResponse;
import com.corefi.entity.*;
import com.corefi.enums.StatutFacture;
import com.corefi.enums.TypeTiers;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.exception.WorkflowException;
import com.corefi.mapper.FactureMapper;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class FactureServiceImplTest {

    @Mock
    private FactureRepository factureRepository;
    @Mock
    private TiersRepository tiersRepository;
    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private DeviseRepository deviseRepository;
    @Mock
    private AffectationPaiementRepository affectationPaiementRepository;
    @Mock
    private FactureMapper factureMapper;
    @Mock
    private IJournalAuditService journalAuditService;
    @Mock
    private INotificationService notificationService;

    @Mock
    private SecurityContext securityContext;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private FactureServiceImpl factureService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContext;
    private Tiers client;
    private Utilisateur user;
    private FactureCreateRequest createRequest;

    @BeforeEach
    void setUp() {
        mockedSecurityContext = mockStatic(SecurityContextHolder.class);
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("test@test.com");

        client = new Tiers();
        client.setId(1L);
        client.setRaisonSociale("Client Test");
        client.setType(TypeTiers.CLIENT);
        client.setActif(true);
        client.setSolde(BigDecimal.ZERO);
        client.setTotalDette(BigDecimal.ZERO);

        user = new Utilisateur();
        user.setEmail("test@test.com");

        createRequest = new FactureCreateRequest();
        createRequest.setTiersId(1L);
        createRequest.setType("VENTE");
        createRequest.setLignes(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContext.close();
    }

    @Test
    @DisplayName("Doit créer une facture avec succès (cas nominal)")
    void creer_nominal_success() {
        // given
        given(tiersRepository.findById(1L)).willReturn(Optional.of(client));
        given(utilisateurRepository.findByEmail("test@test.com")).willReturn(Optional.of(user));
        given(deviseRepository.findByCode("XAF")).willReturn(Optional.of(new Devise()));
        
        Facture facture = new Facture();
        facture.setId(10L);
        facture.setMontantTtc(BigDecimal.valueOf(1000));
        given(factureMapper.toEntity(any(), any())).willReturn(facture);
        given(factureRepository.save(any())).willReturn(facture);
        given(factureMapper.toResponse(any(), any())).willReturn(new FactureResponse());

        // when
        FactureResponse result = factureService.creer(createRequest);

        // then
        assertThat(result).isNotNull();
        then(factureRepository).should().save(any(Facture.class));
        then(tiersRepository).should().save(client);
        assertThat(client.getSolde()).isEqualByComparingTo("1000");
    }

    @Test
    @DisplayName("Doit lever une exception si le tiers n'existe pas")
    void creer_tiersInexistant_throwsResourceNotFoundException() {
        // given
        given(tiersRepository.findById(1L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> factureService.creer(createRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Tiers introuvable");
    }

    @Test
    @DisplayName("Doit lever une exception si le tiers est inactif")
    void creer_tiersInactif_throwsWorkflowException() {
        // given
        client.setActif(false);
        given(tiersRepository.findById(1L)).willReturn(Optional.of(client));

        // when & then
        assertThatThrownBy(() -> factureService.creer(createRequest))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("tiers désactivé");
    }

    @Test
    @DisplayName("Doit trouver une facture par son ID")
    void findById_nominal_returnsResponse() {
        // given
        Facture f = new Facture();
        f.setId(1L);
        f.setMontantTtc(BigDecimal.valueOf(500));
        given(factureRepository.findById(1L)).willReturn(Optional.of(f));
        given(affectationPaiementRepository.findByFactureId(1L)).willReturn(List.of());
        given(factureMapper.toResponse(eq(f), any())).willReturn(new FactureResponse());

        // when
        FactureResponse result = factureService.findById(1L);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("Doit annuler une facture valide")
    void annuler_factureValidee_updatesStatusToAnnulee() {
        // given
        Facture f = new Facture();
        f.setId(1L);
        f.setStatut(StatutFacture.EN_ATTENTE_PAIEMENT);
        f.setTiers(client);
        f.setMontantTtc(BigDecimal.valueOf(1000));
        client.setSolde(BigDecimal.valueOf(1000));

        given(factureRepository.findById(1L)).willReturn(Optional.of(f));
        given(factureRepository.save(any())).willReturn(f);
        given(factureMapper.toResponse(any(), any())).willReturn(new FactureResponse());

        // when
        factureService.annuler(1L);

        // then
        assertThat(f.getStatut()).isEqualTo(StatutFacture.ANNULEE);
        assertThat(client.getSolde()).isEqualByComparingTo("0");
        then(factureRepository).should().save(f);
    }

    @Test
    @DisplayName("Doit lever une exception si on annule une facture déjà soldée")
    void annuler_factureSoldee_throwsWorkflowException() {
        // given
        Facture f = new Facture();
        f.setStatut(StatutFacture.SOLDEE);
        given(factureRepository.findById(1L)).willReturn(Optional.of(f));

        // when & then
        assertThatThrownBy(() -> factureService.annuler(1L))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("déjà soldée");
    }

    @Test
    @DisplayName("Doit lever une exception si on annule une facture déjà annulée")
    void annuler_factureDejaAnnulee_throwsWorkflowException() {
        // given
        Facture f = new Facture();
        f.setStatut(StatutFacture.ANNULEE);
        f.setNumero("FAC-001");
        given(factureRepository.findById(1L)).willReturn(Optional.of(f));

        // when & then
        assertThatThrownBy(() -> factureService.annuler(1L))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("déjà annulée");
    }

    @Test
    @DisplayName("Doit lever une exception si le type de facture ne correspond pas au type de tiers")
    void creer_typeMismatch_throwsWorkflowException() {
        // given
        client.setType(TypeTiers.FOURNISSEUR);
        createRequest.setType("VENTE");
        given(tiersRepository.findById(1L)).willReturn(Optional.of(client));

        // when & then
        assertThatThrownBy(() -> factureService.creer(createRequest))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("doit être associée à un CLIENT");
    }

    @Test
    @DisplayName("Doit lever une exception si on valide une facture qui n'est pas en BROUILLON")
    void valider_notBrouillon_throwsWorkflowException() {
        // given
        Facture f = new Facture();
        f.setStatut(StatutFacture.VALIDEE); // Statut existant dans l'énum? 
        // Note: StatutFacture.VALIDEE semble être EN_ATTENTE_PAIEMENT dans le code
        f.setStatut(StatutFacture.EN_ATTENTE_PAIEMENT);
        given(factureRepository.findById(1L)).willReturn(Optional.of(f));

        // when & then
        assertThatThrownBy(() -> factureService.valider(1L))
                .isInstanceOf(WorkflowException.class)
                .hasMessageContaining("statut BROUILLON");
    }
}
