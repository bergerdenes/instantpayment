package hu.bergerdenes.instantpayment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableRetry
public class InstantPaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(InstantPaymentApplication.class, args);
    }
}
