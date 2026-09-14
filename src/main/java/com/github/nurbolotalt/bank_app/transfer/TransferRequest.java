package com.github.nurbolotalt.bank_app.transfer;

import java.math.BigDecimal;

public record TransferRequest(String fromAccountNumber, String toAccountNumber, BigDecimal amount, String idempotencyKey) {
}