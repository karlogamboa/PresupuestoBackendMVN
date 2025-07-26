package com.cdc.fin.presupuesto.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.cdc.fin.presupuesto.util.OpenSamlProcessor;
import com.cdc.fin.presupuesto.util.FormDataParser;
import java.util.Base64;
import java.util.Map;

public class SamlLambdaHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        Map<String, String> formData = request.getIsBase64Encoded() ?
            FormDataParser.parse(new String(Base64.getDecoder().decode(request.getBody()))) :
            FormDataParser.parse(request.getBody());

        String samlResponseBase64 = formData.get("SAMLResponse");
        if (samlResponseBase64 == null || samlResponseBase64.isEmpty()) {
            return new APIGatewayProxyResponseEvent().withStatusCode(400).withBody("SAMLResponse no encontrada.");
        }

        String idpCert = System.getenv("IDP_CERTIFICATE_PEM");
        try {
            OpenSamlProcessor processor = new OpenSamlProcessor();
            String nameId = processor.processSamlResponse(samlResponseBase64, idpCert);

            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(200)
                    .withBody("Autenticación exitosa para usuario: " + nameId);

        } catch (Exception e) {
            context.getLogger().log("Error de validación SAML: " + e.getMessage());
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(401)
                    .withBody("Error de autenticación SAML.");
        }
    }
}
