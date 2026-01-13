package com.dhkimxx.jhub_k8s_spring.dto.cluster;

/**
 * 클러스터 노드 요약 정보 DTO.
 * 노드별 리소스 할당량, 요청량 및 파드 수 정보를 담습니다.
 */
public record ClusterNodeSummaryResponse(
                String nodeName,
                String status,
                String kubeletVersion,
                String osImage,
                double capacityCpuMilliCores,
                double allocatableCpuMilliCores,
                double requestedCpuMilliCores,
                double cpuUsagePercent,
                double capacityMemoryBytes,
                double allocatableMemoryBytes,
                double requestedMemoryBytes,
                double memoryUsagePercent,
                int runningPodCount) {
}
