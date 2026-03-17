package com.corefi.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret:DEV_SECRET_KEY_CHANGE_IN_PROD_VERY_LONG_STRING_REQUIRED}")
    private String jwtSecret;

    @Value("${jwt.expiration:3600000}") // 1 heure
    private long jwtExpirationMs;

    // Refresh token : 24 heures
    private static final long REFRESH_EXPIRATION_MS = 86400000L;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes());
    }

    // ── Générer un token depuis un objet Authentication (après login Spring Security) ──
    public String generateToken(Authentication authentication) {
        User userPrincipal = (User) authentication.getPrincipal();
        return buildToken(userPrincipal.getUsername(), jwtExpirationMs);
    }

    // ── Générer un token depuis email + rôle (utilisé pour le refresh) ──
    public String generateTokenFromEmail(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // ── Générer un refresh token (longue durée) ──
    public String generateRefreshToken(String email) {
        return buildToken(email, REFRESH_EXPIRATION_MS);
    }

    // ── Extraire l'email depuis un token ──
    public String getUserNameFromJwtToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    // ── Valider un token ──
    public boolean validateJwtToken(String authToken) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(authToken);
            return true;
        } catch (ExpiredJwtException e) {
            // Token expiré
        } catch (JwtException | IllegalArgumentException e) {
            // Token invalide
        }
        return false;
    }

    // ── Méthode privée : construction d'un token ──
    private String buildToken(String subject, long expirationMs) {
        return Jwts.builder()
                .setSubject(subject)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}
