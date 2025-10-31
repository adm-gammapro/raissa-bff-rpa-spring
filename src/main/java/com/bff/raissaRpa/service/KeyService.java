package com.bff.raissaRpa.service;

import com.bff.raissaRpa.domain.dto.request.KeyCreateRequest;
import com.bff.raissaRpa.domain.dto.request.KeyUpdateRequest;
import com.bff.raissaRpa.domain.dto.response.KeyResponse;
import com.bff.raissaRpa.service.impl.KeyServiceImpl;

import java.util.List;

public interface KeyService {
    KeyResponse createKey(KeyCreateRequest request);
    KeyResponse updateKey(Integer id, KeyUpdateRequest request);
    void deleteKey(Integer id);
    KeyResponse getKeyById(Integer id);
    KeyResponse getKeyByApiKey(String apiKey);
    List<KeyResponse> getAllKeys();
    List<KeyResponse> getActiveKeys();
    List<KeyResponse> getInactiveKeys();
    KeyServiceImpl.KeyAccessData getKeyAccessData(String apiKey);
}
