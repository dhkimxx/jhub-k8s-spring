package com.dhkimxx.jhub_k8s_spring.util;

import io.kubernetes.client.custom.Quantity;

/**
 * 쿠버네티스 리소스 수량(Quantity) 문자열을 파싱하는 유틸리티.
 * CPU(milli-cores) 및 Memory(MiB) 단위 변환을 수행합니다.
 */
public final class ResourceQuantityParser {

    private ResourceQuantityParser() {
    }

    /**
     * CPU 문자열을 milli-core 단위로 변환합니다.
     * 예: "1" -> 1000.0, "500m" -> 500.0
     */
    public static double toMilliCores(String cpu) {
        if (cpu == null || cpu.isBlank()) {
            return 0;
        }
        String value = cpu.trim().toLowerCase();
        if (value.endsWith("n")) {
            return parse(value, "n", 1_000_000d);
        }
        if (value.endsWith("u")) {
            return parse(value, "u", 1_000d);
        }
        if (value.endsWith("m")) {
            return safeParseDouble(value.substring(0, value.length() - 1));
        }
        // plain cores
        return safeParseDouble(value) * 1000d;
    }

    /**
     * Quantity 객체에서 milli-core 값을 추출합니다.
     * Quantity.getNumber()는 기본 단위(cores)를 반환하므로 1000을 곱합니다.
     */
    public static double toMilliCores(Quantity quantity) {
        if (quantity == null) {
            return 0;
        }
        // getNumber() returns value in base units (cores)
        return quantity.getNumber().doubleValue() * 1000d;
    }

    /**
     * 메모리 문자열을 MiB 단위로 변환합니다.
     * 예: "1Gi" -> 1024.0, "512Mi" -> 512.0
     */
    public static double toMiB(String memory) {
        if (memory == null || memory.isBlank()) {
            return 0;
        }
        String value = memory.trim();
        if (value.endsWith("Ki")) {
            return parse(value, "Ki", 1 / 1024d);
        }
        if (value.endsWith("Mi")) {
            return safeParseDouble(value.substring(0, value.length() - 2));
        }
        if (value.endsWith("Gi")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d;
        }
        if (value.endsWith("Ti")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d * 1024d;
        }
        if (value.endsWith("Pi")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d * 1024d * 1024d;
        }
        if (value.endsWith("Ei")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d * 1024d * 1024d * 1024d;
        }
        if (value.endsWith("K")) {
            return parse(value, "K", 1 / 1024d);
        }
        if (value.endsWith("M")) {
            return safeParseDouble(value.substring(0, value.length() - 1));
        }
        if (value.endsWith("G")) {
            return safeParseDouble(value.substring(0, value.length() - 1)) * 1024d;
        }
        if (value.endsWith("T")) {
            return safeParseDouble(value.substring(0, value.length() - 1)) * 1024d * 1024d;
        }
        // assume bytes
        return safeParseDouble(value) / (1024d * 1024d);
    }

    /**
     * 메모리 문자열을 bytes 단위로 변환합니다.
     * 예: "1Gi" -> 1073741824.0, "512Mi" -> 536870912.0
     */
    public static double toBytes(String memory) {
        if (memory == null || memory.isBlank()) {
            return 0;
        }
        String value = memory.trim();
        if (value.endsWith("Ki")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d;
        }
        if (value.endsWith("Mi")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d * 1024d;
        }
        if (value.endsWith("Gi")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d * 1024d * 1024d;
        }
        if (value.endsWith("Ti")) {
            return safeParseDouble(value.substring(0, value.length() - 2)) * 1024d * 1024d * 1024d * 1024d;
        }
        if (value.endsWith("K")) {
            return safeParseDouble(value.substring(0, value.length() - 1)) * 1000d;
        }
        if (value.endsWith("M")) {
            return safeParseDouble(value.substring(0, value.length() - 1)) * 1000d * 1000d;
        }
        if (value.endsWith("G")) {
            return safeParseDouble(value.substring(0, value.length() - 1)) * 1000d * 1000d * 1000d;
        }
        if (value.endsWith("T")) {
            return safeParseDouble(value.substring(0, value.length() - 1)) * 1000d * 1000d * 1000d * 1000d;
        }
        // assume bytes
        return safeParseDouble(value);
    }

    /**
     * Quantity 객체에서 MiB 값을 추출합니다.
     * Quantity.getNumber()는 기본 단위(bytes)를 반환하므로 1024*1024로 나눕니다.
     */
    public static double toMiB(Quantity quantity) {
        if (quantity == null) {
            return 0;
        }
        // getNumber() returns value in base units (bytes)
        return quantity.getNumber().doubleValue() / (1024d * 1024d);
    }

    /**
     * Quantity 객체에서 bytes 값을 추출합니다.
     * toSuffixedString()으로 변환 후 문자열 파서를 통해 bytes로 변환합니다.
     */
    public static double toBytes(Quantity quantity) {
        if (quantity == null) {
            return 0;
        }
        return toBytes(quantity.toSuffixedString());
    }

    private static double parse(String value, String suffix, double divisor) {
        String number = value.substring(0, value.length() - suffix.length());
        return safeParseDouble(number) / divisor;
    }

    private static double safeParseDouble(String number) {
        try {
            return Double.parseDouble(number.trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }
}
