package com.cdc.presupuesto.service;

import org.springframework.stereotype.Service;
import com.cdc.presupuesto.repository.ScimUserRepository;

@Service
public class ScimUserService {
    private final ScimUserRepository userRepository;

    public ScimUserService(ScimUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Todos los métodos SCIM para usuarios están implementados.

    public String createUser(String userJson) {
        return userRepository.createUser(userJson);
    }

    public String getUser(String id) {
        return userRepository.getUser(id);
    }

    public String replaceUser(String id, String userJson) {
        return userRepository.replaceUser(id, userJson);
    }

    public String patchUser(String id, String patchJson) {
        return userRepository.patchUser(id, patchJson);
    }

    public void deleteUser(String id) {
        userRepository.deleteUser(id);
    }

    public String listUsers() {
        return userRepository.listUsers();
    }
}
