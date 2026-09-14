package com.github.nurbolotalt.bank_app.Loan;

import java.math.BigDecimal;
import java.time.Instant;

public record LoanResponse(
    Long id,
    String lenderAccountNumber,
    String borrowerAccountNumber,
    BigDecimal principalAmount,
    BigDecimal interestRate,
    BigDecimal totalOwed,
    BigDecimal remainingAmount,
    Instant dueDate,
    String status,
    Instant createdAt
) {

}