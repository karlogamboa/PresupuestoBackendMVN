package com.cdc.fin.presupuesto.util;

import org.opensaml.core.config.InitializationService;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.io.Unmarshaller;
import org.opensaml.core.xml.io.UnmarshallerFactory;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.opensaml.saml.saml2.core.Response;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OktaSAMLHelper {

    private static final Logger logger = LoggerFactory.getLogger(OktaSAMLHelper.class);

    /**
     * Inicializa las librerías de OpenSAML. Es crucial llamar a este método
     * una vez al inicio de la aplicación.
     */
    public static void initializeSAML() {
        try {
            InitializationService.initialize();
        } catch (Exception e) {
            throw new RuntimeException("Error al inicializar OpenSAML", e);
        }
    }

    /**
     * Procesa la respuesta SAML recibida en Base64 desde Okta.
     *
     * @param samlResponseBase64 La cadena Base64 que contiene la respuesta SAML.
     * @return Un mapa con los atributos del usuario. Las claves son los nombres de los atributos
     * y los valores son listas de los valores de dichos atributos.
     * @throws Exception si ocurre un error al procesar la respuesta.
     */
    public static Map<String, List<String>> getUserAttributes(String samlResponseBase64) throws Exception {
        logger.info("Decodificando SAMLResponse Base64...");
        byte[] samlResponseBytes = Base64.getDecoder().decode(samlResponseBase64);
        InputStream samlInputStream = new ByteArrayInputStream(samlResponseBytes);

        logger.info("Parseando XML SAML...");
        DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
        documentBuilderFactory.setNamespaceAware(true);
        DocumentBuilder docBuilder = documentBuilderFactory.newDocumentBuilder();
        Document document = docBuilder.parse(samlInputStream);
        Element samlElement = document.getDocumentElement();

        logger.info("Deserializando XML a objeto SAML Response...");
        UnmarshallerFactory unmarshallerFactory = org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport.getUnmarshallerFactory();
        Unmarshaller unmarshaller = unmarshallerFactory.getUnmarshaller(samlElement);
        Response samlResponse = (Response) unmarshaller.unmarshall(samlElement);

        Assertion assertion = samlResponse.getAssertions().get(0);
        if (assertion == null) {
            logger.error("La respuesta SAML no contiene ninguna aserción.");
            throw new IllegalArgumentException("La respuesta SAML no contiene ninguna aserción.");
        }

        Map<String, List<String>> attributesMap = new HashMap<>();
        for (AttributeStatement attributeStatement : assertion.getAttributeStatements()) {
            for (Attribute attribute : attributeStatement.getAttributes()) {
                String name = attribute.getName();
                List<String> values = attribute.getAttributeValues().stream()
                        .map(xmlObj -> xmlObj.getDOM().getTextContent())
                        .collect(Collectors.toList());
                logger.info("Atributo SAML: {} = {}", name, values);
                attributesMap.put(name, values);
            }
        }
        
        // Opcional: Extraer el NameID (identificador principal del usuario)
        String nameId = assertion.getSubject().getNameID().getValue();
        logger.info("NameID extraído: {}", nameId);
        attributesMap.put("NameID", List.of(nameId));

        return attributesMap;
    }

    /**
     * Método de demostración para obtener atributos de usuario desde una respuesta SAML en Base64.
     * Llama a initializeSAML() y getUserAttributes(), imprime los atributos.
     */
    public static void demoGetUserAttributes(String samlResponseFromOkta) {
        initializeSAML();
        try {
            Map<String, List<String>> userAttributes = getUserAttributes(samlResponseFromOkta);
            logger.debug("Atributos del usuario obtenidos de Okta:");
            userAttributes.forEach((key, value) ->
                logger.debug("  - {}: {}", key, String.join(", ", value))
            );
        } catch (Exception e) {
            logger.error("Error procesando la respuesta SAML: {}", e.getMessage(), e);
            // En un entorno real, aquí deberías manejar el error apropiadamente (ej. redirigir a una página de error).
        }
    }

    // El método main solo para pruebas manuales
    public static void main(String[] args) {
        // Reemplazar con una respuesta SAML real codificada en Base64
        String samlResponseFromOkta = "PD94bWwgdm...Cg==";
        demoGetUserAttributes(samlResponseFromOkta);
    }
}