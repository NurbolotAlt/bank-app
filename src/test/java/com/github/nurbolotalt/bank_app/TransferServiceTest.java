package com.github.nurbolotalt.bank_app;

import com.github.nurbolotalt.bank_app.Account.Account;
import com.github.nurbolotalt.bank_app.Account.AccountRepository;
import com.github.nurbolotalt.bank_app.Account.AccountService;
import com.github.nurbolotalt.bank_app.LedgerEntry.LedgerEntry;
import com.github.nurbolotalt.bank_app.LedgerEntry.LedgerEntryRepository;
import com.github.nurbolotalt.bank_app.User.User;
import com.github.nurbolotalt.bank_app.User.UserRepository;
import com.github.nurbolotalt.bank_app.transfer.TransferService;
import com.github.nurbolotalt.bank_app.Transaction.Transaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class TransferServiceTest extends AbstractIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AccountService accountService;

    @Autowired
    private LedgerEntryRepository ledgerEntryRepository;

    @Test
    void concurrentTransfersShouldNotAllowNegativeBalance() throws InterruptedException {
        User senderUser = userRepository.save(new User("sender@test.com", "hash"));
        User receiver1User = userRepository.save(new User("receiver1@test.com", "hash"));
        User receiver2User = userRepository.save(new User("receiver2@test.com", "hash"));

        Account senderAccount = accountRepository.save(new Account(senderUser, "SENDER-ACC", "USD"));
        Account receiver1Account = accountRepository.save(new Account(receiver1User, "RECEIVER1-ACC", "USD"));
        Account receiver2Account = accountRepository.save(new Account(receiver2User, "RECEIVER2-ACC", "USD"));

        transferService.deposit(
                senderAccount.getAccountNumber(),
                BigDecimal.valueOf(100),
                UUID.randomUUID().toString(),
                senderUser.getEmail()
        );

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        Runnable makeTransferTask = null; // просто для наглядности, ниже два конкретных задания

        Runnable task1 = () -> {
            try {
                startLatch.await();
                transferService.transfer(
                        senderAccount.getAccountNumber(),
                        receiver1Account.getAccountNumber(),
                        BigDecimal.valueOf(80),
                        UUID.randomUUID().toString(),
                        senderUser.getEmail(),
                        senderUser.getEmail()
                );
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        Runnable task2 = () -> {
            try {
                startLatch.await();
                transferService.transfer(
                        senderAccount.getAccountNumber(),
                        receiver2Account.getAccountNumber(),
                        BigDecimal.valueOf(80),
                        UUID.randomUUID().toString(),
                        senderUser.getEmail(),
                        senderUser.getEmail()
                );
                successCount.incrementAndGet();
            } catch (Exception e) {
                failureCount.incrementAndGet();
            }
        };

        executor.submit(task1);
        executor.submit(task2);

        // даём обоим потокам время дойти до startLatch.await()
        Thread.sleep(200);
        startLatch.countDown(); // открываем шлагбаум — оба потока стартуют одновременно

        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.SECONDS);

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(failureCount.get()).isEqualTo(1);
    }

    @Test
    void duplicateIdempotencyKeyShouldNotCreateSecondTransaction() {
        User senderUser = userRepository.save(new User("idem-sender@test.com", "hash"));
        User receiverUser = userRepository.save(new User("idem-receiver@test.com", "hash"));

        Account senderAccount = accountRepository.save(new Account(senderUser, "IDEM-SENDER", "USD"));
        Account receiverAccount = accountRepository.save(new Account(receiverUser, "IDEM-RECEIVER", "USD"));

        transferService.deposit(senderAccount.getAccountNumber(), BigDecimal.valueOf(100),
                UUID.randomUUID().toString(), senderUser.getEmail());

        String sharedKey = UUID.randomUUID().toString();

        Transaction first = transferService.transfer(
                senderAccount.getAccountNumber(), receiverAccount.getAccountNumber(),
                BigDecimal.valueOf(50), sharedKey, senderUser.getEmail(), senderUser.getEmail());

        Transaction second = transferService.transfer(
                senderAccount.getAccountNumber(), receiverAccount.getAccountNumber(),
                BigDecimal.valueOf(50), sharedKey, senderUser.getEmail(), senderUser.getEmail());

        assertThat(first.getId()).isEqualTo(second.getId());

        BigDecimal receiverBalance = accountService.calculateBalance(receiverAccount.getId());
        assertThat(receiverBalance).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void transferFromAccountNotOwnedByUserShouldBeRejected() {
        User ownerUser = userRepository.save(new User("owner@test.com", "hash"));
        User attackerUser = userRepository.save(new User("attacker@test.com", "hash"));
        User receiverUser = userRepository.save(new User("receiver-own@test.com", "hash"));

        Account ownerAccount = accountRepository.save(new Account(ownerUser, "OWNER-ACC", "USD"));
        Account receiverAccount = accountRepository.save(new Account(receiverUser, "RECEIVER-OWN-ACC", "USD"));

        transferService.deposit(ownerAccount.getAccountNumber(), BigDecimal.valueOf(100),
                UUID.randomUUID().toString(), ownerUser.getEmail());

        assertThatThrownBy(() -> transferService.transfer(
                ownerAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                BigDecimal.valueOf(50),
                UUID.randomUUID().toString(),
                attackerUser.getEmail(),  // ownerCheckEmail — НЕ владелец счёта
                attackerUser.getEmail()
        )).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("do not own");

        BigDecimal ownerBalance = accountService.calculateBalance(ownerAccount.getId());
        assertThat(ownerBalance).isEqualByComparingTo(BigDecimal.valueOf(100));
    }

    @Test
    void transferExceedingBalanceShouldBeRejected() {
        User senderUser = userRepository.save(new User("poor-sender@test.com", "hash"));
        User receiverUser = userRepository.save(new User("poor-receiver@test.com", "hash"));

        Account senderAccount = accountRepository.save(new Account(senderUser, "POOR-SENDER", "USD"));
        Account receiverAccount = accountRepository.save(new Account(receiverUser, "POOR-RECEIVER", "USD"));

        transferService.deposit(senderAccount.getAccountNumber(), BigDecimal.valueOf(50),
                UUID.randomUUID().toString(), senderUser.getEmail());

        assertThatThrownBy(() -> transferService.transfer(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                BigDecimal.valueOf(100), // больше, чем есть на счёте (50)
                UUID.randomUUID().toString(),
                senderUser.getEmail(),
                senderUser.getEmail()
        )).isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Insufficient funds");

        BigDecimal senderBalance = accountService.calculateBalance(senderAccount.getId());
        assertThat(senderBalance).isEqualByComparingTo(BigDecimal.valueOf(50));
    }

    @Test
    void everyTransactionShouldHaveBalancedLedgerEntries() {
        User senderUser = userRepository.save(new User("balance-sender@test.com", "hash"));
        User receiverUser = userRepository.save(new User("balance-receiver@test.com", "hash"));

        Account senderAccount = accountRepository.save(new Account(senderUser, "BALANCE-SENDER", "USD"));
        Account receiverAccount = accountRepository.save(new Account(receiverUser, "BALANCE-RECEIVER", "USD"));

        transferService.deposit(senderAccount.getAccountNumber(), BigDecimal.valueOf(200),
                UUID.randomUUID().toString(), senderUser.getEmail());

        Transaction transaction = transferService.transfer(
                senderAccount.getAccountNumber(),
                receiverAccount.getAccountNumber(),
                BigDecimal.valueOf(75),
                UUID.randomUUID().toString(),
                senderUser.getEmail(),
                senderUser.getEmail()
        );

        List<LedgerEntry> entries = ledgerEntryRepository.findByTransactionId(transaction.getId());

        BigDecimal sum = entries.stream()
                .map(LedgerEntry::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        assertThat(entries).hasSize(2);
        assertThat(sum).isEqualByComparingTo(BigDecimal.ZERO);
    }
}