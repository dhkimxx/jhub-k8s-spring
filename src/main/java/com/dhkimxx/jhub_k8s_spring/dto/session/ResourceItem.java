package com.dhkimxx.jhub_k8s_spring.dto.session;

public record ResourceItem(
        Double request,
        Double limit,
        Double usage) {
    public static ResourceItem of(Double request, Double limit, Double usage) {
        return new ResourceItem(request, limit, usage);
    }
}
