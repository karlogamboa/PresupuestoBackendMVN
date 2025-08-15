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

import com.cdc.fin.presupuesto.util.OktaSAMLHelper;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticationException;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.w3c.dom.Element;

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
        // String loginPageUrl = "/" + stage +"/saml2/authenticate/okta-saml";

        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/health", "/login/saml2/sso/okta-saml", "/scim/v2/**", "/api/netsuite/**").permitAll()
                .requestMatchers("/api/**").authenticated()
                .anyRequest().authenticated()
            )
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .saml2Login(saml2 -> saml2
                .loginProcessingUrl("/saml2/authenticate/okta-saml") // <-- NO debe tener el stage
                .successHandler(jwtSamlSuccessHandler())
            )
            // .formLogin(form -> form
            //     .loginPage(loginPageUrl)
            // )
            .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        List<String> origins = Arrays.asList(allowedOrigins.split(","));
        config.setAllowedOrigins(origins);
        config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowCredentials(allowCredentials);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource() {
            @Override
            public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                CorsConfiguration cors = super.getCorsConfiguration(request);
                if (cors != null && allowCredentials) {
                    String origin = request.getHeader("Origin");
                    if (origin != null && origins.stream().anyMatch(o -> o.trim().equals(origin))) {
                        cors.setAllowedOrigins(List.of(origin));
                    }
                }
                return cors;
            }
        };
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
                boolean isApi = path.startsWith("/api/");
                boolean authenticated = false;
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
                        authenticated = true;
                    } else {
                        // Para otros endpoints, JWT estricto
                        try {
                            Claims claims = Jwts.parser()
                                    .setSigningKey(jwtSecret.getBytes())
                                    .parseClaimsJws(token)
                                    .getBody();
                            String username = claims.getSubject();
                            String email = claims.get("email", String.class);
                            Object groupClaim = claims.get("group");
                            List<String> groups;
                            if (groupClaim instanceof String) {
                                groups = Collections.singletonList((String) groupClaim);
                            } else if (groupClaim instanceof List) {
                                groups = (List<String>) groupClaim;
                            } else {
                                groups = Collections.emptyList();
                            }
                            List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                            for (String group : groups) {
                                authorities.add(new SimpleGrantedAuthority(group));
                            }
                            UsernamePasswordAuthenticationToken authentication =
                                    new UsernamePasswordAuthenticationToken(username, null, authorities);
                            authentication.setDetails(email); // Puedes usar un objeto custom si necesitas más atributos
                            SecurityContextHolder.getContext().setAuthentication(authentication);
                            authenticated = true;
                        } catch (io.jsonwebtoken.ExpiredJwtException e) {
                            logger.warn("JWT expirado: {}", e.getMessage());
                            jakarta.servlet.http.HttpServletResponse httpResp = (jakarta.servlet.http.HttpServletResponse) response;
                            httpResp.setStatus(401);
                            httpResp.setContentType("application/json");
                            httpResp.getWriter().write("{\"error\":\"jwt_expired\",\"message\":\"Tu sesión ha expirado. Por favor vuelve a iniciar sesión.\"}");
                            return;
                        } catch (Exception e) {
                            logger.warn("JWT Filter: Invalid token. Secret used: {}. Token: {}", jwtSecret, token, e);
                        }
                    }
                }
                // Si es endpoint /api/** y no está autenticado, responde 401 en vez de redirigir
                if (isApi && !authenticated) {
                    ((jakarta.servlet.http.HttpServletResponse) response).sendError(401, "Unauthorized: Invalid or missing token");
                    return;
                }
                chain.doFilter(request, response);
            }
        };
    }

    @Bean
    public AuthenticationSuccessHandler jwtSamlSuccessHandler() {
        return new SimpleUrlAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response,
                                                org.springframework.security.core.Authentication authentication) throws IOException {
                logger.debug("SAML Success Handler triggered");
                String username = authentication.getName();
                logger.debug("Authenticated username: " + username);
                Map<String, Object> samlClaims = new HashMap<>();
                List<String> groupList = new ArrayList<>();

                String samlResponseBase64 = request.getParameter("SAMLResponse");
                logger.debug("SAMLResponse param present: " + (samlResponseBase64 != null && !samlResponseBase64.isEmpty()));
                if (samlResponseBase64 != null && !samlResponseBase64.isEmpty()) {
                    try {
                        // Usa el parser OpenSAML para obtener el assertion ignorando InResponseTo
                        Assertion assertion = parseSamlResponseIgnoringInResponseTo(samlResponseBase64);
                        logger.debug("OpenSAML assertion parsed: " + assertion.getID());
                        // Extrae atributos del assertion
                        if (assertion.getAttributeStatements() != null) {
                            for (org.opensaml.saml.saml2.core.AttributeStatement attrStmt : assertion.getAttributeStatements()) {
                                for (org.opensaml.saml.saml2.core.Attribute attr : attrStmt.getAttributes()) {
                                    List<Object> values = new ArrayList<>();
                                    for (org.opensaml.core.xml.XMLObject valueObj : attr.getAttributeValues()) {
                                        values.add(valueObj.getDOM().getTextContent());
                                    }
                                    if (!values.isEmpty()) {
                                        samlClaims.put(attr.getName(), values.size() == 1 ? values.get(0) : values);
                                        if ("group".equalsIgnoreCase(attr.getName())) {
                                            if (values.size() == 1) {
                                                groupList.add(values.get(0).toString());
                                            } else {
                                                for (Object v : values) groupList.add(v.toString());
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        if (groupList.isEmpty()) {
                            groupList.add("user");
                            logger.debug("No group found, defaulting to 'user'");
                        }
                    } catch (Exception e) {
                        logger.warn("Error parsing SAML attributes with OpenSAML: " + e.getMessage());
                    }
                } else {
                    logger.debug("No SAMLResponse found in request");
                }
                JwtBuilder jwtBuilder = Jwts.builder()
                        .setSubject(username)
                        .setIssuedAt(new Date())
                        .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs));
                logger.debug("Building JWT claims");
                for (Map.Entry<String, Object> entry : samlClaims.entrySet()) {
                    logger.debug("JWT claim: " + entry.getKey() + " = " + entry.getValue());
                    jwtBuilder.claim(entry.getKey(), entry.getValue());
                }
                if (!groupList.isEmpty()) {
                    logger.debug("JWT group claim: " + groupList);
                    jwtBuilder.claim("group", groupList);
                }
                String jwt = jwtBuilder
                        .signWith(SignatureAlgorithm.HS256, jwtSecret.getBytes())
                        .compact();
                logger.debug("Generated JWT: " + jwt);
                String redirectUrl = frontendRedirectUrl + "?token=" + jwt;
                logger.debug("Redirecting to frontend: " + redirectUrl);
                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
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

    // Ejemplo de parser SAML usando OpenSAML ignorando InResponseTo
    public Assertion parseSamlResponseIgnoringInResponseTo(String samlResponseBase64) throws Exception {
        // Decodifica y parsea el XML SAML Response
        byte[] decoded = java.util.Base64.getDecoder().decode(samlResponseBase64);
        String xml = new String(decoded, java.nio.charset.StandardCharsets.UTF_8);
        javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        org.w3c.dom.Document doc = factory.newDocumentBuilder().parse(new java.io.ByteArrayInputStream(xml.getBytes()));
        org.w3c.dom.Element element = doc.getDocumentElement();

        org.opensaml.core.xml.io.UnmarshallerFactory unmarshallerFactory = org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        org.opensaml.core.xml.io.Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(element);
        org.opensaml.core.xml.XMLObject xmlObject = unmarshaller.unmarshall(element);

        org.opensaml.saml.saml2.core.Response response = (org.opensaml.saml.saml2.core.Response) xmlObject;
        // Ignora la validación de InResponseTo aquí
        if (response.getAssertions() != null && !response.getAssertions().isEmpty()) {
            return response.getAssertions().get(0);
        }
        throw new IllegalArgumentException("No assertions found in SAML response");
    }
}
// La configuración actual es correcta y cumple con los requisitos.
// No se requieren cambios en este archivo.