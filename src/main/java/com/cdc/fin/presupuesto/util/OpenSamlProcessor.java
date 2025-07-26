package com.cdc.fin.presupuesto.util;

import net.shibboleth.utilities.java.support.component.ComponentInitializationException;
import net.shibboleth.utilities.java.support.xml.BasicParserPool;
import net.shibboleth.utilities.java.support.xml.XMLParserException;
import org.opensaml.core.config.ConfigurationService;
import org.opensaml.core.config.InitializationException;
import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.saml.saml2.core.Subject;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureValidator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.Objects;

public class OpenSamlProcessor {

    static {
        try {
            InitializationService.initialize();
        } catch (InitializationException e) {
            throw new RuntimeException("Error al inicializar OpenSAML", e);
        }
    }

    /**
     * Procesa una SAMLResponse en Base64, valida su firma y extrae el NameID.
     *
     * @param base64SamlResponse La respuesta SAML codificada en Base64.
     * @param idpCertificatePem  El certificado público del IdP en formato PEM.
     * @return El NameID del sujeto si la validación es exitosa.
     * @throws Exception si la validación o el procesamiento fallan.
     */
    public String processSamlResponse(String base64SamlResponse, String idpCertificatePem) throws Exception {
        byte[] xmlBytes = Base64.getDecoder().decode(base64SamlResponse);
        String samlXml = new String(xmlBytes, StandardCharsets.UTF_8);

        Response samlResponse = (Response) unmarshall(samlXml);
        Assertion assertion = samlResponse.getAssertions().stream().findFirst()
                .orElseThrow(() -> new Exception("La respuesta SAML no contiene aserciones."));

        java.security.cert.X509Certificate cert = loadCertificate(idpCertificatePem);
        BasicX509Credential idpCredential = new BasicX509Credential(cert);

        Signature signature = assertion.getSignature();
        if (signature == null) {
            throw new Exception("La aserción no está firmada.");
        }
        SignatureValidator.validate(signature, idpCredential);

        Subject subject = assertion.getSubject();
        if (subject == null || subject.getNameID() == null) {
            throw new Exception("La aserción no contiene un Subject o NameID.");
        }
        return subject.getNameID().getValue();
    }

    private XMLObject unmarshall(String xmlString) throws XMLParserException, UnmarshallingException, ComponentInitializationException {
        BasicParserPool parserPool = new BasicParserPool();
        parserPool.initialize();

        InputStream in = new ByteArrayInputStream(xmlString.getBytes(StandardCharsets.UTF_8));
        Document doc = parserPool.parse(in);
        Element element = doc.getDocumentElement();

        UnmarshallerFactory unmarshallerFactory = ConfigurationService.get(UnmarshallerFactory.class);
        Unmarshaller unmarshaller = Objects.requireNonNull(unmarshallerFactory).getUnmarshaller(element);

        return Objects.requireNonNull(unmarshaller).unmarshall(element);
    }

    private X509Certificate loadCertificate(String certPem) throws Exception {
        CertificateFactory cf = CertificateFactory.getInstance("X.509");
        return (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(certPem.getBytes(StandardCharsets.UTF_8)));
    }
}
