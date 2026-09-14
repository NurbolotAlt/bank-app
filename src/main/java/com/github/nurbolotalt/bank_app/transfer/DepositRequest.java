package com.github.nurbolotalt.bank_app.transfer;

import java.math.BigDecimal;

public record DepositRequest(String accountNumber, BigDecimal amount, String idempotencyKey) {
}