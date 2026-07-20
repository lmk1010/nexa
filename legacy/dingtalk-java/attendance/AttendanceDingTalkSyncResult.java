package com.kyx.service.hr.service.attendance;

import lombok.Data;

@Data
public class AttendanceDingTalkSyncResult {

    private int processedCount;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;

    public void addCreated() {
        this.processedCount++;
        this.createdCount++;
    }

    public void addUpdated() {
        this.processedCount++;
        this.updatedCount++;
    }

    public void addSkipped() {
        this.skippedCount++;
    }

}
