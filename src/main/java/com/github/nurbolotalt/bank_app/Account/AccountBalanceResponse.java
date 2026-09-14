package com.github.nurbolotalt.bank_app.Account;

import java.math.BigDecimal;

public record AccountBalanceResponse(String accountNumber, String currency, BigDecimal balance) {
}