package com.vibethema.service;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class SystemDataServiceTest {

    @Test
    void testIsCoreDataImported() {
        // This test verifies the service logic doesn't crash.
        // It relies on CharmDataService.getUserCharmsPath() which is hardcoded.
        SystemDataService service = new SystemDataService();
        assertDoesNotThrow(service::isCoreDataImported);
    }
}
