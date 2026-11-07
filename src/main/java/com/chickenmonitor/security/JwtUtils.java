package com.chickenmonitor.security;

import com.chickenmonitor.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtils {

    public static final String COOKIE_NAME = "AUTH_TOKEN";

    private final SecretKey secretKey;
    private final long jwtExpirationMs;

    public JwtUtils(
            @Value("${app.jwt.secret:VGhpc0lzQVNlY3VyZVNlY3JldEtleUZvckpXVEVuY29kaW5nMTIzNDU2Nzg5MDE=}") String jwtSecret,
            @Value("${app.jwt.expiration-ms:86400000}") long jwtExpirationMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
        this.jwtExpirationMs = jwtExpirationMs;
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id_user", String.valueOf(user.getUserId()));
        claims.put("role", user.getNormalizedRole());

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getUsername())
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractIdUser(String token) {
        return extractAllClaims(token).get("id_user", String.class);
    }

    public String extractRole(String token) {
        return User.normalizeRole(extractAllClaims(token).get("role", String.class));
    }

    public long getJwtExpirationMs() {
        return jwtExpirationMs;
    }

    public boolean isTokenValid(String token, User user) {
        String username = extractUsername(token);
        String idUser = extractIdUser(token);
        String role = extractRole(token);

        return username.equals(user.getUsername())
                && idUser.equals(String.valueOf(user.getUserId()))
                && user.getNormalizedRole().equals(role)
                && !isTokenExpired(token);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private boolean isTokenExpired(String token) {
        Date expiration = extractClaim(token, Claims::getExpiration);
        return expiration.before(new Date());
    }
}
