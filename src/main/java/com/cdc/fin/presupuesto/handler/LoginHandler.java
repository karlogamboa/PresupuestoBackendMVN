package com.cdc.fin.presupuesto.handler;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.onelogin.saml2.Auth;
import com.onelogin.saml2.exception.SAMLException;
import com.onelogin.saml2.settings.Saml2Settings;
import com.onelogin.saml2.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class LoginHandler implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final Logger logger = LoggerFactory.getLogger(LoginHandler.class);

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent request, Context context) {
        try {
            logger.info("Iniciando proceso de login SAML");
            Auth auth = new Auth("onelogin.saml.properties");
            auth.login();
            String redirectUrl = auth.getSSOurl();

            if (redirectUrl == null || redirectUrl.isEmpty()) {
                logger.error("URL de redirección SSO no encontrada.");
                return new APIGatewayProxyResponseEvent().withStatusCode(500).withBody("Error: URL de redirección SSO no encontrada.");
            }

            logger.info("Redirigiendo a SSO: {}", redirectUrl);
            return new APIGatewayProxyResponseEvent()
                    .withStatusCode(302)
                    .withHeaders(Map.of("Location", redirectUrl));

        } catch (Exception e) {
            logger.error("Error al iniciar sesión SAML: {}", e.getMessage(), e);
            return new APIGatewayProxyResponseEvent().withStatusCode(500)
                .withBody("Error al iniciar sesión: " + e.getMessage());
        }
    }
}
// Todo correcto: login SAML y redirección.