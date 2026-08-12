package com.yuegang.zhihui.inventory.application;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class InventoryMaintenanceJobTest {
    @Test
    void scheduledJobReleasesExpiredReservations() {
        var service = mock(InventoryMaintenanceService.class);
        new InventoryMaintenanceJob(service).releaseExpired();
        verify(service).releaseExpired();
    }
}
