package com.cdc.fin.presupuesto.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.onelogin.saml2.Auth;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.List;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opensaml.core.config.InitializationService;
import org.opensaml.saml.saml2.core.Response;
import org.opensaml.xmlsec.signature.support.SignatureException;
import java.util.Base64;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.io.UnmarshallingException;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;

public class AcsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AcsHandler.class);

    // Lee secretos desde AWS Secrets Manager
    private static final Map<String, String> secrets = getSecrets();
    private static final String JWT_SECRET = secrets.getOrDefault("JWT_SECRET_KEY", "default_secret");
    private static final String FRONTEND_URL = secrets.getOrDefault("FRONTEND_URL", "https://d38gv65skwp3eh.cloudfront.net");

    private static Map<String, String> getSecrets() {
        try (SecretsManagerClient client = SecretsManagerClient.builder().build()) {
            GetSecretValueRequest getSecretValueRequest = GetSecretValueRequest.builder()
                    .secretId("presupuesto-backend-secrets") // Cambia por el nombre real del secreto
                    .build();
            GetSecretValueResponse getSecretValueResponse = client.getSecretValue(getSecretValueRequest);
            String secretString = getSecretValueResponse.secretString();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(secretString, HashMap.class);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    static {
        try {
            InitializationService.initialize();
        } catch (Exception e) {
            throw new RuntimeException("OpenSAML initialization failed", e);
        }
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            logger.info("Received ACS request");
            String samlResponseBase64 = extractSamlResponse(request.getBody());
            if (samlResponseBase64 == null || samlResponseBase64.isEmpty()) {
                logger.warn("SAMLResponse body is missing");
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Error: SAMLResponse vacío.");
            }

            // Procesa el SAMLResponse con OpenSAML
            Response samlResponse = decodeAndParseSamlResponse(samlResponseBase64);
            String nameId = extractNameId(samlResponse);

            // ...JWT y redirección igual que antes...
            Algorithm algorithm = Algorithm.HMAC256(JWT_SECRET);
            String token = JWT.create()
                .withSubject(nameId)
                .withClaim("email", nameId)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(algorithm);

            String redirectUrl = FRONTEND_URL + "?token=" + token;
            logger.info("Redirecting to frontend: {}", FRONTEND_URL);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(302)
                    .withHeaders(Map.of("Location", redirectUrl));

        } catch (Exception e) {
            logger.error("Error al procesar la respuesta SAML: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(500)
                .withBody("Error al procesar la respuesta SAML: " + e.getMessage());
        }
    }

    private String extractSamlResponse(String body) {
        // Extrae el parámetro SAMLResponse del body (form-urlencoded)
        if (body == null) return null;
        for (String pair : body.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2 && kv[0].equals("SAMLResponse")) {
                return java.net.URLDecoder.decode(kv[1], java.nio.charset.StandardCharsets.UTF_8);
            }
        }
        return null;
    }

    private Response decodeAndParseSamlResponse(String samlResponse) throws Exception {
        // Decode Base64
        byte[] decodedBytes = java.util.Base64.getDecoder().decode(samlResponse);
        String xmlString = new String(decodedBytes, java.nio.charset.StandardCharsets.UTF_8).trim();

        if (xmlString.isEmpty()) {
            throw new IllegalArgumentException("Decoded SAMLResponse XML is empty");
        }

        // Secure XML parsing
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new java.io.ByteArrayInputStream(xmlString.getBytes(java.nio.charset.StandardCharsets.UTF_8)));

        // Get root element
        Element rootElement = document.getDocumentElement();
        if (rootElement == null || rootElement.getLocalName() == null || rootElement.getLocalName().isEmpty()) {
            throw new IllegalArgumentException("SAML Response root element is missing or invalid");
        }

        // Unmarshall to OpenSAML Response object
        UnmarshallerFactory unmarshallerFactory = XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(rootElement);
        if (unmarshaller == null) {
            throw new IllegalStateException("No unmarshaller found for SAML Response element");
        }
        XMLObject xmlObject = unmarshaller.unmarshall(rootElement);
        if (!(xmlObject instanceof Response)) {
            throw new IllegalArgumentException("Decoded SAMLResponse is not an OpenSAML Response object");
        }
        return (Response) xmlObject;
    }

    private String extractNameId(Response samlResponse) {
        if (samlResponse.getAssertions().isEmpty()) return null;
        return samlResponse.getAssertions().get(0).getSubject().getNameID().getValue();
    }
}
// Todo correcto: ACS, JWT y redirección.