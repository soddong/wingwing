package com.ssafy.shieldron.global.util;

import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;

    public JwtUtil(@Value("${jwt.secret}") String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public Boolean isExpired(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration().before(new Date());
    }

    public String getPhoneNumber(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("phoneNumber", String.class);
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String generateAccessToken(String username, String phoneNumber) {
        return createToken(username, phoneNumber, (long) (1000 * 60 * 15));
    }

    public String generateRefreshToken(String username, String phoneNumber) {
        return createToken(username, phoneNumber, (long) (1000 * 60 * 60 * 24 * 7));
    }

    public boolean isTokenInvalid(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return false;
        } catch (Exception e) {
            return true;
        }
    }

    private String createToken(String username, String phoneNumber, Long expiredMs) {
        return Jwts.builder()
                .header().add("typ", "JWT")
                .and()
                .claim("username", username)
                .claim("phoneNumber", phoneNumber)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }
}
