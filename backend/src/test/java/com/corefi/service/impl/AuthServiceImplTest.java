package com.corefi.service.impl;

import com.corefi.dto.request.auth.LoginRequest;
import com.corefi.dto.response.auth.AuthResponse;
import com.corefi.entity.Utilisateur;
import com.corefi.exception.AccesNonAutoriseException;
import com.corefi.mapper.UtilisateurMapper;
import com.corefi.repository.UtilisateurRepository;
import com.corefi.security.JwtTokenProvider;
import com.corefi.service.interfaces.IJournalAuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UtilisateurRepository utilisateurRepository;
    @Mock
    private JwtTokenProvider jwtTokenProvider;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UtilisateurMapper utilisateurMapper;
    @Mock
    private IJournalAuditService journalAuditService;

    @InjectMocks
    private AuthServiceImpl authService;

    private Utilisateur utilisateur;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        utilisateur = new Utilisateur();
        utilisateur.setId(1L);
        utilisateur.setEmail("user@example.com");
        utilisateur.setActif(true);
        utilisateur.setTentativesConnexion(0);
        utilisateur.setRole(com.corefi.enums.Role.ADMINISTRATEUR);

        loginRequest = new LoginRequest();
        loginRequest.setEmail("user@example.com");
        loginRequest.setPassword("password");
    }

    @Test
    @DisplayName("Doit réussir la connexion avec des identifiants valides")
    void login_validCredentials_returnsAuthResponse() {
        // given
        given(utilisateurRepository.findByEmail("user@example.com")).willReturn(Optional.of(utilisateur));
        Authentication auth = mock(Authentication.class);
        given(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).willReturn(auth);
        given(jwtTokenProvider.generateToken(auth)).willReturn("jwt-token");
        given(jwtTokenProvider.generateRefreshToken("user@example.com")).willReturn("refresh-token");
        given(utilisateurMapper.toAuthResponse(any(), any(), any())).willReturn(new AuthResponse());

        // when
        AuthResponse result = authService.login(loginRequest);

        // then
        assertThat(result).isNotNull();
        assertThat(utilisateur.getTentativesConnexion()).isZero();
        then(utilisateurRepository).should().save(utilisateur);
        then(journalAuditService).should().enregistrer(any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Doit lever une exception pour des identifiants incorrects (échec authentification)")
    void login_badCredentials_incrementsAttemptsAndThrows() {
        // given
        given(utilisateurRepository.findByEmail("user@example.com")).willReturn(Optional.of(utilisateur));
        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccesNonAutoriseException.class)
                .hasMessageContaining("Identifiants incorrects");
        
        assertThat(utilisateur.getTentativesConnexion()).isEqualTo(1);
        then(utilisateurRepository).should().save(utilisateur);
    }

    @Test
    @DisplayName("Doit bloquer le compte après le nombre maximal de tentatives échouées")
    void login_tooManyAttempts_blocksAccount() {
        // given
        utilisateur.setTentativesConnexion(4);
        given(utilisateurRepository.findByEmail("user@example.com")).willReturn(Optional.of(utilisateur));
        given(authenticationManager.authenticate(any())).willThrow(new BadCredentialsException("Bad credentials"));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccesNonAutoriseException.class)
                .hasMessageContaining("Compte bloqué");

        assertThat(utilisateur.getBloqueJusqua()).isNotNull();
        then(utilisateurRepository).should().save(utilisateur);
    }

    @Test
    @DisplayName("Doit refuser la connexion si le compte est actuellement bloqué")
    void login_accountCurrentlyBlocked_throwsException() {
        // given
        utilisateur.setBloqueJusqua(LocalDateTime.now().plusMinutes(10));
        given(utilisateurRepository.findByEmail("user@example.com")).willReturn(Optional.of(utilisateur));

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccesNonAutoriseException.class)
                .hasMessageContaining("Compte bloqué");
    }

    @Test
    @DisplayName("Doit lever une exception si l'utilisateur n'existe pas")
    void login_userNotFound_throwsException() {
        // given
        given(utilisateurRepository.findByEmail("user@example.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(AccesNonAutoriseException.class)
                .hasMessageContaining("Identifiants incorrects");
    }

    @Test
    @DisplayName("Doit rafraîchir le token si le refresh token est valide")
    void refreshToken_validToken_returnsNewTokens() {
        // given
        String refreshToken = "valid-refresh-token";
        given(jwtTokenProvider.validateJwtToken(refreshToken)).willReturn(true);
        given(jwtTokenProvider.getUserNameFromJwtToken(refreshToken)).willReturn("user@example.com");
        given(utilisateurRepository.findByEmail("user@example.com")).willReturn(Optional.of(utilisateur));
        given(jwtTokenProvider.generateTokenFromEmail(any(), any())).willReturn("new-jwt-token");
        given(jwtTokenProvider.generateRefreshToken(any())).willReturn("new-refresh-token");
        given(utilisateurMapper.toAuthResponse(any(), any(), any())).willReturn(new AuthResponse());

        // when
        AuthResponse result = authService.refreshToken(refreshToken);

        // then
        assertThat(result).isNotNull();
        then(jwtTokenProvider).should().generateTokenFromEmail(any(), any());
    }
}
