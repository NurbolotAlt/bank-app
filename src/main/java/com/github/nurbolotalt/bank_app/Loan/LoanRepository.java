package com.github.nurbolotalt.bank_app.Loan;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByLenderAccountId(Long lenderAccountId);
    List<Loan> findByBorrowerAccountId(Long borrowerAccountId);
}