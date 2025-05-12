package com.ecommerce.project.model;

public enum PaymentStatus {
    PENDING("pending"),
    COMPLETED("succeeded"),
    CANCELLED("cancelled");

    private final String status;

    PaymentStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public static PaymentStatus fromString(String status) {
        if (status == null) return PENDING;

        String normalizedStatus = status.toLowerCase();

        return switch (normalizedStatus) {
            case "succeeded", "completed" -> COMPLETED;
            case "cancelled", "canceled" -> CANCELLED;
            default -> PENDING;
        };
    }
}
