package com.cdc.presupuesto.controller;

import com.cdc.presupuesto.model.Subdepartamento;
import com.cdc.presupuesto.repository.SubdepartamentoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/subdepartamentos")
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:4200"})
public class SubdepartamentoController {

    @Autowired
    private SubdepartamentoRepository subdepartamentoRepository;

    @GetMapping
    public ResponseEntity<List<Subdepartamento>> getSubdepartamentosByAreaId(@RequestParam String areaId,
                                                                             @AuthenticationPrincipal Jwt jwt) {
        List<Subdepartamento> subdepartamentos = subdepartamentoRepository.findByAreaId(areaId);
        return ResponseEntity.ok(subdepartamentos);
    }

    @GetMapping("/all")
    public ResponseEntity<List<Subdepartamento>> getAllSubdepartamentos(@AuthenticationPrincipal Jwt jwt) {
        List<Subdepartamento> subdepartamentos = subdepartamentoRepository.findAll();
        return ResponseEntity.ok(subdepartamentos);
    }

    @GetMapping("/{areaId}/{id}")
    public ResponseEntity<Subdepartamento> getSubdepartamentoById(@PathVariable String areaId, 
                                                                 @PathVariable String id,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        Optional<Subdepartamento> subdepartamento = subdepartamentoRepository.findById(areaId, id);
        return subdepartamento.map(ResponseEntity::ok)
                             .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Subdepartamento> createSubdepartamento(@RequestBody Subdepartamento subdepartamento,
                                                                 @AuthenticationPrincipal Jwt jwt) {
        if (subdepartamento.getId() == null || subdepartamento.getId().isEmpty()) {
            subdepartamento.setId(UUID.randomUUID().toString());
        }
        Subdepartamento created = subdepartamentoRepository.save(subdepartamento);
        return ResponseEntity.ok(created);
    }

    @PutMapping("/{areaId}/{id}")
    public ResponseEntity<Subdepartamento> updateSubdepartamento(@PathVariable String areaId,
                                                                @PathVariable String id, 
                                                                @RequestBody Subdepartamento subdepartamento,
                                                                @AuthenticationPrincipal Jwt jwt) {
        subdepartamento.setAreaId(areaId);
        subdepartamento.setId(id);
        Subdepartamento updated = subdepartamentoRepository.save(subdepartamento);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{areaId}/{id}")
    public ResponseEntity<Void> deleteSubdepartamento(@PathVariable String areaId, @PathVariable String id,
                                                      @AuthenticationPrincipal Jwt jwt) {
        subdepartamentoRepository.deleteById(areaId, id);
        return ResponseEntity.noContent().build();
    }
}
