package com.cdc.fin.presupuesto.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.github.scribejava.core.builder.ServiceBuilder;
import com.github.scribejava.core.model.OAuth1AccessToken;
import com.github.scribejava.core.model.OAuthRequest;
import com.github.scribejava.core.model.Response;
import com.github.scribejava.core.model.Verb;
import com.github.scribejava.core.oauth.OAuth10aService;

@Service
public class NetSuiteService {

    @Value("${netsuite.consumerKey}")
    private String consumerKey;
    @Value("${netsuite.consumerSecret}")
    private String consumerSecret;
    @Value("${netsuite.accessToken}")
    private String accessToken;
    @Value("${netsuite.tokenSecret}")
    private String tokenSecret;
    @Value("${netsuite.realm}")
    private String realm;
    @Value("${netsuite.baseUrl}")
    private String baseUrl;

    public String testConnection() {
        OAuth10aService service = new ServiceBuilder(consumerKey)
                .apiSecret(consumerSecret)
                .build(new com.github.scribejava.core.builder.api.DefaultApi10a() {
                    @Override
                    public String getRequestTokenEndpoint() {
                        return "https://" + realm + ".suitetalk.api.netsuite.com/services/oauth/request_token";
                    }
                    @Override
                    public String getAccessTokenEndpoint() {
                        return "https://" + realm + ".suitetalk.api.netsuite.com/services/oauth/access_token";
                    }
                    @Override
                    protected String getAuthorizationBaseUrl() {
                        return "https://" + realm + ".app.netsuite.com/app/login/secure/authorizetoken.ssp";
                    }
                });
        OAuth1AccessToken token = new OAuth1AccessToken(accessToken, tokenSecret);
        OAuthRequest request = new OAuthRequest(Verb.GET, baseUrl);
        request.addHeader("Content-Type", "application/json");
        service.signRequest(token, request);
        if (realm != null && !realm.isBlank()) {
            request.addHeader("realm", realm);
        }
        try {
            Response response = service.execute(request);
            if (response.isSuccessful()) {
                return response.getBody();
            } else {
                return "NetSuite error: " + response.getCode() + " - " + response.getMessage();
            }
        } catch (Exception e) {
            return "Exception: " + e.getMessage();
        }
    }
}