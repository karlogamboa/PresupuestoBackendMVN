package com.cdc.fin.presupuesto.service;

import org.springframework.stereotype.Service;
import com.cdc.fin.presupuesto.repository.ScimUserRepository;
import com.cdc.fin.presupuesto.model.ScimUser;
import com.cdc.fin.presupuesto.model.ScimListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ScimUserService {
    // Actualiza usuario con atributos SAML
    public void updateUserWithSamlAttributes(String userName, java.util.Map<String, Object> samlAttrs) {
        userRepository.updateUserWithSamlAttributes(userName, samlAttrs);
    }
    private final ScimUserRepository userRepository;

    public ScimUserService(ScimUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Crea usuario y retorna el objeto ScimUser
    public ScimUser createUser(ScimUser user) throws JsonProcessingException {
        // Cambia para usar createUserFromJson si el objeto tiene schemas de extensión
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(user);
        return userRepository.createUserFromJson(json);
    }

    // Obtiene usuario por id
    public ScimUser getUser(String id) throws JsonProcessingException {
        return userRepository.getUser(id);
    }

    // Reemplaza usuario y retorna el objeto ScimUser
    public ScimUser replaceUser(String id, ScimUser user) throws JsonProcessingException {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScimUserService.class);
        logger.debug("[SCIM][ScimUserService] Entrando a replaceUser con id={}, user={}", id, user);
        return userRepository.replaceUser(id, user);
    }

    // Aplica patch y retorna el objeto ScimUser
    public ScimUser patchUser(String id, ScimUser patch) throws JsonProcessingException {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScimUserService.class);
        logger.debug("[SCIM][ScimUserService] Entrando a patchUser con id={}, patch={}", id, patch);
        return userRepository.patchUser(id, patch);
    }

    // Elimina usuario
    public void deleteUser(String id) {
        userRepository.deleteUser(id);
    }

    // Lista usuarios y retorna ScimListResponse, acepta filtro
    public ScimListResponse<ScimUser> listUsers(String filter) throws JsonProcessingException {
        return userRepository.listUsers(filter);
    }

    public ScimUser replaceUserFromJson(String id, String body) throws JsonProcessingException {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScimUserService.class);
        logger.debug("[SCIM][ScimUserService] Entrando a replaceUserFromJson con id={}, body={}", id, body);
        return userRepository.replaceUserFromJson(id, body);
    }

    public ScimUser createUserFromJson(String body) throws JsonProcessingException {
        org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(ScimUserService.class);
        logger.debug("[SCIM][ScimUserService] Entrando a createUserFromJson con body={}", body);
        return userRepository.createUserFromJson(body);
    }
}