package com.corefi.service.impl;

import com.corefi.dto.request.utilisateur.ProfileUpdateRequest;
import com.corefi.dto.request.utilisateur.UtilisateurCreateRequest;
import com.corefi.dto.response.utilisateur.UtilisateurResponse;
import com.corefi.entity.Utilisateur;
import com.corefi.enums.Role;
import com.corefi.exception.ResourceNotFoundException;
import com.corefi.mapper.UtilisateurMapper;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.service.interfaces.IJournalAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UtilisateurServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UtilisateurMapper utilisateurMapper;

    @Mock
    private IJournalAuditService journalAuditService;

    @InjectMocks
    private UtilisateurServiceImpl utilisateurService;

    private Utilisateur utilisateur;
    private UtilisateurResponse response;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setNom("Gouaffo");
        utilisateur.setPrenom("Johann");
        utilisateur.setEmail("admin@sodica.cm");
        utilisateur.setRole(Role.ADMINISTRATEUR);
        utilisateur.setActif(true);

        response = new UtilisateurResponse();
        response.setId(1L);
        response.setNom("Gouaffo");
        response.setPrenom("Johann");
        response.setEmail("admin@sodica.cm");
    }

    @Test
    @DisplayName("Doit retourner la liste de tous les utilisateurs")
    void findAll_nominal_returnsList() {
        given(utilisateurRepository.findAll()).willReturn(List.of(utilisateur));
        given(utilisateurMapper.toResponse(utilisateur)).willReturn(response);

        List<UtilisateurResponse> result = utilisateurService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmail()).isEqualTo("admin@sodica.cm");
    }

    @Test
    @DisplayName("Doit retourner un utilisateur par ID")
    void findById_existingId_returnsUtilisateur() {
        given(utilisateurRepository.findById(1L)).willReturn(Optional.of(utilisateur));
        given(utilisateurMapper.toResponse(utilisateur)).willReturn(response);

        UtilisateurResponse result = utilisateurService.findById(1L);

        assertThat(result.getNom()).isEqualTo("Gouaffo");
    }

    @Test
    @DisplayName("Doit lever une exception si l'utilisateur n'existe pas")
    void findById_nonExistingId_throwsException() {
        given(utilisateurRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> utilisateurService.findById(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Doit désactiver un utilisateur")
    void desactiver_existingId_setsInactif() {
        given(utilisateurRepository.findById(1L)).willReturn(Optional.of(utilisateur));

        utilisateurService.desactiver(1L);

        assertThat(utilisateur.isActif()).isFalse();
        verify(utilisateurRepository).save(utilisateur);
    }

    @Test
    @DisplayName("Doit réactiver un utilisateur")
    void reactiver_existingId_setsActif() {
        utilisateur.setActif(false);
        given(utilisateurRepository.findById(1L)).willReturn(Optional.of(utilisateur));

        utilisateurService.reactiver(1L);

        assertThat(utilisateur.isActif()).isTrue();
        verify(utilisateurRepository).save(utilisateur);
    }

    @Test
    @DisplayName("Doit mettre à jour le profil d'un utilisateur")
    void updateProfile_validRequest_updatesProfile() {
        ProfileUpdateRequest profileRequest = new ProfileUpdateRequest();
        profileRequest.setNom("Gouaffo Modifié");
        profileRequest.setPrenom("Johann");
        profileRequest.setEmail("admin@sodica.cm");

        given(utilisateurRepository.findByEmail("admin@sodica.cm")).willReturn(Optional.of(utilisateur));
        given(utilisateurRepository.save(any(Utilisateur.class))).willReturn(utilisateur);
        given(utilisateurMapper.toResponse(any(Utilisateur.class))).willReturn(response);

        UtilisateurResponse result = utilisateurService.updateProfile("admin@sodica.cm", profileRequest);

        assertThat(result).isNotNull();
        verify(utilisateurRepository).save(any(Utilisateur.class));
    }
}
