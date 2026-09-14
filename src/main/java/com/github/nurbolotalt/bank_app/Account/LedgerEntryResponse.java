package com.github.nurbolotalt.bank_app.Account;

import java.math.BigDecimal;
import java.time.Instant;

public record LedgerEntryResponse(BigDecimal amount, String entryType, Instant createdAt) {
}