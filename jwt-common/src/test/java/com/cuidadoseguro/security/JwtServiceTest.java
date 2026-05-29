package com.cuidadoseguro.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private String secret;
    private String validToken;
    private String expiredToken;

    @BeforeEach
    void setUp() {
        secret = "mySecretKeyForTestingPurposesThatIsLongEnough";
        jwtService = new JwtService(secret);
        
        // Create a valid token
        validToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000)) // 1 hour
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
        
        // Create an expired token
        expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date(System.currentTimeMillis() - 7200000)) // 2 hours ago
                .setExpiration(new Date(System.currentTimeMillis() - 3600000)) // 1 hour ago
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();
    }

    @Test
    void testExtractAllClaimsSuccess() {
        // When
        Claims claims = jwtService.extractAllClaims(validToken);

        // Then
        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
    }

    @Test
    void testExtractAllClaimsInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> jwtService.extractAllClaims(invalidToken));
    }

    @Test
    void testExtractUsernameSuccess() {
        // When
        String username = jwtService.extractUsername(validToken);

        // Then
        assertEquals("testuser", username);
    }

    @Test
    void testExtractUsernameInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When & Then
        assertThrows(Exception.class, () -> jwtService.extractUsername(invalidToken));
    }

    @Test
    void testIsTokenValidValidToken() {
        // When
        boolean isValid = jwtService.isTokenValid(validToken);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testIsTokenValidExpiredToken() {
        // When
        boolean isValid = jwtService.isTokenValid(expiredToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsTokenValidInvalidToken() {
        // Given
        String invalidToken = "invalid.token.here";

        // When
        boolean isValid = jwtService.isTokenValid(invalidToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsTokenValidMalformedToken() {
        // Given
        String malformedToken = "not.a.valid.jwt";

        // When
        boolean isValid = jwtService.isTokenValid(malformedToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsTokenValidEmptyToken() {
        // Given
        String emptyToken = "";

        // When
        boolean isValid = jwtService.isTokenValid(emptyToken);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testIsTokenValidNullToken() {
        // When
        boolean isValid = jwtService.isTokenValid(null);

        // Then
        assertFalse(isValid);
    }

    @Test
    void testExtractAllClaimsWithCustomClaims() {
        // Given
        String tokenWithClaims = Jwts.builder()
                .setSubject("testuser")
                .claim("userId", 123)
                .claim("role", "ADMIN")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()))
                .compact();

        // When
        Claims claims = jwtService.extractAllClaims(tokenWithClaims);

        // Then
        assertNotNull(claims);
        assertEquals("testuser", claims.getSubject());
        assertEquals(123, claims.get("userId"));
        assertEquals("ADMIN", claims.get("role"));
    }
}
