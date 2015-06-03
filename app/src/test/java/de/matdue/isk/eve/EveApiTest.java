package de.matdue.isk.eve;

import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class EveApiTest {

    @Test
    public void testGetCharacterUrl() throws Exception {
        // Act
        String url = EveApi.getCharacterUrl("12345678", 128);

        // Assert
        assertEquals("https://image.eveonline.com/Character/12345678_128.jpg", url);
    }

    @Test
    public void testGetTypeUrl() throws Exception {
        // Act
        String url = EveApi.getTypeUrl("12345678", 128);

        // Assert
        assertEquals("https://image.eveonline.com/Type/12345678_128.png", url);
    }
}