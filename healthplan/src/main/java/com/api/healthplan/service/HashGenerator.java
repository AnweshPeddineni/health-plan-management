package com.api.healthplan.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.fasterxml.jackson.databind.JavaType;

public class HashGenerator {
    public static String getMd5(String input)
    {
        MessageDigest md;
		try {
			md = MessageDigest.getInstance("MD5");
			byte[] resultOfDigest = md.digest(input.getBytes(StandardCharsets.UTF_8));
			
			StringBuilder sb = new StringBuilder();
			for(byte b : resultOfDigest)
			{
				sb.append(String.format("%02x", b));
			}
			return sb.toString();
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			throw new RuntimeException(e);
		}
		
    }
 
}
