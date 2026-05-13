package com.soldout.inventario.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class InventarioCacheService {

    private static final String PREFIX = "inventario:disponible:";

    private final RedisTemplate<String, String> redisTemplate;

    public InventarioCacheService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void guardarDisponibilidad(UUID seccionId, int disponibles) {
        redisTemplate.opsForValue().set(buildKey(seccionId), String.valueOf(disponibles));
    }

    public Integer obtenerDisponibilidad(UUID seccionId) {
        String value = redisTemplate.opsForValue().get(buildKey(seccionId));
        if (value == null) {
            return null;
        }
        return Integer.valueOf(value);
    }

    public void invalidarDisponibilidad(UUID seccionId) {
        redisTemplate.delete(buildKey(seccionId));
    }

    private String buildKey(UUID seccionId) {
        return PREFIX + seccionId;
    }
}
