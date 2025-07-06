package com.cdc.presupuesto.service;

import com.cdc.presupuesto.model.Usuario;
import com.cdc.presupuesto.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserInfoService {
    
    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);
    
    @Value("${okta.oauth2.issuer}")
    private String issuer;
    
    @Autowired
    private UsuarioRepository usuarioRepository;
    
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Obtiene la información del usuario desde DynamoDB usando el email del JWT
     * @param jwt JWT token del usuario
     * @return Map con la información del usuario incluyendo roles
     */
    public Map<String, Object> getUserInfo(Jwt jwt) {
        try {
            // Intentar obtener el email de diferentes claims del JWT
            String userEmail = extractEmailFromJwt(jwt);
            
            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("No se encontró email en el JWT. Claims disponibles: {}", jwt.getClaims().keySet());
                // Devolver información básica del JWT si no se encuentra email
                logger.error("No se encontró un email válido en el JWT. No se puede obtener información del usuario.");
                throw new IllegalArgumentException("No se encontró un email válido en el JWT. No se puede obtener información del usuario.");
            }
            
            logger.debug("Buscando información del usuario con email: {}", userEmail);
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(userEmail);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                return Map.of(
                    "email", usuario.getEmail(),
                    "nombre", usuario.getNombre() != null ? usuario.getNombre() : "",
                    "numeroEmpleado", usuario.getNumeroEmpleado() != null ? usuario.getNumeroEmpleado() : "",
                    "role", usuario.getRole() != null ? usuario.getRole() : "User",
                    "activo", usuario.getActivo() != null ? usuario.getActivo() : "true"
                );
            } else {
                logger.warn("Usuario no encontrado en DynamoDB para email: {}", userEmail);
                // Retornar información básica del JWT si no se encuentra en DynamoDB
                return Map.of(
                    "email", userEmail,
                    "nombre", jwt.getClaim("name") != null ? jwt.getClaim("name") : "",
                    "numeroEmpleado", "",
                    "role", "User",
                    "activo", "true"
                );
            }
            
        } catch (Exception e) {
            logger.error("Error getting user info from DynamoDB: {}", e.getMessage(), e);
            return Map.of();
        }
    }
    
    /**
     * Extrae el email del JWT probando diferentes claims
     * @param jwt JWT token
     * @return email del usuario o null si no se encuentra
     */
    private String extractEmailFromJwt(Jwt jwt) {
        // Intentar diferentes claims comunes para el email
        String[] emailClaims = {"email", "preferred_username", "sub", "username"};
        
        for (String claim : emailClaims) {
            String value = jwt.getClaim(claim);
            if (value != null && !value.isEmpty()) {
                // Verificar si es un email válido
                if (value.contains("@")) {
                    logger.debug("Email encontrado en claim '{}': {}", claim, value);
                    return value;
                }
            }
        }
        
        logger.debug("No se encontró email válido en ningún claim. Claims disponibles: {}", jwt.getClaims().keySet());
        return null;
    }
    
    /**
     * Obtiene la información del usuario desde Okta User Info endpoint (método de respaldo)
     * @param jwt JWT token del usuario
     * @return Map con la información del usuario desde Okta
     */
    public Map<String, Object> getOktaUserInfo(Jwt jwt) {
        try {
            String userInfoEndpoint = issuer + "/v1/userinfo";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(jwt.getTokenValue());
            headers.set("Accept", "application/json");
            
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                userInfoEndpoint,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                logger.warn("Failed to get user info from Okta. Status: {}", response.getStatusCode());
                return Map.of();
            }
            
        } catch (Exception e) {
            logger.error("Error getting user info from Okta: {}", e.getMessage(), e);
            return Map.of();
        }
    }
      /**
     * Verifica si el usuario tiene rol de Admin
     * @param jwt JWT token del usuario
     * @return true si el usuario es Admin, false en caso contrario
     */
    public boolean isAdmin(Jwt jwt) {
        try {
            String userEmail = extractEmailFromJwt(jwt);
            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("No se encontró email en el JWT para verificar rol de Admin. Claims: {}", jwt.getClaims().keySet());
                return false;
            }
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(userEmail);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                String role = usuario.getRole();
                boolean isAdmin = "Admin".equals(role);
                logger.debug("Usuario {} tiene role: {}, es Admin: {}", userEmail, role, isAdmin);
                return isAdmin;
            } else {
                logger.debug("Usuario {} no encontrado en DynamoDB, no es Admin", userEmail);
                return false;
            }
            
        } catch (Exception e) {
            logger.error("Error checking admin role for user: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Obtiene los roles del usuario
     * @param jwt JWT token del usuario
     * @return Lista de roles del usuario
     */
    public String getUserRoles(Jwt jwt) {
        try {
            String userEmail = extractEmailFromJwt(jwt);
            if (userEmail == null || userEmail.isEmpty()) {
                logger.warn("No se encontró email en el JWT para obtener roles. Claims: {}", jwt.getClaims().keySet());
                return "None";
            }
            
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(userEmail);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                String role = usuario.getRole();
                if (role != null && !role.isEmpty()) {
                    return role;
                } else {
                    return "User";
                }
            } else {
                logger.debug("Usuario {} no encontrado en DynamoDB, asignando role User", userEmail);
                return "User";
            }
            
        } catch (Exception e) {
            logger.error("Error getting user roles: {}", e.getMessage(), e);
            return "None";
        }
    }

    /**
     * Importa usuarios desde un archivo CSV
     * @param file Archivo CSV con los usuarios
     * @return Map con el resultado de la importación
     */
    public Map<String, Object> importUsuariosFromCsv(MultipartFile file) {
        logger.info("Iniciando importación de usuarios desde CSV: {}", file.getOriginalFilename());
        
        List<Usuario> usuariosImportados = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        int lineNumber = 0;
        
        try (Reader reader = new InputStreamReader(file.getInputStream());
             CSVReader csvReader = new CSVReader(reader)) {
            
            String[] nextLine;
            boolean isFirstLine = true;
            
            while ((nextLine = csvReader.readNext()) != null) {
                lineNumber++;
                
                // Saltar el header si es la primera línea
                if (isFirstLine) {
                    isFirstLine = false;
                    // Verificar que el header tenga el formato correcto
                    if (nextLine.length < 4) {
                        errores.add("Header incorrecto. Se esperan al menos 4 columnas: email, nombre, numeroEmpleado, role");
                        break;
                    }
                    continue;
                }
                
                try {
                    Usuario usuario = parseUsuarioFromCsvLine(nextLine, lineNumber);
                    if (usuario != null) {
                        // Verificar si el usuario ya existe
                        if (usuarioRepository.existsByEmail(usuario.getEmail())) {
                            logger.warn("Usuario con email {} ya existe, se omitirá", usuario.getEmail());
                            errores.add("Línea " + lineNumber + ": Usuario con email " + usuario.getEmail() + " ya existe");
                        } else {
                            Usuario usuarioGuardado = usuarioRepository.save(usuario);
                            usuariosImportados.add(usuarioGuardado);
                            logger.debug("Usuario importado exitosamente: {}", usuario.getEmail());
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error procesando línea {}: {}", lineNumber, e.getMessage());
                    errores.add("Línea " + lineNumber + ": " + e.getMessage());
                }
            }
            
        } catch (IOException | CsvValidationException e) {
            logger.error("Error leyendo archivo CSV: {}", e.getMessage(), e);
            errores.add("Error leyendo archivo CSV: " + e.getMessage());
        }
        
        logger.info("Importación completada. Usuarios importados: {}, Errores: {}", 
                   usuariosImportados.size(), errores.size());
        
        return Map.of(
            "success", errores.isEmpty(),
            "totalImportados", usuariosImportados.size(),
            "totalErrores", errores.size(),
            "errores", errores,
            "message", String.format("Importación completada. %d usuarios importados, %d errores", 
                                   usuariosImportados.size(), errores.size())
        );
    }

    /**
     * Parsea una línea del CSV y crea un objeto Usuario
     * @param csvLine Línea del CSV
     * @param lineNumber Número de línea para logging
     * @return Usuario creado o null si hay error
     */
    private Usuario parseUsuarioFromCsvLine(String[] csvLine, int lineNumber) {
        if (csvLine.length < 4) {
            throw new IllegalArgumentException("Línea debe tener al menos 4 columnas: email, nombre, numeroEmpleado, role");
        }
        
        String email = csvLine[0] != null ? csvLine[0].trim() : "";
        String nombre = csvLine[1] != null ? csvLine[1].trim() : "";
        String numeroEmpleado = csvLine[2] != null ? csvLine[2].trim() : "";
        String role = csvLine[3] != null ? csvLine[3].trim() : "User";
        
        // Validaciones básicas
        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email es requerido");
        }
        
        if (nombre.isEmpty()) {
            throw new IllegalArgumentException("Nombre es requerido");
        }
        
        if (!email.contains("@")) {
            throw new IllegalArgumentException("Email debe tener formato válido");
        }
        
        // Validar role
        if (!role.equals("Admin") && !role.equals("User")) {
            logger.warn("Role '{}' no es válido, se asignará 'User'", role);
            role = "User";
        }
        
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID().toString());
        usuario.setEmail(email);
        usuario.setNombre(nombre);
        usuario.setNumeroEmpleado(numeroEmpleado);
        usuario.setRole(role);
        
        // Campo activo (opcional)
        if (csvLine.length > 4) {
            usuario.setActivo(csvLine[4] != null ? csvLine[4].trim() : "true");
        } else {
            usuario.setActivo("true");
        }
        
        return usuario;
    }

    /**
     * Genera un template CSV para importar usuarios
     * @return String con el contenido del template CSV
     */
    public String generateCsvTemplate() {
        StringBuilder template = new StringBuilder();
        template.append("email,nombre,numeroEmpleado,role,activo\n");
        template.append("usuario1@ejemplo.com,Juan Pérez,EMP001,User,true\n");
        template.append("admin@ejemplo.com,María González,EMP002,Admin,true\n");
        template.append("usuario2@ejemplo.com,Carlos López,EMP003,User,true\n");
        
        return template.toString();
    }

    /**
     * Método de debug para obtener todos los usuarios (solo para debug)
     * @return Lista de todos los usuarios en DynamoDB
     */
    public List<Usuario> getAllUsersForDebug() {
        try {
            logger.debug("Debug: Obteniendo todos los usuarios de DynamoDB");
            List<Usuario> usuarios = usuarioRepository.findAll();
            logger.debug("Debug: Se encontraron {} usuarios", usuarios.size());
            return usuarios;
        } catch (Exception e) {
            logger.error("Debug: Error obteniendo usuarios: {}", e.getMessage(), e);
            throw e;
        }
    }
}
