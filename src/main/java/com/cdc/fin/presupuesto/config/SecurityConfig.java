package com.cdc.fin.presupuesto.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import io.jsonwebtoken.*;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.*;
import java.net.URL;
import java.util.*;
import java.util.regex.*;
import org.springframework.security.config.Customizer;
import org.springframework.util.StreamUtils;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${security.jwt.secret:supersecretkey123}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.metadata-uri}")
    private String samlMetadataUrl;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.entity-id}")
    private String samlEntityId;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.sso-url:https://trial-4567848.okta.com/app/exktiw7x5dBKBcgUs697/sso/saml}")
    private String samlSsoUrl;

    @Value("${frontend.redirect-url:https://d38gv65skwp3eh.cloudfront.net}")
    private String frontendRedirectUrl;

    @Value("${stage:qa}")
    private String stage;

    @Value("${security.login.failure-url}")
    private String failureUrl;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/login", "/qa/login", "/scim/v2/**").permitAll() // <-- permite /qa/login
                .requestMatchers("/api/**", "/saml/user").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .saml2Login(saml2 -> saml2.successHandler(jwtSamlSuccessHandler()))
            .formLogin(form -> form
                .loginPage("/qa/login") // <-- loginPage ajustado
                .failureUrl("/qa/login?error") // <-- failureUrl ajustado
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Permite solo frontend y Okta (origins, not patterns)
        config.setAllowedOrigins(Arrays.asList(
            "https://d38gv65skwp3eh.cloudfront.net",
            "https://trial-4567848.okta.com"
        ));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        X509Certificate oktaCertificate = loadOktaCertificateFromMetadata(samlMetadataUrl);

        RelyingPartyRegistration registration = RelyingPartyRegistration
            .withRegistrationId("okta-saml")
            .entityId("https://v9hhsb7ju3.execute-api.us-east-2.amazonaws.com/qa/login/saml2/sso/okta-saml")
            .assertionConsumerServiceLocation("https://v9hhsb7ju3.execute-api.us-east-2.amazonaws.com/qa/login/saml2/sso/okta-saml")
            .assertingPartyDetails(party -> party
                .entityId(samlEntityId)
                .singleSignOnServiceLocation(samlSsoUrl)
                .wantAuthnRequestsSigned(false)
                .verificationX509Credentials(c -> {
                    if (oktaCertificate != null) {
                        c.add(new Saml2X509Credential(oktaCertificate, Saml2X509Credential.Saml2X509CredentialType.VERIFICATION));
                    } else {
                        logger.warn("Okta verification certificate is null! SAML2 verification will fail.");
                    }
                })
            )
            // No signingX509Credentials
            .build();
        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }

    @Bean
    public AuthenticationSuccessHandler jwtSamlSuccessHandler() {
        // Genera JWT tras login SAML2 y redirige al frontend
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication) throws IOException {
                String username = authentication.getName();
                String jwt = Jwts.builder()
                        .setSubject(username)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                        .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                        .compact();
                String redirectUrl = frontendRedirectUrl + "?token=" + jwt;
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            }
        };
    }

    @Bean
    public Filter jwtAuthenticationFilter() {
        // Valida JWT en endpoints protegidos
        return new Filter() {
            @Override
            public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                    throws IOException, ServletException {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                String authHeader = httpRequest.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    logger.info("JWT Filter: Validating token: {}", token);
                    logger.info("JWT Filter: Using secret: {}", jwtSecret);
                    try {
                        Claims claims = Jwts.parser()
                                .setSigningKey(jwtSecret.getBytes())
                                .parseClaimsJws(token)
                                .getBody();
                        String username = claims.getSubject();
                        String email = claims.get("email", String.class);
                        List<String> roles = claims.get("roles", List.class);
                        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                        if (roles != null) {
                            for (String role : roles) {
                                authorities.add(new SimpleGrantedAuthority(role));
                            }
                        } else {
                            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
                        }
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(username, null, authorities);
                        authentication.setDetails(email); // Puedes usar un objeto custom si necesitas más atributos
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } catch (Exception e) {
                        logger.warn("JWT Filter: Invalid token. Secret used: {}. Token: {}", jwtSecret, token, e);
                    }
                }
                chain.doFilter(request, response);
            }
        };
    }

    public Claims validateJwt(String jwt, String secret) {
        try {
            return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(jwt)
                .getBody();
        } catch (JwtException e) {
            return null;
        }
    }

    // Utilidad para cargar el certificado de Okta desde el metadata XML
    private X509Certificate loadOktaCertificateFromMetadata(String metadataUrl) {
        try (InputStream in = new URL(metadataUrl).openStream()) {
            String xml = new String(StreamUtils.copyToByteArray(in));
            Pattern pattern = Pattern.compile("<ds:X509Certificate>([^<]+)</ds:X509Certificate>");
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                String base64Cert = matcher.group(1).replaceAll("\\s+", "");
                byte[] decoded = Base64.getDecoder().decode(base64Cert);
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(decoded));
            }
        } catch (Exception e) {
            logger.error("Error loading Okta certificate from metadata", e);
        }
        return null;
    }


}