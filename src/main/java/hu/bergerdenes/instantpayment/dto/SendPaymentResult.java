package hu.bergerdenes.instantpayment.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.NOT_REQUIRED;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.Schema;

public record SendPaymentResult(
        @Schema(description = "Payment Sending Success Code", example = "SUCCESSFUL", requiredMode = REQUIRED)
        SuccessCode successCode,
        @Schema(description = "Payment Sending Result Message", example = "Payment success", requiredMode = NOT_REQUIRED)
        String message) {
}
