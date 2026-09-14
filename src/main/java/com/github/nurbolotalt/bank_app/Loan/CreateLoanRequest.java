package com.github.nurbolotalt.bank_app.Loan;

import java.math.BigDecimal;
import java.time.Instant;

public record CreateLoanRequest(String lenderAccountNumber, String borrowerAccountNumber,
                                  BigDecimal principalAmount, BigDecimal interestRate, Instant dueDate) {
}