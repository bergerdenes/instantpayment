package hu.bergerdenes.instantpayment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import jakarta.inject.Inject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import hu.bergerdenes.instantpayment.dto.SendPaymentResult;
import hu.bergerdenes.instantpayment.dto.SuccessCode;
import hu.bergerdenes.instantpayment.model.Account;
import hu.bergerdenes.instantpayment.model.Transaction;
import hu.bergerdenes.instantpayment.repository.AccountRepository;
import hu.bergerdenes.instantpayment.repository.TransactionRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class PaymentServiceTest {

    @Inject
    private PaymentService paymentService;

    @MockitoBean
    private AccountRepository accountRepo;

    @MockitoBean
    private TransactionRepository transactionRepo;

    @BeforeEach
    void setup() {
        Account a1 = new Account();
        a1.setId("user1");
        a1.setBalance(BigDecimal.valueOf(1000));
        Account a2 = new Account();
        a2.setId("user2");
        a2.setBalance(BigDecimal.valueOf(500));
        when(accountRepo.findById("user1")).thenReturn(Optional.of(a1));
        when(accountRepo.findById("user2")).thenReturn(Optional.of(a2));
    }

    @Test
    void testSendPaymentSuccess() {
        String key = UUID.randomUUID().toString();
        SendPaymentResult result = paymentService.sendPayment("user1", "user2", BigDecimal.valueOf(100), key);
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        Account user1 = accountRepo.findById("user1").get();
        assertEquals(BigDecimal.valueOf(900), user1.getBalance());
        Account user2 = accountRepo.findById("user2").get();
        assertEquals(BigDecimal.valueOf(600), user2.getBalance());
        verify(accountRepo, times(1)).save(user1);
        verify(accountRepo, times(1)).save(user2);
        assertEquals(SuccessCode.SUCCESSFUL, result.successCode());
        assertEquals("Payment is processed.", result.message());
        verify(transactionRepo, times(1)).save(transactionCaptor.capture());
        Transaction transaction = transactionCaptor.getValue();
        assertEquals(key, transaction.getIdempotencyKey());
    }

    @Test
    void testSendPaymentInsufficientFunds() {
        SendPaymentResult result = paymentService.sendPayment("user2", "user1", BigDecimal.valueOf(1000), UUID.randomUUID().toString());
        assertEquals(SuccessCode.FAILED, result.successCode());
        assertEquals("Insufficient balance.", result.message());
        verify(accountRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

    @Test
    void testIdempotentSendPayment() {
        String key = UUID.randomUUID().toString();
        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);

        when(transactionRepo.findByIdempotencyKey(key)).thenReturn(Optional.empty());
        SendPaymentResult res1 = paymentService.sendPayment("user1", "user2", BigDecimal.valueOf(50), key);
        verify(transactionRepo, times(1)).save(transactionCaptor.capture());
        Transaction transaction = transactionCaptor.getValue();
        assertEquals(key, transaction.getIdempotencyKey());

        Mockito.reset(transactionRepo);
        when(transactionRepo.findByIdempotencyKey(key)).thenReturn(Optional.of(transaction));
        SendPaymentResult res2 = paymentService.sendPayment("user1", "user2", BigDecimal.valueOf(50), key);
        verify(transactionRepo, never()).save(any());

        assertEquals(SuccessCode.SUCCESSFUL, res1.successCode());
        assertEquals("Payment is processed.", res1.message());
        assertEquals(SuccessCode.SUCCESSFUL, res2.successCode());
        assertEquals("Payment is already processed.", res2.message());
        assertEquals(BigDecimal.valueOf(950), accountRepo.findById("user1").get().getBalance());
        assertEquals(BigDecimal.valueOf(550), accountRepo.findById("user2").get().getBalance());
    }

    @Test
    void testSendPaymentSenderNotFound() {
        String key = UUID.randomUUID().toString();
        SendPaymentResult result = paymentService.sendPayment("nonexistentSender", "user2", BigDecimal.valueOf(100), key);
        assertEquals(SuccessCode.FAILED, result.successCode());
        assertEquals("Sender account not found.", result.message());
        verify(accountRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

    @Test
    void testSendPaymentRecipientNotFound() {
        String key = UUID.randomUUID().toString();
        SendPaymentResult result = paymentService.sendPayment("user1", "nonexistentRecipient", BigDecimal.valueOf(100), key);
        assertEquals(SuccessCode.FAILED, result.successCode());
        assertEquals("Recipient account not found.", result.message());
        verify(accountRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

    @Test
    void testFallbackSendPayment() {
        String key = UUID.randomUUID().toString();
        SendPaymentResult result = paymentService.fallbackSendPayment("user1", "user2", BigDecimal.valueOf(100), key, new RuntimeException("Simulated failure"));
        assertEquals(SuccessCode.FAILED, result.successCode());
        assertEquals("Payment processing failed. Please try again later.", result.message());
        verify(accountRepo, never()).save(any());
        verify(transactionRepo, never()).save(any());
    }

}
