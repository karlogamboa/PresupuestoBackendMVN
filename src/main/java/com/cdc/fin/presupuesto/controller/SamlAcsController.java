package com.cdc.fin.presupuesto.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import com.cdc.fin.presupuesto.handler.AcsHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

@RestController
public class SamlAcsController {

    @PostMapping("/saml/acs")
    public ResponseEntity<?> acs(@RequestBody String samlResponse) {
        AcsHandler handler = new AcsHandler();
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent().withBody(samlResponse);
        APIGatewayProxyResponseEvent response = handler.handleRequest(event, (Context) null);

        if (response.getStatusCode() == 302 && response.getHeaders() != null && response.getHeaders().containsKey("Location")) {
            return ResponseEntity.status(302).header("Location", response.getHeaders().get("Location")).build();
        }
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }
}
// Todo correcto: endpoint ACS y delegación.
