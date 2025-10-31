package com.bff.raissaRpa.service.impl;

import com.bff.raissaRpa.domain.dto.request.KeyCreateRequest;
import com.bff.raissaRpa.domain.dto.request.KeyUpdateRequest;
import com.bff.raissaRpa.domain.dto.response.KeyResponse;
import com.bff.raissaRpa.domain.entity.Key;
import com.bff.raissaRpa.domain.repository.KeyRepository;
import com.bff.raissaRpa.service.EncryptionService;
import com.bff.raissaRpa.service.KeyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class KeyServiceImpl implements KeyService {
    private final KeyRepository keyEntryRepository;
    private final EncryptionService encryptionService;

    @Override
    public KeyResponse createKey(KeyCreateRequest request) {
        try {
            validateKeyCreateRequest(request);

            String apiKey = encryptionService.encryptKeyAccessData(
                    request.getKeyAccess().trim(),
                    request.getSecretAccess().trim()
            );

            Key keyEntry = Key.builder()
                    .apiKey(apiKey)
                    .active((short) 1)
                    .createdBy(request.getCreatedBy())
                    .updatedBy(request.getCreatedBy())
                    .build();

            keyEntry = keyEntryRepository.save(keyEntry);
            log.info("API Key creada exitosamente: ID={}, API_KEY={}", keyEntry.getId(), keyEntry.getApiKey());

            return mapToResponse(keyEntry);

        } catch (DataIntegrityViolationException e) {
            log.error("Error de integridad al crear API Key: {}", e.getMessage());
            throw new RuntimeException("El API Key ya existe", e);
        } catch (Exception e) {
            log.error("Error al crear API Key: {}", e.getMessage());
            throw new RuntimeException("Error al crear API Key", e);
        }
    }

    @Override
    public KeyResponse updateKey(Integer id, KeyUpdateRequest request) {
        Key keyEntry = keyEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API Key no encontrada con ID: " + id));

        if (request.getKeyAccess() != null && request.getSecretAccess() != null) {
            String encryptedData = encryptionService.encryptKeyAccessData(
                    request.getKeyAccess().trim(),
                    request.getSecretAccess().trim()
            );
            keyEntry.setApiKey(encryptedData);
        } else if (request.getKeyAccess() != null || request.getSecretAccess() != null) {
            throw new RuntimeException("Debe proporcionar ambos key_access y secret_access para actualizar");
        }

        if (request.getActive() != null) {
            keyEntry.setActive(request.getActive());
        }

        if (request.getUpdatedBy() != null) {
            keyEntry.setUpdatedBy(request.getUpdatedBy());
        }

        keyEntry = keyEntryRepository.save(keyEntry);
        log.info("API Key actualizada: ID={}", id);

        return mapToResponse(keyEntry);
    }

    @Override
    public void deleteKey(Integer id) {
        Key keyEntry = keyEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API Key no encontrada con ID: " + id));

        keyEntryRepository.delete(keyEntry);
        log.info("API Key eliminada: ID={}", id);
    }

    @Override
    public KeyResponse getKeyById(Integer id) {
        Key keyEntry = keyEntryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("API Key no encontrada con ID: " + id));

        return mapToResponse(keyEntry);
    }

    @Override
    public KeyResponse getKeyByApiKey(String apiKey) {
        Key keyEntry = keyEntryRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("API Key no encontrada: " + apiKey));

        return mapToResponse(keyEntry);
    }

    @Override
    public List<KeyResponse> getAllKeys() {
        return keyEntryRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<KeyResponse> getActiveKeys() {
        return keyEntryRepository.findByActive((short) 1).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<KeyResponse> getInactiveKeys() {
        return keyEntryRepository.findByActive((short) 0).stream()
                .map(this::mapToResponse)
                .toList();
    }

    public KeyAccessData getKeyAccessData(String apiKey) {
        Key keyEntry = keyEntryRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("API Key no encontrada: " + apiKey));

        if (keyEntry.getApiKey() == null) {
            throw new RuntimeException("No hay datos encriptados para esta API Key");
        }

        try {
            String[] decryptedData = encryptionService.decryptKeyAccessData(keyEntry.getApiKey());
            return new KeyAccessData(decryptedData[0], decryptedData[1]);
        } catch (Exception e) {
            log.error("Error al desencriptar datos de la API Key {}: {}", apiKey, e.getMessage());
            throw new RuntimeException("Error al desencriptar datos de la API Key", e);
        }
    }

    private KeyResponse mapToResponse(Key keyEntry) {
        return KeyResponse.builder()
                .id(keyEntry.getId())
                .apiKey(keyEntry.getApiKey())
                .active(keyEntry.getActive())
                .createdAt(keyEntry.getCreatedAt())
                .createdBy(keyEntry.getCreatedBy())
                .updatedAt(keyEntry.getUpdatedAt())
                .updatedBy(keyEntry.getUpdatedBy())
                .build();
    }

    private void validateKeyCreateRequest(KeyCreateRequest request) {
        if (request.getKeyAccess() == null || request.getKeyAccess().trim().isEmpty()) {
            throw new RuntimeException("key_access es requerido");
        }
        if (request.getSecretAccess() == null || request.getSecretAccess().trim().isEmpty()) {
            throw new RuntimeException("secret_access es requerido");
        }
    }

    public static class KeyAccessData {
        private final String keyAccess;
        private final String secretAccess;

        public KeyAccessData(String keyAccess, String secretAccess) {
            this.keyAccess = keyAccess;
            this.secretAccess = secretAccess;
        }

        public String getKeyAccess() { return keyAccess; }
        public String getSecretAccess() { return secretAccess; }
    }
}
