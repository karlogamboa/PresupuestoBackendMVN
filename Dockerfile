FROM amazoncorretto:21-alpine

# Set working directory
WORKDIR /app

# Copy Maven build output
COPY target/presupuesto-backend-*.jar app.jar

# Create non-root user for security
RUN addgroup -g 1001 -S apprunner && \
    adduser -u 1001 -S apprunner -G apprunner

# Change ownership of the app directory
RUN chown -R apprunner:apprunner /app
USER apprunner

# Expose port 8080
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/health || exit 1

# Run the Spring Boot application
ENTRYPOINT ["java", "-Xmx1024m", "-Xms512m", "-jar", "app.jar"]
