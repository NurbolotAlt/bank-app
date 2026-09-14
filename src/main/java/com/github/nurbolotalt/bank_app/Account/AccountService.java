package com.github.nurbolotalt.bank_app.Account;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;

import com.github.nurbolotalt.bank_app.LedgerEntry.LedgerEntry;
import com.github.nurbolotalt.bank_app.LedgerEntry.LedgerEntryRepository;

@Service
public class AccountService {
    private final AccountRepository accountRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    public AccountService(AccountRepository accountRepository, LedgerEntryRepository ledgerEntryRepository) {
        this.accountRepository = accountRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
    }

    public Account getAccountByNumber(String accountNumber) {
    return accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountNumber));
    }

    public List<Account> getAccountsByUserId(Long userId) {
    return accountRepository.findByUserId(userId);
    }
    
    public List<LedgerEntry> getLedgerEntriesByAccountId(Long accountId) {
        return ledgerEntryRepository.findByAccountId(accountId);
    }

    public BigDecimal calculateBalance(Long accountId) {
    List<LedgerEntry> entries = ledgerEntryRepository.findByAccountId(accountId);
    return entries.stream()
            .map(LedgerEntry::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Account saveAccount(Account account) {
        return accountRepository.save(account);
    }
}
