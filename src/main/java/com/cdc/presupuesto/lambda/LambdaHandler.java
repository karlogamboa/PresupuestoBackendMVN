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

public class LambdaHandler implements RequestHandler<AwsProxyRequest, AwsProxyResponse> {

    private static final Logger logger = LoggerFactory.getLogger(LambdaHandler.class);
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static final Object LOCK = new Object();

    @Override
    public AwsProxyResponse handleRequest(AwsProxyRequest input, Context context) {
        if (handler == null) {
            synchronized (LOCK) {
                if (handler == null) {
                    try {
                       handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(PresupuestoBackendApplication.class, "lambda");
                       
                        logger.info("Lambda handler initialized successfully");
                    } catch (ContainerInitializationException e) {
                        logger.error("Could not initialize Spring Boot application", e);
                        throw new RuntimeException("Could not initialize Spring Boot application", e);
                    }
                }
            }
        }
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
