package com.bff.raissaRpa.service;

public interface EncryptionService {
    String encrypt(String data);
    String decrypt(String encryptedData);
    String encryptKeyAccessData(String keyAccess, String secretAccess);
    String[] decryptKeyAccessData(String encryptedData);
}
