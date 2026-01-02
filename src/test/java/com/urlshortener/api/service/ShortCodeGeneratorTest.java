package com.urlshortener.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotEquals;;

@SpringBootTest
class ShortCodeGeneratorTest {
    
    @Autowired
    private ShortCodeGenerator generator;
    
    @Test
    void testBase62Encoding() {
        // Test encoding
        assertEquals("0", generator.encodeBase62(0));
        assertEquals("a", generator.encodeBase62(10));
        assertEquals("A", generator.encodeBase62(36));
        assertEquals("Z", generator.encodeBase62(61));
        
        // Test decoding
        assertEquals(0, generator.decodeBase62("0"));
        assertEquals(10, generator.decodeBase62("a"));
        assertEquals(36, generator.decodeBase62("A"));
        assertEquals(61, generator.decodeBase62("Z"));
    }
    
    @Test
    void testGenerateShortCode() {
        String code1 = generator.generateShortCode();
        String code2 = generator.generateShortCode();
        
        assertNotNull(code1);
        assertNotNull(code2);
        assertNotEquals(code1, code2); // Should be unique
    }
}
