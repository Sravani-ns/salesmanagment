package com.vehicle.salesmanagement.service;


import com.vehicle.salesmanagement.domain.dto.apiresponse.UnifiedWorkflowResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String WORKFLOW_STATE_KEY_PREFIX = "workflow:state:";
    private static final String SIGNAL_KEY_PREFIX = "workflow:signal:";
    private static final long CACHE_TTL_MINUTES = 60; // Cache expiry time

    public void cacheWorkflowState(Long customerOrderId, UnifiedWorkflowResponse state) {
        String key = WORKFLOW_STATE_KEY_PREFIX + customerOrderId;
        try {
            redisTemplate.opsForValue().set(key, state, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Cached workflow state for order ID: {}", customerOrderId);
        } catch (Exception e) {
            log.error("Failed to cache workflow state for order ID: {}: {}", customerOrderId, e.getMessage());
        }
    }

    public UnifiedWorkflowResponse getWorkflowState(Long customerOrderId) {
        String key = WORKFLOW_STATE_KEY_PREFIX + customerOrderId;
        try {
            UnifiedWorkflowResponse state = (UnifiedWorkflowResponse) redisTemplate.opsForValue().get(key);
            if (state != null) {
                log.info("Retrieved workflow state from cache for order ID: {}", customerOrderId);
            }
            return state;
        } catch (Exception e) {
            log.error("Failed to retrieve workflow state for order ID: {}: {}", customerOrderId, e.getMessage());
            return null;
        }
    }

    public void cacheSignal(String signalType, Long customerOrderId, Object signalData) {
        String key = SIGNAL_KEY_PREFIX + signalType + ":" + customerOrderId;
        try {
            redisTemplate.opsForValue().set(key, signalData, CACHE_TTL_MINUTES, TimeUnit.MINUTES);
            log.info("Cached signal {} for order ID: {}", signalType, customerOrderId);
        } catch (Exception e) {
            log.error("Failed to cache signal {} for order ID: {}: {}", signalType, customerOrderId, e.getMessage());
        }
    }

    public Object getSignal(String signalType, Long customerOrderId) {
        String key = SIGNAL_KEY_PREFIX + signalType + ":" + customerOrderId;
        try {
            Object signalData = redisTemplate.opsForValue().get(key);
            if (signalData != null) {
                log.info("Retrieved signal {} for order ID: {}", signalType, customerOrderId);
            }
            return signalData;
        } catch (Exception e) {
            log.error("Failed to retrieve signal {} for order ID: {}: {}", signalType, customerOrderId, e.getMessage());
            return null;
        }
    }

    public void deleteSignal(String signalType, Long customerOrderId) {
        String key = SIGNAL_KEY_PREFIX + signalType + ":" + customerOrderId;
        try {
            redisTemplate.delete(key);
            log.info("Deleted signal {} for order ID: {}", signalType, customerOrderId);
        } catch (Exception e) {
            log.error("Failed to delete signal {} for order ID: {}: {}", signalType, customerOrderId, e.getMessage());
        }
    }
}