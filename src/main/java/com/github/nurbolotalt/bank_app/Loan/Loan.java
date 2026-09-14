package com.github.nurbolotalt.bank_app.Loan;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.github.nurbolotalt.bank_app.Account.Account;
import jakarta.persistence.*;

@Entity
@Table(name = "loans")
public class Loan {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lender_account_id", nullable = false)
    private Account lenderAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "borrower_account_id", nullable = false)
    private Account borrowerAccount;

    @Column(name = "principal_amount", nullable = false)
    private BigDecimal principalAmount;

    @Column(name = "interest_rate", nullable = false)
    private BigDecimal interestRate;

    @Column(name = "total_owed", nullable = false)
    private BigDecimal totalOwed;

    @Column(name = "remaining_amount", nullable = false)
    private BigDecimal remainingAmount;

    @Column(name = "due_date", nullable = false)
    private Instant dueDate;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected Loan() {}

    public Loan(Account lenderAccount, Account borrowerAccount, BigDecimal principalAmount,
                BigDecimal interestRate, Instant dueDate) {
        this.lenderAccount = lenderAccount;
        this.borrowerAccount = borrowerAccount;
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;

        BigDecimal interestAmount = principalAmount.multiply(interestRate).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        this.totalOwed = principalAmount.add(interestAmount);
        this.remainingAmount = this.totalOwed;

        this.dueDate = dueDate;
        this.status = "ACTIVE";
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Account getLenderAccount() { return lenderAccount; }
    public Account getBorrowerAccount() { return borrowerAccount; }
    public BigDecimal getPrincipalAmount() { return principalAmount; }
    public BigDecimal getInterestRate() { return interestRate; }
    public BigDecimal getTotalOwed() { return totalOwed; }
    public BigDecimal getRemainingAmount() { return remainingAmount; }
    public Instant getDueDate() { return dueDate; }
    public String getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }

    public void applyPayment(BigDecimal amount) {
        this.remainingAmount = this.remainingAmount.subtract(amount);
        if (this.remainingAmount.compareTo(BigDecimal.ZERO) <= 0) {
            this.status = "PAID_OFF";
        }
    }
}
