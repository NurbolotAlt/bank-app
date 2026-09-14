package com.github.nurbolotalt.bank_app.LedgerEntry;

import com.github.nurbolotalt.bank_app.Account.Account;
import com.github.nurbolotalt.bank_app.Transaction.Transaction;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "ledger_entries")
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaction_id", nullable = false)
    private Transaction transaction;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "entry_type", nullable = false)
    private String entryType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LedgerEntry() {}

    public LedgerEntry(Account account, Transaction transaction, BigDecimal amount, String entryType) {
        this.account = account;
        this.transaction = transaction;
        this.amount = amount;
        this.entryType = entryType;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Account getAccount() { return account; }
    public Transaction getTransaction() { return transaction; }
    public BigDecimal getAmount() { return amount; }
    public String getEntryType() { return entryType; }
    public Instant getCreatedAt() { return createdAt; }
}