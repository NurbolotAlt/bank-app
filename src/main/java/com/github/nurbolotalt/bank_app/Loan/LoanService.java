package com.github.nurbolotalt.bank_app.Loan;

import com.github.nurbolotalt.bank_app.Account.Account;
import com.github.nurbolotalt.bank_app.Account.AccountRepository;
import com.github.nurbolotalt.bank_app.transfer.TransferService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.List;

@Service
public class LoanService {
    private final LoanRepository loanRepository;
    private final AccountRepository accountRepository;
    private final TransferService transferService;

    public LoanService(LoanRepository loanRepository, AccountRepository accountRepository, TransferService transferService) {
        this.loanRepository = loanRepository;
        this.accountRepository = accountRepository;
        this.transferService = transferService;
    }

    public Loan createLoan(String lenderAccountNumber, String borrowerAccountNumber, BigDecimal principalAmount, BigDecimal interestRate, Instant dueDate, String lenderEmail) {
        Account lenderAccount = accountRepository.findByAccountNumber(lenderAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Lender account not found: " + lenderAccountNumber));
        if (!lenderAccount.getUser().getEmail().equals(lenderEmail)) {
            throw new IllegalArgumentException("You do not own this account");
        }
        Account borrowerAccount = accountRepository.findByAccountNumber(borrowerAccountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Borrower account not found: " + borrowerAccountNumber));

        Loan loan = new Loan(lenderAccount, borrowerAccount, principalAmount, interestRate, dueDate);
        loanRepository.save(loan);

        // Transfer the principal amount from lender to borrower
        String idempotencyKey = UUID.randomUUID().toString();
        transferService.transfer(lenderAccount.getAccountNumber(), borrowerAccount.getAccountNumber(), principalAmount, idempotencyKey, lenderEmail, lenderEmail);

        return loan;
    }

    public Loan repayLoan(Long loanId, BigDecimal repaymentAmount, String borrowerEmail) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));

        if (loan.getStatus().equals("PAID_OFF")) {
            throw new IllegalArgumentException("Loan has already been repaid");
        }

        if (repaymentAmount.compareTo(loan.getRemainingAmount()) > 0) {
            throw new IllegalArgumentException("Repayment amount exceeds remaining amount owed");
        }

        // Transfer the repayment amount from borrower to lender
        String idempotencyKey = UUID.randomUUID().toString();
        transferService.transfer(loan.getBorrowerAccount().getAccountNumber(), loan.getLenderAccount().getAccountNumber(), repaymentAmount, idempotencyKey, borrowerEmail, borrowerEmail);

        loan.applyPayment(repaymentAmount);
        loanRepository.save(loan);

        return loan;
    }

    public List<Loan> getLoansAsLender(Long accountId) {
        return loanRepository.findByLenderAccountId(accountId);
    }

    public List<Loan> getLoansAsBorrower(Long accountId) {
        return loanRepository.findByBorrowerAccountId(accountId);
    }

}