package com.bff.raissaRpa.service.impl;

import com.bff.raissaRpa.service.EncryptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Slf4j
@Service
public class EncryptionServiceImpl implements EncryptionService {
    private static final String ALGORITHM = "AES";
    private final SecretKeySpec secretKey;

    public EncryptionServiceImpl(@Value("${encryption.secret-key}") String secretKeyValue) {
        byte[] keyBytes = secretKeyValue.getBytes();
        if (keyBytes.length < 16) {
            byte[] paddedKey = new byte[16];
            System.arraycopy(keyBytes, 0, paddedKey, 0, Math.min(keyBytes.length, 16));
            this.secretKey = new SecretKeySpec(paddedKey, ALGORITHM);
        } else {
            this.secretKey = new SecretKeySpec(keyBytes, ALGORITHM);
        }
        log.info("Servicio de encriptación inicializado con clave de {} caracteres", keyBytes.length);
    }

    @Override
    public String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey);
            byte[] encryptedBytes = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encryptedBytes);
        } catch (Exception e) {
            log.error("Error al encriptar datos: {}", e.getMessage());
            throw new RuntimeException("Error al encriptar datos", e);
        }
    }

    @Override
    public String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey);
            byte[] decodedBytes = Base64.getDecoder().decode(encryptedData);
            byte[] decryptedBytes = cipher.doFinal(decodedBytes);
            return new String(decryptedBytes);
        } catch (Exception e) {
            log.error("Error al desencriptar datos: {}", e.getMessage());
            throw new RuntimeException("Error al desencriptar datos", e);
        }
    }

    @Override
    public String encryptKeyAccessData(String keyAccess, String secretAccess) {
        try {
            if (keyAccess == null || secretAccess == null) {
                throw new IllegalArgumentException("keyAccess y secretAccess no pueden ser nulos");
            }

            String combinedData = keyAccess + "|||" + secretAccess;
            log.debug("Encriptando datos combinados: {} caracteres", combinedData.length());
            return encrypt(combinedData);
        } catch (Exception e) {
            log.error("Error al encriptar keyAccess y secretAccess: {}", e.getMessage());
            throw new RuntimeException("Error al encriptar datos de acceso", e);
        }
    }

    @Override
    public String[] decryptKeyAccessData(String encryptedData) {
        try {
            if (encryptedData == null || encryptedData.trim().isEmpty()) {
                throw new IllegalArgumentException("encryptedData no puede ser nulo o vacío");
            }

            String decryptedData = decrypt(encryptedData);
            log.debug("Datos desencriptados: {} caracteres", decryptedData.length());

            String[] parts = decryptedData.split("\\|\\|\\|", 2);
            if (parts.length != 2) {
                throw new RuntimeException("Formato de datos desencriptados inválido");
            }

            return parts;
        } catch (Exception e) {
            log.error("Error al desencriptar keyAccess y secretAccess: {}", e.getMessage());
            throw new RuntimeException("Error al desencriptar datos de acceso", e);
        }
    }
}
