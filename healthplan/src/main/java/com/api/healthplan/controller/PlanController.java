package com.api.healthplan.controller;

import com.api.healthplan.exception.DataNotFoundException;
import com.api.healthplan.exception.NotModifiedException;
import com.api.healthplan.service.PlanService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Set;

@RestController
@RequestMapping("/healthplans")
public class PlanController {

    private final ObjectMapper objectMapper;
    private final PlanService planService;

    @Autowired
    public PlanController(ObjectMapper objectMapper, PlanService planService) {
        this.objectMapper = objectMapper;
        this.planService = planService;
    }

    @PostMapping
    public ResponseEntity<String> createHealthPlan(@RequestBody JsonNode healthPlanJson) {
       try {
    	// Validate the incoming JSON payload against the JSON schema
          Set<ValidationMessage> errorSet =  validateJsonSchema(healthPlanJson);
          
          
            if(errorSet.isEmpty()) {
            	// Convert the validated JSON to a string
                String healthPlanString = healthPlanJson.toString();

                // Check if the object already exists in Redis
                if (planService.healthPlanExists(healthPlanString)) {
                    return new ResponseEntity<>("Health Plan already exists", HttpStatus.CONFLICT);
                }
                
                // Create the health plan using the PlanService
                String planId = planService.createHealthPlan(healthPlanString);

                return new ResponseEntity<>("Health Plan created successfully with ID: " + planId, HttpStatus.CREATED);
            
            } else {
            	
            	return new ResponseEntity<>("{ \"error\":" + errorSet.toString() + "}", HttpStatus.BAD_REQUEST);
            }
               
		
	   } catch (Exception e) {
		// TODO: handle exception
		 return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);

	  }
    	
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> getHealthPlan(@PathVariable String id, @RequestHeader(value = "If-None-Match", required = false) String eTag) {
        String healthPlan;
        try {
            healthPlan = planService.getHealthPlan(id, eTag);
            String retrievedEtag = planService.getEtag(id); // Retrieve the ETag from the PlanService

            return ResponseEntity.status(HttpStatus.OK).header("ETag", retrievedEtag).body(healthPlan);
        } catch (NotModifiedException e) {
            return new ResponseEntity<>("Not Modified", HttpStatus.NOT_MODIFIED);
        } catch (DataNotFoundException e) {
            return new ResponseEntity<>("Health plan not found", HttpStatus.NOT_FOUND);
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteHealthPlan(@PathVariable String id) {
        // Perform delete operation or any other logic
        // Implement your business logic here
        boolean deleted = planService.deleteHealthPlan(id);
        if (deleted) {
            return new ResponseEntity<>("Health Plan deleted successfully", HttpStatus.OK);
        } else {
            return new ResponseEntity<>("Health Plan not found", HttpStatus.NOT_FOUND);
        }
    }


    
    private Set<ValidationMessage> validateJsonSchema(JsonNode jsonNode) throws IOException {
        
            // Load the JSON schema from the classpath resource
            InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("plan-schema.json");

            // Check if the schema file was found
            if (schemaStream == null) {
                throw new FileNotFoundException("plan-schema.json not found");
            }

            // Parse the JSON schema
            JsonNode schemaNode = objectMapper.readTree(schemaStream);
            JsonSchema schema = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4).getSchema(schemaNode);

            // Validate the JSON payload against the schema
            Set<ValidationMessage> errorSet = schema.validate(jsonNode);
            return errorSet;
        
    }
    

}
