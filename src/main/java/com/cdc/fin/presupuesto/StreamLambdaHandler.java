package com.cdc.fin.presupuesto;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.*;

public class StreamLambdaHandler implements RequestStreamHandler {
    private static SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static final Object LOCK = new Object();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        // Initialize Spring Boot handler once (thread-safe)
        if (handler == null) {
            synchronized (LOCK) {
                if (handler == null) {
                    try {
                        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(PresupuestoBackendApplication.class);
                    } catch (ContainerInitializationException e) {
                        throw new RuntimeException("Could not initialize Spring Boot application", e);
                    }
                }
            }
        }

        // Read and log the incoming event for debugging
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        byte[] eventBytes = baos.toByteArray();
        String event = new String(eventBytes, "UTF-8");
        context.getLogger().log("Received event: " + event);

        // Pass a fresh InputStream to the handler
        ByteArrayInputStream resetStream = new ByteArrayInputStream(eventBytes);

        // Delegate to Spring Boot handler
        handler.proxyStream(resetStream, outputStream, context);
    }
}
// Todo correcto: inicialización, logging y delegación.
