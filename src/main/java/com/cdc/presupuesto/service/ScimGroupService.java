package com.cdc.presupuesto.service;

import com.cdc.presupuesto.repository.ScimGroupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ScimGroupService {

    @Autowired
    private ScimGroupRepository groupRepository;

    public String listGroups() {
        return groupRepository.listGroups();
    }

    public String createGroup(String groupJson) {
        return groupRepository.createGroup(groupJson);
    }

    public String getGroup(String id) {
        return groupRepository.getGroup(id);
    }

    public String replaceGroup(String id, String groupJson) {
        return groupRepository.replaceGroup(id, groupJson);
    }

    public String patchGroup(String id, String patchJson) {
        return groupRepository.patchGroup(id, patchJson);
    }

    public void deleteGroup(String id) {
        groupRepository.deleteGroup(id);
    }

    // Todos los métodos SCIM para grupos están implementados.
}
