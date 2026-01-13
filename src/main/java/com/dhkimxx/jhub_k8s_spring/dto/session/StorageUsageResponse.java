package com.dhkimxx.jhub_k8s_spring.dto.session;

public record StorageUsageResponse(
        StorageType type, // "PVC", "Ephemeral", "None"
        double capacityBytes, // Total Capacity (Limit for Ephemeral)
        double requestBytes, // Requested Capacity
        String pvcName, // Only for PVC
        String storageClassName // Only for PVC
) {
    public static StorageUsageResponse none() {
        return new StorageUsageResponse(StorageType.NONE, 0, 0, null, null);
    }

    public static StorageUsageResponse ephemeral(double capacityBytes) {
        return new StorageUsageResponse(StorageType.EPHEMERAL, capacityBytes, 0, null, null);
    }

    public static StorageUsageResponse pvc(double capacityBytes, double requestBytes, String pvcName,
            String storageClassName) {
        return new StorageUsageResponse(StorageType.PVC, capacityBytes, requestBytes, pvcName, storageClassName);
    }
}
