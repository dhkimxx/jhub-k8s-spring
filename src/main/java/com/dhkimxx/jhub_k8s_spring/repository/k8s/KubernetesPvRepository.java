package com.dhkimxx.jhub_k8s_spring.repository.k8s;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.dhkimxx.jhub_k8s_spring.dto.storage.PvSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.exception.KubernetesClientException;
import com.dhkimxx.jhub_k8s_spring.util.ResourceQuantityParser;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1ObjectReference;
import io.kubernetes.client.openapi.models.V1PersistentVolume;
import io.kubernetes.client.openapi.models.V1PersistentVolumeList;
import io.kubernetes.client.openapi.models.V1PersistentVolumeSpec;
import io.kubernetes.client.openapi.models.V1PersistentVolumeStatus;
import lombok.RequiredArgsConstructor;

/**
 * 쿠버네티스 PersistentVolume을 조회하는 리포지토리.
 * 클러스터 전체의 PV 정보를 조회합니다.
 */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KubernetesPvRepository {

    private final CoreV1Api coreV1Api;

    /**
     * 클러스터의 모든 PV를 조회합니다.
     */
    public List<PvSummaryResponse> findAllPvs() {
        try {
            V1PersistentVolumeList pvList = coreV1Api.listPersistentVolume(
                    null, null, null, null, null, null, null, null, null, null, null);
            return pvList.getItems().stream()
                    .map(this::toPvSummary)
                    .toList();
        } catch (ApiException ex) {
            throw new KubernetesClientException("Failed to fetch PV list", ex);
        }
    }

    private PvSummaryResponse toPvSummary(V1PersistentVolume pv) {
        V1PersistentVolumeSpec spec = pv.getSpec();
        V1PersistentVolumeStatus status = pv.getStatus();

        String pvName = pv.getMetadata() != null ? pv.getMetadata().getName() : "unknown";

        double capacityBytes = 0.0;
        if (spec != null && spec.getCapacity() != null) {
            Quantity storage = spec.getCapacity().get("storage");
            if (storage != null) {
                capacityBytes = ResourceQuantityParser.toBytes(storage);
            }
        }

        List<String> accessModes = spec != null && spec.getAccessModes() != null
                ? spec.getAccessModes()
                : List.of();
        String reclaimPolicy = spec != null ? spec.getPersistentVolumeReclaimPolicy() : null;
        String storageClassName = spec != null ? spec.getStorageClassName() : null;
        String phase = status != null ? status.getPhase() : "Unknown";

        // ClaimRef: PVC와의 바인딩 정보
        String claimRef = null;
        if (spec != null && spec.getClaimRef() != null) {
            V1ObjectReference ref = spec.getClaimRef();
            claimRef = ref.getNamespace() + "/" + ref.getName();
        }

        return new PvSummaryResponse(
                pvName,
                capacityBytes,
                accessModes,
                reclaimPolicy,
                storageClassName,
                phase,
                claimRef);
    }
}
