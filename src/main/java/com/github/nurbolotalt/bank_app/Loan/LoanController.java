package com.github.nurbolotalt.bank_app.Loan;

import com.github.nurbolotalt.bank_app.Account.Account;
import com.github.nurbolotalt.bank_app.Account.AccountService;
import com.github.nurbolotalt.bank_app.User.User;
import com.github.nurbolotalt.bank_app.User.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanService loanService;
    private final AccountService accountService;
    private final UserRepository userRepository;

    public LoanController(LoanService loanService, AccountService accountService, UserRepository userRepository) {
        this.loanService = loanService;
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public LoanResponse createLoan(@RequestBody CreateLoanRequest request, Authentication authentication) {
        String email = authentication.getName();
        Loan loan = loanService.createLoan(
                request.lenderAccountNumber(),
                request.borrowerAccountNumber(),
                request.principalAmount(),
                request.interestRate(),
                request.dueDate(),
                email
        );
        return toResponse(loan);
    }

    @PostMapping("/{loanId}/repay")
    public LoanResponse repayLoan(@PathVariable Long loanId, @RequestBody RepayLoanRequest request, Authentication authentication) {
        String email = authentication.getName();
        Loan loan = loanService.repayLoan(loanId, request.amount(), email);
        return toResponse(loan);
    }

    @GetMapping("/lent")
    public List<LoanResponse> myLentLoans(Authentication authentication) {
        Account account = getMyAccount(authentication);
        return loanService.getLoansAsLender(account.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/borrowed")
    public List<LoanResponse> myBorrowedLoans(Authentication authentication) {
        Account account = getMyAccount(authentication);
        return loanService.getLoansAsBorrower(account.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    private Account getMyAccount(Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        List<Account> accounts = accountService.getAccountsByUserId(user.getId());
        return accounts.get(0);
    }

    private LoanResponse toResponse(Loan loan) {
        return new LoanResponse(
                loan.getId(),
                loan.getLenderAccount().getAccountNumber(),
                loan.getBorrowerAccount().getAccountNumber(),
                loan.getPrincipalAmount(),
                loan.getInterestRate(),
                loan.getTotalOwed(),
                loan.getRemainingAmount(),
                loan.getDueDate(),
                loan.getStatus(),
                loan.getCreatedAt()
        );
    }
}