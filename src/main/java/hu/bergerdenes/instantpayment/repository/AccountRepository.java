package hu.bergerdenes.instantpayment.repository;

import hu.bergerdenes.instantpayment.model.Account;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, String> {}
