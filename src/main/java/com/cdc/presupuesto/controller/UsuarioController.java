package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.Usuario;
import com.cdc.presupuesto.repository.UsuarioRepository;
import com.cdc.presupuesto.service.UserInfoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
@Tag(name = "Usuarios", description = "API para gestionar usuarios del sistema")
@SecurityRequirement(name = "JWT Authentication")
public class UsuarioController {

    private static final Logger logger = LoggerFactory.getLogger(UsuarioController.class);

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UserInfoService userInfoService;

    @GetMapping
    @Operation(summary = "Obtener todos los usuarios", description = "Obtiene la lista de todos los usuarios")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de usuarios obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<List<Usuario>> getAllUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        return ResponseEntity.ok(usuarios);
    }

    @GetMapping("/{email}")
    @Operation(summary = "Obtener usuario por email", description = "Obtiene un usuario específico por su email")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Usuario> getUsuario(@PathVariable String email) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        return usuario.map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    @Operation(summary = "Crear usuario", description = "Crea un nuevo usuario en el sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario creado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos de usuario inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Usuario> createUsuario(
            @Parameter(description = "Datos del nuevo usuario")
            @RequestBody Usuario usuario,
            @AuthenticationPrincipal Jwt jwt) {
        
        
        // Si no se especifica rol, asignar User por defecto
        if (usuario.getRole() == null || usuario.getRole().isEmpty()) {
            usuario.setRole("User");
        }
        
        // Si no se especifica activo, asignar true por defecto
        if (usuario.getActivo() == null || usuario.getActivo().isEmpty()) {
            usuario.setActivo("true");
        }
        
        Usuario savedUsuario = usuarioRepository.save(usuario);
        return ResponseEntity.ok(savedUsuario);
    }

    @PutMapping("/{email}")
    @Operation(summary = "Actualizar usuario", description = "Actualiza un usuario existente")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario actualizado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "400", description = "Datos de usuario inválidos"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Usuario> updateUsuario(
            @PathVariable String email,
            @Parameter(description = "Datos actualizados del usuario")
            @RequestBody Usuario usuario,
            @AuthenticationPrincipal Jwt jwt) {
        
        Optional<Usuario> existingUsuario = usuarioRepository.findByEmail(email);
        if (existingUsuario.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        // Mantener el email como clave primaria
        usuario.setEmail(email);
        
        
        Usuario updatedUsuario = usuarioRepository.save(usuario);
        return ResponseEntity.ok(updatedUsuario);
    }

    @DeleteMapping("/{email}")
    @Operation(summary = "Eliminar usuario", description = "Elimina un usuario del sistema")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Usuario eliminado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Usuario no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Map<String, String>> deleteUsuario(@PathVariable String email) {
        Optional<Usuario> usuario = usuarioRepository.findByEmail(email);
        if (usuario.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        usuarioRepository.deleteByEmail(email);
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado exitosamente"));
    }

    @GetMapping("/exists/{email}")
    @Operation(summary = "Verificar si existe usuario", description = "Verifica si existe un usuario con el email especificado")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Verificación completada"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    public ResponseEntity<Map<String, Boolean>> existsUsuario(@PathVariable String email) {
        boolean exists = usuarioRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of("exists", exists));
    }

    /**
     * Importar usuarios desde CSV (solo Admin)
     */
    @PostMapping("/import-csv")
    @Operation(summary = "Importar usuarios desde CSV", description = "Importa usuarios desde un archivo CSV (solo Admin)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Importación exitosa"),
        @ApiResponse(responseCode = "400", description = "Archivo inválido o errores en los datos"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Acceso denegado - Solo Admin")
    })
    public ResponseEntity<Map<String, Object>> importUsuariosFromCsv(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal Jwt jwt) {
        try {
            logger.info("Importando usuarios desde CSV: {}", file.getOriginalFilename() != null ? file.getOriginalFilename() : "archivo_sin_nombre.csv");
            
            // Verificar que el usuario es Admin
            if (!userInfoService.isAdmin(jwt)) {
                logger.warn("Usuario no autorizado para importar usuarios");
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "No autorizado - Solo Admin"));
            }
            
            // Validar archivo
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "Archivo vacío"));
            }
            
            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "El archivo debe ser CSV"));
            }
            
            // Importar usuarios
            Map<String, Object> resultado = userInfoService.importUsuariosFromCsv(file);
            
            if ((Boolean) resultado.get("success")) {
                return ResponseEntity.ok(resultado);
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(resultado);
            }
            
        } catch (Exception e) {
            logger.error("Error importando usuarios desde CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "Error interno del servidor"));
        }
    }

    /**
     * Descargar template CSV para importar usuarios
     */
    @GetMapping("/csv-template")
    @Operation(summary = "Descargar template CSV", description = "Descarga un template CSV para importar usuarios")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Template descargado exitosamente"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    public ResponseEntity<String> downloadCsvTemplate() {
        try {
            logger.info("Generando template CSV para usuarios");
            
            String csvTemplate = userInfoService.generateCsvTemplate();
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", "usuarios_template.csv");
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .body(csvTemplate);
                    
        } catch (Exception e) {
            logger.error("Error generando template CSV: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
