package com.github.nurbolotalt.bank_app.transfer;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.context.annotation.Lazy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import com.github.nurbolotalt.bank_app.Account.Account;
import com.github.nurbolotalt.bank_app.Account.AccountRepository;
import com.github.nurbolotalt.bank_app.Transaction.TransactionRepository;



import com.github.nurbolotalt.bank_app.LedgerEntry.LedgerEntry;
import com.github.nurbolotalt.bank_app.LedgerEntry.LedgerEntryRepository;
import com.github.nurbolotalt.bank_app.Transaction.Transaction;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.List;


@Service
public class TransferService {
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private LedgerEntryRepository ledgerEntryRepository;
    private TransferService self;

    private static final BigDecimal MIN_BALANCE_FOR_FRAUD_CHECK = BigDecimal.valueOf(1000);
    private static final BigDecimal SUSPICIOUS_RATIO_THRESHOLD = BigDecimal.valueOf(0.9);
    private static final int MAX_TRANSACTIONS_PER_WINDOW = 5;
    private static final Duration FRAUD_CHECK_WINDOW = Duration.ofMinutes(10);
    private static final String EXTERNAL_ACCOUNT_NUMBER = "EXTERNAL";
    private static final String SYSTEM_EMAIL = "system@bank.internal";

    public TransferService(AccountRepository accountRepository,
                        TransactionRepository transactionRepository,
                        LedgerEntryRepository ledgerEntryRepository,
                        @Lazy TransferService self)
                        {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.ledgerEntryRepository = ledgerEntryRepository;
        this.self = self;
    }
    @Transactional
    public Transaction transfer(String fromAccountNumber, String toAccountNumber, BigDecimal amount,
                             String idempotencyKey, String ownerCheckEmail, String initiatedByEmail) {
        Optional<Transaction> existing = transactionRepository.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) return existing.get();

        if(fromAccountNumber.equals(toAccountNumber)) {
            throw new IllegalArgumentException("Cannot transfer to the same account");
        }

        Optional<Account> fromAccountOpt = accountRepository.findWithLockByAccountNumber(fromAccountNumber);
        Optional<Account> toAccountOpt = accountRepository.findByAccountNumber(toAccountNumber);

        if (fromAccountOpt.isEmpty() || toAccountOpt.isEmpty()) {
            throw new IllegalArgumentException("One or both accounts not found");
        }

        Account fromAccount = fromAccountOpt.get();
        if (!fromAccount.getUser().getEmail().equals(ownerCheckEmail)) {
            throw new IllegalArgumentException("You do not own this account");
        }
        Account toAccount = toAccountOpt.get();

        BigDecimal fromBalance = calculateBalance(fromAccount.getId());
        if (fromBalance.compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient funds in the source account");
        }

        checkForSuspiciousActivity(amount, fromBalance);
        checkTransactionFrequency(fromAccount.getId(), fromAccountNumber);


        Transaction transaction = new Transaction(idempotencyKey, "TRANSFER", initiatedByEmail);
        transaction = transactionRepository.save(transaction);

        LedgerEntry debitEntry = new LedgerEntry(fromAccount, transaction, amount.negate(), "DEBIT");
        LedgerEntry creditEntry = new LedgerEntry(toAccount, transaction, amount, "CREDIT");

        ledgerEntryRepository.save(debitEntry);
        ledgerEntryRepository.save(creditEntry);

        transaction.markCompleted();
        transaction = transactionRepository.save(transaction);

        return transaction;
    }
    
        
    private void checkForSuspiciousActivity(BigDecimal amount, BigDecimal fromBalance) {
        if (fromBalance.compareTo(MIN_BALANCE_FOR_FRAUD_CHECK) <= 0) {
            return; // счёт слишком мал, чтобы применять это правило
        }

        BigDecimal ratio = amount.divide(fromBalance, 4, RoundingMode.HALF_UP);

        if (ratio.compareTo(SUSPICIOUS_RATIO_THRESHOLD) > 0) {
            throw new IllegalArgumentException("Transfer amount exceeds 90% of the source account balance");
        }
    }

    private void checkTransactionFrequency(Long accountId, String accountNumber) {
        if (EXTERNAL_ACCOUNT_NUMBER.equals(accountNumber)) {
            return; // не применяем rate-limit к служебному счёту
        }

        Instant windowStart = Instant.now().minus(FRAUD_CHECK_WINDOW);
        List<LedgerEntry> recentEntries = ledgerEntryRepository.findByAccountIdAndCreatedAtAfter(accountId, windowStart);

        if (recentEntries.size() > MAX_TRANSACTIONS_PER_WINDOW) {
            throw new IllegalArgumentException("Too many transactions in a short period");
        }
    }

    private BigDecimal calculateBalance(Long accountId) {
        List<LedgerEntry> entries = ledgerEntryRepository.findByAccountId(accountId);
        return entries.stream()
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Transaction deposit(String accountNumber, BigDecimal amount, String idempotencyKey, String currentUserEmail) {
        return self.transfer(EXTERNAL_ACCOUNT_NUMBER, accountNumber, amount, idempotencyKey, SYSTEM_EMAIL, currentUserEmail);
    }

    public Transaction withdraw(String accountNumber, BigDecimal amount, String idempotencyKey, String currentUserEmail) {
        return self.transfer(accountNumber, EXTERNAL_ACCOUNT_NUMBER, amount, idempotencyKey, currentUserEmail, currentUserEmail);
    }

}