package com.dhkimxx.jhub_k8s_spring.util;

import io.kubernetes.client.custom.Quantity;

public final class ResourceQuantityParser {

    private ResourceQuantityParser() {
    }

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

    public static double toMilliCores(Quantity quantity) {
        if (quantity == null) {
            return 0;
        }
        return toMilliCores(quantity.toSuffixedString());
    }

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

    public static double toMiB(Quantity quantity) {
        if (quantity == null) {
            return 0;
        }
        return toMiB(quantity.toSuffixedString());
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
