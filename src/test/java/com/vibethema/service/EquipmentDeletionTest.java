package com.vibethema.service;

import com.vibethema.model.Weapon;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.*;

public class EquipmentDeletionTest {

    @TempDir
    Path tempDir;

    @Test
    public void testWeaponDeletion() throws IOException {
        String originalHome = System.getProperty("user.home");
        try {
            System.setProperty("user.home", tempDir.toString());
            EquipmentDataService service = new EquipmentDataService();
            
            Weapon w = new Weapon("test-id", "Test Weapon", Weapon.WeaponRange.CLOSE, Weapon.WeaponType.MORTAL, Weapon.WeaponCategory.MEDIUM);
            service.saveWeapon(w);
            
            Path filePath = EquipmentDataService.getWeaponsPath().resolve("test-id.json");
            assertTrue(Files.exists(filePath), "File should be created");

            service.deleteWeapon("test-id");
            assertFalse(Files.exists(filePath), "File should be deleted");
        } finally {
            System.setProperty("user.home", originalHome);
        }
    }
}
