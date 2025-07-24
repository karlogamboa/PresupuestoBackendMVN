package com.cdc.presupuesto.util;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class JwtUtils {
    // En producción, usa una clave segura y almacénala en un vault/env
    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
    private static final long EXPIRATION_MS = 3600_000; // 1 hora

    public static String generateToken(String email, List<String> roles, Map<String, Object> extraClaims) {
        return Jwts.builder()
                .setSubject(email)
                .addClaims(extraClaims)
                .claim("roles", roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(key)
                .compact();
    }
}
