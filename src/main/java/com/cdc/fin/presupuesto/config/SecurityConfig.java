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
import com.cdc.fin.presupuesto.util.OktaSAMLHelper;

@Configuration
@EnableWebSecurity
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);

    private final com.cdc.fin.presupuesto.service.ScimUserService scimUserService;

    public SecurityConfig(com.cdc.fin.presupuesto.service.ScimUserService scimUserService) {
        this.scimUserService = scimUserService;
    }

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.expiration-ms}")
    private long jwtExpirationMs;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.metadata-uri}")
    private String samlAssertingPartyMetadataUrl;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.entity-id}")
    private String samlAssertingPartyEntityId;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.entity-id}")
    private String oktaEntityId;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.assertion-consumer-service.location}")
    private String samlAssertionConsumerServiceLocation;

    @Value("${spring.security.saml2.relyingparty.registration.okta-saml.assertingparty.sso-url}")
    private String samlAssertingPartySsoUrl;

    @Value("${frontend.redirect-url}")
    private String frontendRedirectUrl;

    @Value("${api.stage:qa}")
    private String stage;

    @Value("${security.login.failure-url}")
    private String failureUrl;
    
    @Value("${cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods}")
    private String allowedMethods;

    @Value("${cors.allowed-headers}")
    private String allowedHeaders;

    @Value("${cors.allow-credentials}")
    private boolean allowCredentials;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        String loginPageUrl = "/" + stage + "/login";
        String failurePageUrl = "/" + stage + "/login?error";

        http
            .authorizeHttpRequests(auth -> auth
            .requestMatchers("/health", loginPageUrl, "/scim/v2/**").permitAll()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .saml2Login(saml2 -> saml2
            .successHandler(jwtSamlSuccessHandler())
            )
            .formLogin(form -> form
            .loginPage(loginPageUrl)
            .failureUrl(failurePageUrl)
            )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowCredentials(allowCredentials);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() {
        X509Certificate oktaCertificate = loadOktaCertificateFromMetadata(samlAssertingPartyMetadataUrl);

        RelyingPartyRegistration registration = RelyingPartyRegistration
            .withRegistrationId("okta-saml")
            .entityId(oktaEntityId)
            .assertionConsumerServiceLocation(samlAssertionConsumerServiceLocation)
            .assertingPartyDetails(party -> party
                .entityId(samlAssertingPartyEntityId)
                .singleSignOnServiceLocation(samlAssertingPartySsoUrl)
                .wantAuthnRequestsSigned(false)
                .verificationX509Credentials(c -> {
                    if (oktaCertificate != null) {
                        c.add(new Saml2X509Credential(oktaCertificate, Saml2X509Credential.Saml2X509CredentialType.VERIFICATION));
                    } else {
                        logger.warn("Okta verification certificate is null! SAML2 verification will fail.");
                    }
                })
            )
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
                Map<String, Object> samlClaims = new HashMap<>();
                List<String> roles = new ArrayList<>();

                // Obtener el SAMLResponse en Base64 si Okta lo envía como parámetro POST
                String samlResponseBase64 = request.getParameter("SAMLResponse");
                if (samlResponseBase64 != null && !samlResponseBase64.isEmpty()) {
                    try {
                        OktaSAMLHelper.initializeSAML();
                        Map<String, java.util.List<String>> samlAttrs = OktaSAMLHelper.getUserAttributes(samlResponseBase64);
                        logger.info("SAML attributes at login: {" + samlAttrs + "}");
                        // Flatten attributes for JWT claims
                        for (Map.Entry<String, java.util.List<String>> entry : samlAttrs.entrySet()) {
                            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                                samlClaims.put(entry.getKey(), entry.getValue().size() == 1 ? entry.getValue().get(0) : entry.getValue());
                                // Si el atributo es "admin" o "user", agrégalo como rol
                                if ("admin".equalsIgnoreCase(entry.getKey()) || "user".equalsIgnoreCase(entry.getKey())) {
                                    roles.add(entry.getValue().get(0));
                                }
                            }
                        }
                        // Si no hay roles, usa "user" por defecto
                        if (roles.isEmpty()) {
                            roles.add("user");
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing SAML attributes: {" + e.getMessage() + "}");
                    }
                }
                JwtBuilder jwtBuilder = Jwts.builder()
                        .setSubject(username)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs));
                // Agrega los atributos SAML como claims
                for (Map.Entry<String, Object> entry : samlClaims.entrySet()) {
                    jwtBuilder.claim(entry.getKey(), entry.getValue());
                }
                // Agrega los roles como claim "roles"
                if (!roles.isEmpty()) {
                    jwtBuilder.claim("roles", roles);
                }
                String jwt = jwtBuilder
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
                String path = httpRequest.getRequestURI();
                String authHeader = httpRequest.getHeader("Authorization");
                boolean isScim = path.startsWith("/scim/v2/");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (isScim) {
                        // Para SCIM, solo verifica que el token exista (no JWT estricto)
                        // Puedes agregar validación extra si lo requieres
                        logger.info("SCIM endpoint: Accepting simple bearer token");
                        // Opcional: puedes setear un usuario dummy si lo necesitas
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken("scim-client", null, List.of(new SimpleGrantedAuthority("ROLE_SCIM")));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        // Para otros endpoints, JWT estricto
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