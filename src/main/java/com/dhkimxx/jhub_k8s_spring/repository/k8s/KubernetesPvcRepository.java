package com.dhkimxx.jhub_k8s_spring.repository.k8s;

import java.util.List;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import com.dhkimxx.jhub_k8s_spring.config.JhubK8sProperties;
import com.dhkimxx.jhub_k8s_spring.dto.session.PvcSummaryResponse;
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

/**
 * 쿠버네티스 PersistentVolumeClaim을 조회하는 리포지토리.
 * Pod에 연결된 PVC 정보를 조회합니다.
 */
@Repository
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "jhub.k8s", name = "enabled", havingValue = "true", matchIfMissing = true)
public class KubernetesPvcRepository {

    private final CoreV1Api coreV1Api;
    private final JhubK8sProperties properties;

    /**
     * Pod에 연결된 PVC 정보를 조회합니다.
     * Pod의 volumes에서 persistentVolumeClaim을 찾아 해당 PVC 정보를 반환합니다.
     */
    public Optional<PvcSummaryResponse> findPvcByPod(V1Pod pod) {
        if (pod == null || pod.getSpec() == null || pod.getSpec().getVolumes() == null) {
            return Optional.empty();
        }

        // Pod의 volumes에서 PVC 참조 찾기
        List<V1Volume> volumes = pod.getSpec().getVolumes();
        for (V1Volume volume : volumes) {
            if (volume.getPersistentVolumeClaim() != null) {
                String pvcName = volume.getPersistentVolumeClaim().getClaimName();
                return findPvcByName(pvcName);
            }
        }
        return Optional.empty();
    }

    /**
     * PVC 이름으로 PVC 정보를 조회합니다.
     */
    public Optional<PvcSummaryResponse> findPvcByName(String pvcName) {
        try {
            V1PersistentVolumeClaim pvc = coreV1Api.readNamespacedPersistentVolumeClaim(
                    pvcName,
                    properties.getNamespace(),
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
                accessModes,
                storageClassName,
                phase,
                volumeName);
    }
}
