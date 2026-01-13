package com.dhkimxx.jhub_k8s_spring.repository.k8s;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.dhkimxx.jhub_k8s_spring.config.JhubK8sProperties;
import com.dhkimxx.jhub_k8s_spring.dto.session.PvcSummaryResponse;
import com.dhkimxx.jhub_k8s_spring.dto.session.StorageUsageResponse;
import com.dhkimxx.jhub_k8s_spring.exception.KubernetesClientException;
import com.dhkimxx.jhub_k8s_spring.util.ResourceQuantityParser;

import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.ApiException;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaim;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimList;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimSpec;
import io.kubernetes.client.openapi.models.V1PersistentVolumeClaimStatus;
import io.kubernetes.client.openapi.models.V1Pod;
import io.kubernetes.client.openapi.models.V1Volume;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 쿠버네티스 PersistentVolumeClaim을 조회하는 리포지토리.
 * Pod에 연결된 PVC 정보를 조회합니다.
 */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class KubernetesPvcRepository {

    private final CoreV1Api coreV1Api;
    private final JhubK8sProperties properties;

    /**
     * Pod에 연결된 PVC 정보를 조회합니다.
     * Pod의 volumes에서 persistentVolumeClaim을 찾아 해당 PVC 정보를 반환합니다.
     */
    /**
     * Pod의 스토리지 사용 정보를 조회합니다. (Ephemeral 우선, 없으면 PVC)
     */
    public StorageUsageResponse findStorageUsageByPod(V1Pod pod) {
        if (pod == null || pod.getSpec() == null) {
            return StorageUsageResponse.none();
        }

        // Priority 1: Ephemeral Storage Limit 확인
        if (pod.getSpec().getContainers() != null && !pod.getSpec().getContainers().isEmpty()) {
            if (pod.getSpec().getContainers().get(0).getResources() != null &&
                    pod.getSpec().getContainers().get(0).getResources().getLimits() != null) {
                Quantity ephemeralLimit = pod.getSpec().getContainers().get(0)
                        .getResources().getLimits().get("ephemeral-storage");
                if (ephemeralLimit != null) {
                    double limitBytes = ResourceQuantityParser.toBytes(ephemeralLimit);
                    return StorageUsageResponse.ephemeral(limitBytes);
                }
            }
        }

        // Priority 2: PVC 확인
        return findPvcByPod(pod)
                .map(pvc -> StorageUsageResponse.pvc(
                        pvc.capacityBytes(),
                        pvc.requestBytes(),
                        pvc.pvcName(),
                        pvc.storageClassName()))
                .orElse(StorageUsageResponse.none());
    }

    /**
     * Pod에 연결된 PVC 정보를 조회합니다.
     * Pod의 volumes에서 persistentVolumeClaim을 찾아 해당 PVC 정보를 반환합니다.
     */
    public Optional<PvcSummaryResponse> findPvcByPod(V1Pod pod) {
        if (pod == null || pod.getSpec() == null || pod.getSpec().getVolumes() == null) {
            return Optional.empty();
        }

        String pvcName = null;
        String namespace = pod.getMetadata() != null && pod.getMetadata().getNamespace() != null
                ? pod.getMetadata().getNamespace()
                : properties.getNamespace();

        List<V1Volume> volumes = pod.getSpec().getVolumes();

        for (V1Volume volume : volumes) {
            if (volume.getPersistentVolumeClaim() != null) {
                pvcName = volume.getPersistentVolumeClaim().getClaimName();
                break;
            }
        }

        if (pvcName != null) {
            return findPvcByName(pvcName, namespace);
        }
        return Optional.empty();
    }

    /**
     * PVC 이름으로 PVC 정보를 조회합니다. (기본 네임스페이스 사용)
     */
    public Optional<PvcSummaryResponse> findPvcByName(String pvcName) {
        return findPvcByName(pvcName, properties.getNamespace());
    }

    /**
     * PVC 이름과 네임스페이스로 PVC 정보를 조회합니다.
     */
    public Optional<PvcSummaryResponse> findPvcByName(String pvcName, String namespace) {
        try {
            V1PersistentVolumeClaim pvc = coreV1Api.readNamespacedPersistentVolumeClaim(
                    pvcName,
                    namespace,
                    null);
            return Optional.of(toPvcSummary(pvc));
        } catch (ApiException ex) {
            if (ex.getCode() == 404) {
                return Optional.empty();
            }
            throw new KubernetesClientException("Failed to fetch PVC: " + pvcName, ex);
        }
    }

    /**
     * 네임스페이스 내 모든 PVC를 조회합니다.
     */
    public List<PvcSummaryResponse> findAllPvcs() {
        try {
            V1PersistentVolumeClaimList pvcList = coreV1Api.listNamespacedPersistentVolumeClaim(
                    properties.getNamespace(),
                    null, null, null, null, null, null, null, null, null, null, null);
            return pvcList.getItems().stream()
                    .map(this::toPvcSummary)
                    .toList();
        } catch (ApiException ex) {
            throw new KubernetesClientException("Failed to fetch PVC list", ex);
        }
    }

    private PvcSummaryResponse toPvcSummary(V1PersistentVolumeClaim pvc) {
        V1PersistentVolumeClaimSpec spec = pvc.getSpec();
        V1PersistentVolumeClaimStatus status = pvc.getStatus();

        String pvcName = pvc.getMetadata() != null ? pvc.getMetadata().getName() : "unknown";
        String namespace = pvc.getMetadata() != null ? pvc.getMetadata().getNamespace() : properties.getNamespace();

        double capacityBytes = 0.0;
        if (status != null && status.getCapacity() != null) {
            Quantity storage = status.getCapacity().get("storage");
            if (storage != null) {
                capacityBytes = ResourceQuantityParser.toBytes(storage);
            }
        }

        double requestBytes = 0.0;
        if (spec != null && spec.getResources() != null && spec.getResources().getRequests() != null) {
            Quantity request = spec.getResources().getRequests().get("storage");
            if (request != null) {
                requestBytes = ResourceQuantityParser.toBytes(request);
            }
        }

        List<String> accessModes = spec != null && spec.getAccessModes() != null
                ? spec.getAccessModes()
                : List.of();
        String storageClassName = spec != null ? spec.getStorageClassName() : null;
        String phase = status != null ? status.getPhase() : "Unknown";
        String volumeName = spec != null ? spec.getVolumeName() : null;

        return new PvcSummaryResponse(
                pvcName,
                namespace,
                capacityBytes,
                requestBytes,
                accessModes,
                storageClassName,
                phase,
                volumeName);
    }
}
