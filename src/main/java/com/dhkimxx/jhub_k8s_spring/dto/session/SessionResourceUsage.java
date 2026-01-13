package com.dhkimxx.jhub_k8s_spring.dto.session;

public record SessionResourceUsage(
                ResourceItem cpu,
                ResourceItem memory,
                StorageUsageResponse storage) {
}
