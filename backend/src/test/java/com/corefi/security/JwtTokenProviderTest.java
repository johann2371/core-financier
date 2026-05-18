package com.corefi.security;

import io.jsonwebtoken.ExpiredJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() throws Exception {
        jwtTokenProvider = new JwtTokenProvider();
        // Utiliser la réflexion pour configurer les champs privés
        Field secretField = JwtTokenProvider.class.getDeclaredField("jwtSecret");
        secretField.setAccessible(true);
        secretField.set(jwtTokenProvider, "TEST_SECRET_KEY_CHANGE_IN_PROD_VERY_LONG_STRING_REQUIRED");

        Field expirationField = JwtTokenProvider.class.getDeclaredField("jwtExpirationMs");
        expirationField.setAccessible(true);
        expirationField.set(jwtTokenProvider, 3600000L);

        // Initialiser la clé
        jwtTokenProvider.init();
    }

    @Test
    @DisplayName("Doit générer un token JWT valide")
    void generateToken_validAuth_returnsToken() {
        User user = new User("admin@sodica.cm", "password", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());

        String token = jwtTokenProvider.generateToken(auth);

        assertThat(token).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("Doit extraire l'email depuis un token")
    void getUserNameFromJwtToken_validToken_returnsEmail() {
        User user = new User("admin@sodica.cm", "password", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        String token = jwtTokenProvider.generateToken(auth);

        String email = jwtTokenProvider.getUserNameFromJwtToken(token);

        assertThat(email).isEqualTo("admin@sodica.cm");
    }

    @Test
    @DisplayName("Doit valider un token JWT correct")
    void validateJwtToken_validToken_returnsTrue() {
        User user = new User("admin@sodica.cm", "password", Collections.emptyList());
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
        String token = jwtTokenProvider.generateToken(auth);

        boolean isValid = jwtTokenProvider.validateJwtToken(token);

        assertThat(isValid).isTrue();
    }

    @Test
    @DisplayName("Doit rejeter un token JWT invalide")
    void validateJwtToken_invalidToken_returnsFalse() {
        boolean isValid = jwtTokenProvider.validateJwtToken("token.invalide.ici");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Doit rejeter un token JWT vide")
    void validateJwtToken_emptyToken_returnsFalse() {
        boolean isValid = jwtTokenProvider.validateJwtToken("");

        assertThat(isValid).isFalse();
    }

    @Test
    @DisplayName("Doit générer un refresh token valide")
    void generateRefreshToken_validEmail_returnsToken() {
        String refreshToken = jwtTokenProvider.generateRefreshToken("admin@sodica.cm");

        assertThat(refreshToken).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateJwtToken(refreshToken)).isTrue();
        assertThat(jwtTokenProvider.getUserNameFromJwtToken(refreshToken)).isEqualTo("admin@sodica.cm");
    }

    @Test
    @DisplayName("Doit générer un token depuis un email et un rôle")
    void generateTokenFromEmail_validParams_returnsToken() {
        String token = jwtTokenProvider.generateTokenFromEmail("comptable@sodica.cm", "COMPTABLE");

        assertThat(token).isNotNull().isNotEmpty();
        assertThat(jwtTokenProvider.validateJwtToken(token)).isTrue();
        assertThat(jwtTokenProvider.getUserNameFromJwtToken(token)).isEqualTo("comptable@sodica.cm");
    }
}
