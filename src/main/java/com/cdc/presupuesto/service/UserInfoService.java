package com.cdc.presupuesto.service;

import com.cdc.presupuesto.model.Usuario;
import com.cdc.presupuesto.repository.UsuarioRepository;
import com.cdc.presupuesto.util.UserAuthUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Servicio para manejo de información de usuarios
 * Actualizado para usar API Gateway Authorizer en lugar de JWT
 */
@Service
public class UserInfoService {

    private static final Logger logger = LoggerFactory.getLogger(UserInfoService.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    /**
     * Obtiene la información del usuario desde DynamoDB usando el email del contexto de autenticación
     * @return Información del usuario o información básica del contexto de autenticación
     */
    public Map<String, Object> getCurrentUserInfo() {
        try {
            String userEmail = UserAuthUtils.getCurrentUserEmail();
            String userId = UserAuthUtils.getCurrentUserId();
            String userName = UserAuthUtils.getCurrentUserName();
            
            if (userEmail == null || userEmail.trim().isEmpty()) {
                logger.warn("No se encontró email en el contexto de autenticación para usuario: {}", userId);
                // Devolver información básica del contexto de autenticación
                return Map.of(
                    "id", userId != null ? userId : "unknown",
                    "nombre", userName != null ? userName : "",
                    "email", userEmail != null ? userEmail : "",
                    "numEmpleado", userId != null ? userId : ""
                );
            }

            // Buscar el usuario en DynamoDB por email
            Optional<Usuario> usuarioOpt = usuarioRepository.findByEmail(userEmail);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                logger.info("Usuario encontrado en DynamoDB: {}", usuario.getEmail());
                
                return Map.of(
                    "id", userId,
                    "nombre", usuario.getNombre() != null ? usuario.getNombre() : "",
                    "email", usuario.getEmail(),
                    "numeroEmpleado", usuario.getNumeroEmpleado() != null ? usuario.getNumeroEmpleado() : "",
                    "role", usuario.getRole() != null ? usuario.getRole() : "",
                    "activo", usuario.getActivo() != null ? usuario.getActivo() : ""
                );
            } else {
                logger.warn("Usuario no encontrado en DynamoDB: {}", userEmail);
                // Retornar información básica del contexto de autenticación
                return Map.of(
                    "id", userId,
                    "nombre", userName != null ? userName : "",
                    "email", userEmail,
                    "numEmpleado", ""
                );
            }
        } catch (Exception e) {
            logger.error("Error al obtener información del usuario: {}", e.getMessage(), e);
            throw new RuntimeException("Error al obtener información del usuario", e);
        }
    }

    /**
     * Obtiene el email del usuario actual del contexto de autenticación
     * @return Email del usuario o null si no está disponible
     */
    public String getCurrentUserEmail() {
        return UserAuthUtils.getCurrentUserEmail();
    }

    /**
     * Obtiene el nombre de usuario del contexto de autenticación
     * @return Nombre del usuario o null si no está disponible
     */
    public String getCurrentUserName() {
        return UserAuthUtils.getCurrentUserName();
    }

    /**
     * Verifica si el usuario actual tiene rol de administrador
     * @return true si es administrador, false en caso contrario
     */
    public boolean isCurrentUserAdmin() {
        return UserAuthUtils.hasRole("ADMIN") || UserAuthUtils.hasRole("ADMINISTRATOR");
    }

    /**
     * Procesa archivo CSV de usuarios y los guarda en DynamoDB
     * @param file Archivo CSV con datos de usuarios
     * @return Mensaje de resultado del procesamiento
     */
    public String processUsersCSV(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío");
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVReader csvReader = new CSVReader(reader)) {

            String[] headers = csvReader.readNext();
            if (headers == null) {
                throw new IllegalArgumentException("El archivo CSV está vacío o no tiene encabezados");
            }

            // Log headers for debugging
            logger.info("Headers CSV: {}", Arrays.toString(headers));

            // Mapear headers a índices
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim().toLowerCase(), i);
            }

            List<Usuario> usuarios = new ArrayList<>();
            String[] line;
            int lineNumber = 1;

            while ((line = csvReader.readNext()) != null) {
                lineNumber++;
                try {
                    Usuario usuario = parseUsuarioFromCsv(line, headerMap);
                    if (usuario != null) {
                        usuarios.add(usuario);
                    }
                } catch (Exception e) {
                    logger.warn("Error procesando línea {}: {}", lineNumber, e.getMessage());
                }
            }

            // Guardar usuarios uno por uno
            if (!usuarios.isEmpty()) {
                int saved = 0;
                for (Usuario usuario : usuarios) {
                    try {
                        usuarioRepository.save(usuario);
                        saved++;
                    } catch (Exception e) {
                        logger.warn("Error guardando usuario {}: {}", usuario.getEmail(), e.getMessage());
                    }
                }
                logger.info("Guardados {} usuarios en DynamoDB", saved);
            }

            return String.format("Procesamiento completado. Usuarios procesados: %d", usuarios.size());

        } catch (IOException | CsvValidationException e) {
            logger.error("Error procesando archivo CSV: {}", e.getMessage(), e);
            throw new RuntimeException("Error procesando archivo CSV: " + e.getMessage(), e);
        }
    }

    private Usuario parseUsuarioFromCsv(String[] line, Map<String, Integer> headerMap) {
        try {
            Usuario usuario = new Usuario();

            // Obtener valores usando headerMap
            String email = getValueFromLine(line, headerMap, "email");
            if (email == null || email.trim().isEmpty()) {
                logger.warn("Línea sin email válido, omitiendo");
                return null;
            }

            usuario.setEmail(email.trim().toLowerCase());
            usuario.setNombre(getValueFromLine(line, headerMap, "nombre"));
            usuario.setNumeroEmpleado(getValueFromLine(line, headerMap, "numempleado"));
            

            return usuario;
        } catch (Exception e) {
            logger.warn("Error parseando usuario: {}", e.getMessage());
            return null;
        }
    }

    private String getValueFromLine(String[] line, Map<String, Integer> headerMap, String headerName) {
        Integer index = headerMap.get(headerName);
        if (index != null && index < line.length) {
            String value = line[index];
            return value != null ? value.trim() : "";
        }
        return "";
    }

    /**
     * Obtiene todos los usuarios
     * @return Lista de todos los usuarios
     */
    public List<Usuario> getAllUsers() {
        try {
            return usuarioRepository.findAll();
        } catch (Exception e) {
            logger.error("Error obteniendo todos los usuarios: {}", e.getMessage(), e);
            throw new RuntimeException("Error obteniendo usuarios", e);
        }
    }

    /**
     * Busca usuarios por email
     * @param email Email a buscar
     * @return Usuario que coincide con el email, o null si no se encuentra
     */
    public Usuario findUserByEmail(String email) {
        try {
            Optional<Usuario> usuario = usuarioRepository.findByEmail(email.toLowerCase().trim());
            return usuario.orElse(null);
        } catch (Exception e) {
            logger.error("Error buscando usuario por email {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Error buscando usuario", e);
        }
    }

    /**
     * Guarda un usuario
     * @param usuario Usuario a guardar
     * @return Usuario guardado
     */
    public Usuario saveUser(Usuario usuario) {
        try {
            if (usuario.getEmail() != null) {
                usuario.setEmail(usuario.getEmail().toLowerCase().trim());
            }
            return usuarioRepository.save(usuario);
        } catch (Exception e) {
            logger.error("Error guardando usuario: {}", e.getMessage(), e);
            throw new RuntimeException("Error guardando usuario", e);
        }
    }

    /**
     * Elimina un usuario por email
     * @param email Email del usuario a eliminar
     */
    public void deleteUserByEmail(String email) {
        try {
            usuarioRepository.deleteByEmail(email.toLowerCase().trim());
            logger.info("Usuario eliminado: {}", email);
        } catch (Exception e) {
            logger.error("Error eliminando usuario {}: {}", email, e.getMessage(), e);
            throw new RuntimeException("Error eliminando usuario", e);
        }
    }

    /**
     * Alias para processUsersCSV para compatibilidad
     * @param file Archivo CSV
     * @return Resultado del procesamiento
     */
    public String importUsuariosFromCsv(MultipartFile file) {
        return processUsersCSV(file);
    }

    /**
     * Alias para getAllUsers para compatibilidad
     * @return Lista de usuarios
     */
    public List<Usuario> getAllUsersForDebug() {
        return getAllUsers();
    }

    /**
     * Genera template CSV para usuarios
     * @return String con headers CSV
     */
    public String generateCsvTemplate() {
        return "email,nombre,numempleado";
    }
}
