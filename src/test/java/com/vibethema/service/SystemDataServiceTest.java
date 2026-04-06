package com.vibethema.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SystemDataServiceTest {

    @Test
    void testIsCoreDataImported() {
        // This test verifies the service logic doesn't crash.
        // It relies on CharmDataService.getUserCharmsPath() which is hardcoded.
        SystemDataService service = new SystemDataService();
        assertDoesNotThrow(service::isCoreDataImported);
    }
}
