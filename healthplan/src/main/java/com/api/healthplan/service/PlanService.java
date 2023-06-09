package com.api.healthplan.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.api.healthplan.exception.DataNotFoundException;
import com.api.healthplan.exception.NotModifiedException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Set;

@Service
public class PlanService {

    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public PlanService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String createHealthPlan(String healthPlanJson) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();

            // Convert the healthPlanJson string to a JsonNode object
            JsonNode healthPlanNode = objectMapper.readTree(healthPlanJson);

            // Generate an ID for the health plan using the healthPlanNode
            String id = generateId(healthPlanNode);

            String hash = HashGenerator.getMd5(healthPlanJson);

            // Create a Redis hash map with the ID as the key and store both the object and the ETag
            redisTemplate.opsForHash().put(id, "object", healthPlanJson);
            redisTemplate.opsForHash().put(id, "ETag", hash);

            return id;
        } catch (IOException e) {
            e.printStackTrace();
            // Handle the exception appropriately (e.g., throw a custom exception or return an error message)
            return null;
        }
    }


    public String getHealthPlan(String id, String ETag) {
        String eTag = (String) redisTemplate.opsForHash().get(id, "ETag");

        if (ETag != null && ETag.equals(eTag)) {
            throw new NotModifiedException();
        }

        if (eTag != null) {
            return (String) redisTemplate.opsForHash().get(id, "object");
        }

        throw new DataNotFoundException();
    }


    public String getEtag(String id) {
        return (String) redisTemplate.opsForHash().get(id, "ETag");
    }

    public boolean deleteHealthPlan(String id) {
        redisTemplate.delete(id);
        redisTemplate.delete("ETag-" + id); // Delete the corresponding ETag entry
        return true;
    }

    private String generateId(JsonNode healthPlanJson) {
        // Extract the objectId from the JSON object
        String objectId = healthPlanJson.path("objectId").asText();
        
        // Use the objectId as the ID for the health plan
        return objectId;
    }
    
    public boolean healthPlanExists(String healthPlanString) {
        // Retrieve the keys from Redis to check for existing health plans
        Set<String> keys = redisTemplate.keys("*");

        // Iterate over the keys and compare the stored health plan objects
        for (String key : keys) {
            String storedHealthPlanString = (String) redisTemplate.opsForHash().get(key, "object");

            // Compare the string representation of the health plans
            if (healthPlanString.equals(storedHealthPlanString)) {
                return true; // Health plan already exists
            }
        }

        return false; // Health plan does not exist
    }

}
