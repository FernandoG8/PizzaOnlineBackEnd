package com.ecommerce.project.payload;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PaymentStatusUpdateDTO {
    @NotBlank(message = "Payment status is required")
    private String pgStatus;
    private String pgResponseMessage;
}