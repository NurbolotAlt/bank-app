package com.github.nurbolotalt.bank_app.transfer;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/transactions")
public class TransferController {
    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request, Authentication authentication) {
        String currentUserEmail = authentication.getName();

        transferService.transfer(
            request.fromAccountNumber(),
            request.toAccountNumber(),
            request.amount(),
            request.idempotencyKey(),
            currentUserEmail,
            currentUserEmail
        );
        return "Transfer successful";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestBody DepositRequest request, Authentication authentication) {
        String currentUserEmail = authentication.getName();

        transferService.deposit(
            request.accountNumber(),
            request.amount(),
            request.idempotencyKey(),
            currentUserEmail
        );
        return "Deposit successful";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestBody WithdrawRequest request, Authentication authentication) {
        String currentUserEmail = authentication.getName();

        transferService.withdraw(
            request.accountNumber(),
            request.amount(),
            request.idempotencyKey(),
            currentUserEmail
        );
        return "Withdraw successful";
    }
}