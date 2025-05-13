package hu.bergerdenes.instantpayment.controller;

import jakarta.validation.Valid;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import hu.bergerdenes.instantpayment.dto.SendPaymentRequest;
import hu.bergerdenes.instantpayment.dto.SendPaymentResult;
import hu.bergerdenes.instantpayment.dto.SuccessCode;
import hu.bergerdenes.instantpayment.service.PaymentService;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Instant Payments", description = "Send instant money transfers")
@RestControllerAdvice
public class PaymentController {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleException(Exception ex) {
        LOGGER.warn("Unhandled Exception:", ex);
        return ResponseEntity.internalServerError().body(ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        LOGGER.warn("{}", ex.getMessage());
        StringBuilder errors = new StringBuilder();
        ex.getBindingResult().getFieldErrors().forEach(err -> errors.append(err.getField())
                .append(" - ")
                .append(err.getDefaultMessage())
                .append("; "));
        return ResponseEntity.badRequest().body(new SendPaymentResult(SuccessCode.FAILED, "Validation error(s): " + errors));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArgument(IllegalArgumentException ex) {
        LOGGER.warn("Illegal argument: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(new SendPaymentResult(SuccessCode.FAILED, ex.getMessage()));
    }

    @Operation(summary = "Send payment", description = "Send instant payment from sender to recipient",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Payment sent successfully"),
                    @ApiResponse(responseCode = "400", description = "Bad request", content = @Content(schema = @Schema(implementation = String.class)))
            })
    @PostMapping("/send")
    public ResponseEntity<?> sendPayment(@RequestBody @Valid SendPaymentRequest request) {
        LOGGER.info("Incoming payment request from {} to {}, amount={} idempotencyKey={}",
                request.getSenderId(), request.getRecipientId(), request.getAmount(), request.getIdempotencyKey());
        SendPaymentResult result = paymentService.sendPayment(
                request.getSenderId(),
                request.getRecipientId(),
                request.getAmount(),
                request.getIdempotencyKey()
        );
        return parseResult(result);
    }

    private ResponseEntity<?> parseResult(SendPaymentResult result) {
        switch (result.successCode()) {
            case SUCCESSFUL -> {
                return ResponseEntity.ok(result);
            }
            case FAILED -> {
                return ResponseEntity.badRequest().body(result);
            }
            default -> {
                return ResponseEntity.internalServerError().body(result);
            }

        }
    }

}