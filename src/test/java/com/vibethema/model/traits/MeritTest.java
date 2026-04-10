package com.vibethema.model.traits;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class MeritTest {
    @Test
    public void testMeritInitialization() {
        Merit merit = new Merit("unique-id", "Artifact", 3);
        assertEquals("unique-id", merit.getId());
        assertEquals("Artifact", merit.getName());
        assertEquals(3, merit.getRating());
    }

    @Test
    public void testPropertyBinding() {
        Merit merit = new Merit("id", "Name", 1);
        merit.setName("New Name");
        assertEquals("New Name", merit.getName());
        assertEquals("New Name", merit.nameProperty().get());

        merit.setRating(5);
        assertEquals(5, merit.getRating());
        assertEquals(5, merit.ratingProperty().get());
    }
}
