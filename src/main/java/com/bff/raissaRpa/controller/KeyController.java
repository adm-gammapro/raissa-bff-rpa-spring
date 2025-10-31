package com.bff.raissaRpa.controller;

import com.bff.raissaRpa.domain.dto.request.KeyCreateRequest;
import com.bff.raissaRpa.domain.dto.request.KeyUpdateRequest;
import com.bff.raissaRpa.domain.dto.response.KeyResponse;
import com.bff.raissaRpa.service.KeyService;
import com.bff.raissaRpa.util.Constantes;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/keys")
@RequiredArgsConstructor
public class KeyController {
    private final KeyService keyService;

    @PostMapping
    public ResponseEntity<?> createKey(@Valid @RequestBody KeyCreateRequest request) {
        try {
            KeyResponse response = keyService.createKey(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    Constantes.KEY_SUCCESS, false,
                    Constantes.KEY_MESSAGE, e.getMessage()
            ));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateKey(@PathVariable Integer id,
                                       @Valid @RequestBody KeyUpdateRequest request) {
        try {
            KeyResponse response = keyService.updateKey(id, request);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    Constantes.KEY_SUCCESS, false,
                    Constantes.KEY_MESSAGE, e.getMessage()
            ));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteKey(@PathVariable Integer id) {
        try {
            keyService.deleteKey(id);
            return ResponseEntity.ok(Map.of(
                    Constantes.KEY_SUCCESS, true,
                    Constantes.KEY_MESSAGE, "API Key eliminada exitosamente"
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    Constantes.KEY_SUCCESS, false,
                    Constantes.KEY_MESSAGE, e.getMessage()
            ));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getKeyById(@PathVariable Integer id) {
        try {
            KeyResponse response = keyService.getKeyById(id);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constantes.KEY_SUCCESS, false,
                    Constantes.KEY_MESSAGE, e.getMessage()
            ));
        }
    }

    @GetMapping("/by-api-key/{apiKey}")
    public ResponseEntity<?> getKeyByApiKey(@PathVariable String apiKey) {
        try {
            KeyResponse response = keyService.getKeyByApiKey(apiKey);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    Constantes.KEY_SUCCESS, false,
                    Constantes.KEY_MESSAGE, e.getMessage()
            ));
        }
    }

    @GetMapping
    public ResponseEntity<List<KeyResponse>> getAllKeys() {
        List<KeyResponse> keys = keyService.getAllKeys();
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/active")
    public ResponseEntity<List<KeyResponse>> getActiveKeys() {
        List<KeyResponse> keys = keyService.getActiveKeys();
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/inactive")
    public ResponseEntity<List<KeyResponse>> getInactiveKeys() {
        List<KeyResponse> keys = keyService.getInactiveKeys();
        return ResponseEntity.ok(keys);
    }
}