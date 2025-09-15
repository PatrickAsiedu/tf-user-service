package app.tradeflows.api.user_service;

import app.tradeflows.api.user_service.configurations.JwtProperties;
import app.tradeflows.api.user_service.services.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JwtServiceTests {

    @InjectMocks
    private JwtService jwtService;

    private SecretKey secretKey;

    @BeforeEach
    void setUp() {
        // Mock JwtProperties
        JwtProperties jwtProperties = mock(JwtProperties.class);
        when(jwtProperties.getExpirationTime()).thenReturn(100000L);
        when(jwtProperties.getSecretKey()).thenReturn("app.tradeflows.api.user_service-test");

        // Initialize JwtService
        jwtService = new JwtService(jwtProperties);

        // Prepare secret key
        byte[] keyBytes = jwtProperties.getSecretKey().getBytes(StandardCharsets.UTF_8);
        secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    @Test
    void testExtractUsername() {
        // Given
        String token = Jwts.builder()
                .subject("username")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        // When
        String username = jwtService.extractUsername(token);

        // Then
        assertEquals("username", username);
    }

    @Test
    void testGenerateToken() {
        // Given
        UserDetails userDetails = User.withUsername("username").password("password").roles("USER").build();

        // When
        String token = jwtService.generateToken(userDetails);

        // Then
        assertNotNull(token);
        assertTrue(Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getSubject().equals("username"));
    }

    @Test
    void testIsTokenValid() {
        // Given
        UserDetails userDetails = User.withUsername("username").password("password").roles("USER").build();
        String token = jwtService.generateToken(userDetails);

        // When
        boolean isValid = jwtService.isTokenValid(token, userDetails);

        // Then
        assertTrue(isValid);
    }

    @Test
    void testDecodeToken() {
        // Given
        String token = Jwts.builder()
                .subject("username")
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();

        // When
        String decodedUsername = jwtService.decodeToken(token);

        // Then
        assertEquals("username", decodedUsername);
    }


}
