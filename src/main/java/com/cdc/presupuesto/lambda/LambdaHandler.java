package com.cdc.presupuesto.lambda;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.cdc.presupuesto.PresupuestoBackendApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AWS Lambda Handler for Spring Boot Application
 * This handler allows the Spring Boot application to run on AWS Lambda
 */
public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LambdaHandler.class);
    
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    
    static {
        try {
            logger.info("Initializing Lambda handler for Spring Boot application");
            handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(PresupuestoBackendApplication.class);
            
            // Enable timer with CloudWatch events
            handler.activateSpringProfiles("lambda");
            
            logger.info("Lambda handler initialized successfully");
        } catch (ContainerInitializationException e) {
            logger.error("Could not initialize Spring Boot application", e);
            throw new RuntimeException("Could not initialize Spring Boot application", e);
        }
    }

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        logger.info("Processing request: {} {}", input.getHttpMethod(), input.getPath());
        
        // Set Lambda context in request attributes
        if (context != null) {
            logger.info("Lambda context - Function: {}, Version: {}, Memory: {}MB, Timeout: {}ms",
                       context.getFunctionName(), 
                       context.getFunctionVersion(),
                       context.getMemoryLimitInMB(),
                       context.getRemainingTimeInMillis());
        }
        
        return handler.proxy(input, context);
    }
}
