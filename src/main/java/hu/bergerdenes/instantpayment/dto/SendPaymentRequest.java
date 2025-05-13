package hu.bergerdenes.instantpayment.dto;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

public class SendPaymentRequest {

    @Schema(description = "Sender's account ID", example = "user1", requiredMode = REQUIRED)
    @NotBlank
    private String senderId;

    @Schema(description = "Recipient's account ID", example = "user2", requiredMode = REQUIRED)
    @NotBlank
    private String recipientId;

    @Schema(description = "Amount to transfer", example = "100.00", requiredMode = REQUIRED)
    @DecimalMin(value = "0.01", message = "Amount to send has to be positive")
    private BigDecimal amount;

    @Schema(description = "Unique idempotency key", example = "a1b2c3d4", requiredMode = REQUIRED)
    @NotBlank
    private String idempotencyKey;

    public String getSenderId() {
        return senderId;
    }

    public void setSenderId(String senderId) {
        this.senderId = senderId;
    }

    public String getRecipientId() {
        return recipientId;
    }

    public void setRecipientId(String recipientId) {
        this.recipientId = recipientId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    @Override
    public String toString() {
        return "SendPaymentRequest{" +
                "senderId='" + senderId + '\'' +
                ", recipientId='" + recipientId + '\'' +
                ", amount=" + amount +
                ", idempotencyKey='" + idempotencyKey + '\'' +
                '}';
    }
}