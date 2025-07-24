package com.cdc.presupuesto.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.saml2.provider.service.registration.InMemoryRelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.core.Saml2X509Credential;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.io.InputStream;

@Configuration
public class Saml2RelyingPartyConfig {

    @Value("${saml2.entity-id}")
    private String entityId;

    @Value("${saml2.sso-url}")
    private String ssoUrl;

    @Value("${saml2.cert-path:}")
    private String certPath;

    @Bean
    public RelyingPartyRegistrationRepository relyingPartyRegistrationRepository() throws Exception {
        X509Certificate certificate;
        if (certPath != null && !certPath.isBlank()) {
            try (InputStream certStream = new ClassPathResource(certPath).getInputStream()) {
                CertificateFactory factory = CertificateFactory.getInstance("X.509");
                certificate = (X509Certificate) factory.generateCertificate(certStream);
            }
        } else {
            throw new java.security.cert.CertificateException("No se encontró el certificado SAML2 en saml2.cert-path");
        }

        Saml2X509Credential verificationCredential = new Saml2X509Credential(
            certificate,
            Saml2X509Credential.Saml2X509CredentialType.VERIFICATION
        );

        RelyingPartyRegistration registration = RelyingPartyRegistration
                .withRegistrationId("okta")
                .entityId(entityId)
                .assertingPartyDetails(party -> party
                        .entityId(entityId)
                        .singleSignOnServiceLocation(ssoUrl)
                        .wantAuthnRequestsSigned(false)
                        .verificationX509Credentials(creds -> creds.add(verificationCredential))
                )
                .build();
        return new InMemoryRelyingPartyRegistrationRepository(registration);
    }
}