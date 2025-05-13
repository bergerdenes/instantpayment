package hu.bergerdenes.instantpayment.service;

import java.math.BigDecimal;
import java.util.Optional;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import hu.bergerdenes.instantpayment.dto.SendPaymentResult;
import hu.bergerdenes.instantpayment.dto.SuccessCode;
import hu.bergerdenes.instantpayment.model.Account;
import hu.bergerdenes.instantpayment.model.Transaction;
import hu.bergerdenes.instantpayment.repository.AccountRepository;
import hu.bergerdenes.instantpayment.repository.TransactionRepository;

@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);

    private final AccountRepository accountRepo;

    private final TransactionRepository transactionRepo;

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final boolean kafkaEnabled;

    public PaymentService(AccountRepository accountRepo, TransactionRepository transactionRepo,
                          KafkaTemplate<String, String> kafkaTemplate, @Value("${kafka.enabled}") boolean kafkaEnabled) {
        this.accountRepo = accountRepo;
        this.transactionRepo = transactionRepo;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaEnabled = kafkaEnabled;
    }

    @Transactional
    @Retry(name = "paymentService", fallbackMethod = "fallbackSendPayment")
    @CircuitBreaker(name = "paymentService", fallbackMethod = "fallbackSendPayment")
    public SendPaymentResult sendPayment(String senderId, String recipientId, BigDecimal amount, String idempotencyKey) {
        if (transactionRepo.findByIdempotencyKey(idempotencyKey).isPresent()) {
            LOGGER.info("Payment is already processed: idempotencyKey={}", idempotencyKey);
            return new SendPaymentResult(SuccessCode.SUCCESSFUL, "Payment is already processed.");
        }

        Optional<Account> senderOpt = accountRepo.findById(senderId);
        if (senderOpt.isEmpty()) {
            LOGGER.warn("Sender account not found: senderId={}", senderId);
            return new SendPaymentResult(SuccessCode.FAILED, "Sender account not found.");
        }

        Account sender = senderOpt.get();

        Optional<Account> recipientOpt = accountRepo.findById(recipientId);
        if (recipientOpt.isEmpty()) {
            LOGGER.warn("Recipient account not found: senderId={}", recipientId);
            return new SendPaymentResult(SuccessCode.FAILED, "Recipient account not found.");
        }

        Account recipient = recipientOpt.get();

        synchronized (senderId.intern()) {
            if (hasSufficientBalance(amount, sender)) {
                LOGGER.info("Insufficient balance: senderId={}, amount={}", senderId, amount);
                return new SendPaymentResult(SuccessCode.FAILED, "Insufficient balance.");
            }

            transferMoney(sender, recipient, amount);
            saveTransaction(senderId, recipientId, amount, idempotencyKey);
            sendToKafka(recipientId, amount);

            return new SendPaymentResult(SuccessCode.SUCCESSFUL, "Payment is processed.");
        }
    }

    private void sendToKafka(String recipientId, BigDecimal amount) {
        if (kafkaEnabled) {
            kafkaTemplate.send("transaction_notifications", recipientId, "You received: " + amount);
            LOGGER.debug("Kafka message sent: recipientId={}, amount={}", recipientId, amount);
        } else {
            LOGGER.debug("Kafka is disabled, not sending message: recipientId={}, amount={}", recipientId, amount);
        }
    }

    private void saveTransaction(String senderId, String recipientId, BigDecimal amount, String idempotencyKey) {
        Transaction tx = new Transaction();
        tx.setSenderId(senderId);
        tx.setRecipientId(recipientId);
        tx.setAmount(amount);
        tx.setIdempotencyKey(idempotencyKey);
        transactionRepo.save(tx);
        LOGGER.debug("Transaction saved: {}", tx);
    }

    private boolean hasSufficientBalance(BigDecimal amount, Account sender) {
        return sender.getBalance().compareTo(amount) < 0;
    }

    private void transferMoney(Account sender, Account recipient, BigDecimal amount) {
        sender.setBalance(sender.getBalance().subtract(amount));
        recipient.setBalance(recipient.getBalance().add(amount));

        accountRepo.save(sender);
        accountRepo.save(recipient);
        LOGGER.debug("Accounts balances updated: senderId={}, recipientId={}, amount={}", sender.getId(), recipient.getId(), amount);
    }

    public SendPaymentResult fallbackSendPayment(String senderId, String recipientId, BigDecimal amount, String idempotencyKey, Throwable ex) {
        LOGGER.error("Fallback triggered for payment: sender={}, recipient={}, amount={}, idempotencyKey={}, error={}",
                senderId, recipientId, amount, idempotencyKey, ex.getMessage());
        return new SendPaymentResult(SuccessCode.FAILED, "Payment processing failed. Please try again later.");
    }
}