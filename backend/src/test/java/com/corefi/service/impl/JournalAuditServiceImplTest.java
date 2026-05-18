package com.corefi.service.impl;

import com.corefi.entity.JournalAudit;
import com.corefi.entity.Utilisateur;
import com.corefi.repository.JournalAuditRepository;
import com.corefi.repository.UtilisateurRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JournalAuditServiceImplTest {

    @Mock
    private JournalAuditRepository journalAuditRepository;

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private HttpServletRequest httpServletRequest;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private JournalAuditServiceImpl journalAuditService;

    private MockedStatic<SecurityContextHolder> mockedSecurityContext;
    private JournalAudit auditEntry;

    @BeforeEach
    void setUp() {
        mockedSecurityContext = mockStatic(SecurityContextHolder.class);
        mockedSecurityContext.when(SecurityContextHolder::getContext).thenReturn(securityContext);
        org.mockito.Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        org.mockito.Mockito.lenient().when(authentication.getName()).thenReturn("admin@sodica.cm");

        auditEntry = new JournalAudit();
        auditEntry.setId(1L);
        auditEntry.setAction("CREATION_FACTURE");
        auditEntry.setDateAction(LocalDateTime.now());
    }

    @AfterEach
    void tearDown() {
        mockedSecurityContext.close();
    }

    @Test
    @DisplayName("Doit enregistrer une entrée d'audit avec un utilisateur authentifié")
    void enregistrer_validEntry_savesAudit() {
        Utilisateur user = new Utilisateur();
        user.setId(1L);
        user.setEmail("admin@sodica.cm");

        given(utilisateurRepository.findByEmail("admin@sodica.cm")).willReturn(Optional.of(user));
        given(httpServletRequest.getRemoteAddr()).willReturn("192.168.1.1");
        given(journalAuditRepository.save(any(JournalAudit.class))).willReturn(auditEntry);

        journalAuditService.enregistrer("CREATION_FACTURE", "Facture", 1L, null, null, null);

        verify(journalAuditRepository).save(any(JournalAudit.class));
    }

    @Test
    @DisplayName("Doit retourner toutes les entrées d'audit paginées (sans filtre)")
    void findAll_noFilter_returnsPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JournalAudit> page = new PageImpl<>(List.of(auditEntry));

        given(journalAuditRepository.findAllByOrderByDateActionDesc(pageable)).willReturn(page);

        Page<JournalAudit> result = journalAuditService.findAll(null, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getAction()).isEqualTo("CREATION_FACTURE");
    }

    @Test
    @DisplayName("Doit retourner les entrées d'audit filtrées")
    void findAll_withFilters_returnsFilteredPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<JournalAudit> page = new PageImpl<>(List.of(auditEntry));

        given(journalAuditRepository.searchAudit("facture", "CREATION", pageable)).willReturn(page);

        Page<JournalAudit> result = journalAuditService.findAll("facture", "CREATION", pageable);

        assertThat(result.getContent()).hasSize(1);
    }
}
