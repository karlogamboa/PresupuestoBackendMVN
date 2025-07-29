package com.cdc.fin.presupuesto.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.opensaml.saml.saml2.core.Response;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;

public class AcsHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(AcsHandler.class);

    @Value("${cors.allowed.origins:https://d38gv65skwp3eh.cloudfront.net,https://trial-4567848.okta.com}")
    private String corsAllowedOrigins;

    @Value("${jwt.secret:supersecretkey123}")
    private String jwtSecret;
    
    @Value("${frontend.redirect-url:https://d38gv65skwp3eh.cloudfront.net}")
    private String frontEndUrl;

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            logger.info("Received ACS request");
            String samlResponseBase64 = extractSamlResponse(request.getBody());
            if (samlResponseBase64 == null || samlResponseBase64.isEmpty()) {
                logger.warn("SAMLResponse body is missing");
                return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("Error: SAMLResponse vacío.");
            }

            // Procesa SAMLResponse, genera JWT y redirige al frontend
            Response samlResponse = decodeAndParseSamlResponse(samlResponseBase64);
            String nameId = extractNameId(samlResponse);

            // ...JWT y redirección igual que antes...
            Algorithm algorithm = Algorithm.HMAC256(jwtSecret);
            String token = JWT.create()
                .withSubject(nameId)
                .withClaim("email", nameId)
                .withIssuedAt(Date.from(Instant.now()))
                .withExpiresAt(Date.from(Instant.now().plusSeconds(3600)))
                .sign(algorithm);

            String redirectUrl = frontEndUrl + "?token=" + token;
            logger.info("Redirecting to frontend: {}", frontEndUrl);

            // Set cookie header for JWT token
            Map<String, String> headers = new HashMap<>();
            headers.put("Location", redirectUrl);
            headers.put("Access-Control-Allow-Origin", corsAllowedOrigins);
            headers.put("Access-Control-Allow-Credentials", "true");
            headers.put("Set-Cookie", "SESSION=" + token + "; Path=/; HttpOnly; Secure; SameSite=None");

            return new APIGatewayProxyResponseEvent()
                .withStatusCode(302)
                .withHeaders(headers);

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