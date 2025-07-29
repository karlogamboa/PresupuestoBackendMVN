package com.cdc.fin.presupuesto.service;

import org.springframework.stereotype.Service;
import com.cdc.fin.presupuesto.repository.ScimUserRepository;
import com.cdc.fin.presupuesto.model.ScimUser;
import com.cdc.fin.presupuesto.model.ScimListResponse;
import com.fasterxml.jackson.core.JsonProcessingException;

@Service
public class ScimUserService {
    private final ScimUserRepository userRepository;

    public ScimUserService(ScimUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Crea usuario y retorna el objeto ScimUser
    public ScimUser createUser(ScimUser user) throws JsonProcessingException {
        return userRepository.createUser(user);
    }

    // Obtiene usuario por id
    public ScimUser getUser(String id) throws JsonProcessingException {
        return userRepository.getUser(id);
    }

    // Reemplaza usuario y retorna el objeto ScimUser
    public ScimUser replaceUser(String id, ScimUser user) throws JsonProcessingException {
        return userRepository.replaceUser(id, user);
    }

    // Aplica patch y retorna el objeto ScimUser
    public ScimUser patchUser(String id, ScimUser patch) throws JsonProcessingException {
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
}
