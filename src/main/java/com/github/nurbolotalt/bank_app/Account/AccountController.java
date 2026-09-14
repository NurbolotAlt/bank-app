package com.github.nurbolotalt.bank_app.Account;

import com.github.nurbolotalt.bank_app.LedgerEntry.LedgerEntry;
import com.github.nurbolotalt.bank_app.User.User;
import com.github.nurbolotalt.bank_app.User.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;


@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;
    private final UserRepository userRepository;

    public AccountController(AccountService accountService, UserRepository userRepository) {
        this.accountService = accountService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public List<AccountBalanceResponse> myAccounts(Authentication authentication) {
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Account> accounts = accountService.getAccountsByUserId(user.getId());

        return accounts.stream()
                .map(account -> new AccountBalanceResponse(
                        account.getAccountNumber(),
                        account.getCurrency(),
                        accountService.calculateBalance(account.getId())
                ))
                .toList();
    }

    @GetMapping("/{accountNumber}/history")
    public List<LedgerEntryResponse> getHistory(@PathVariable String accountNumber, Authentication authentication) {
        String email = authentication.getName();
        Account account = accountService.getAccountByNumber(accountNumber);
        if (!account.getUser().getEmail().equals(email)) {
            throw new IllegalArgumentException("You do not have access to this account's history");
        }

        List<LedgerEntry> entries = accountService.getLedgerEntriesByAccountId(account.getId());

        return entries.stream()
                .map(entry -> new LedgerEntryResponse(
                        entry.getAmount(),
                        entry.getEntryType(),
                        entry.getCreatedAt()
                ))
                .toList();
    }

    @PostMapping
    public AccountBalanceResponse createAccount(@RequestBody CreateAccountRequest request, Authentication authentication) {
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        String accountNumber = "ACC-" + java.util.UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        Account newAccount = new Account(user, accountNumber, request.currency());
        Account saved = accountService.saveAccount(newAccount);

        return new AccountBalanceResponse(saved.getAccountNumber(), saved.getCurrency(), java.math.BigDecimal.ZERO);
    }
}