package com.bankingsystem.accountservice.service;

import com.bankingsystem.accountservice.dto.AccountResponse;
import com.bankingsystem.accountservice.dto.CreateAccountRequest;
import com.bankingsystem.accountservice.entity.AccountEntity;
import com.bankingsystem.accountservice.entity.enums.AccountStatus;
import com.bankingsystem.accountservice.entity.enums.AccountType;
import com.bankingsystem.accountservice.repository.JpaAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountService {

    private final JpaAccountRepository accountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating a new account for email: {}", request.email());

        if (accountRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Account with email " + request.email() + " already exists.");
        }

        AccountEntity account = AccountEntity.builder()
                .accountNumber(this.generateAccountNumber())
                .accountOwnerName(request.accountOwnerName())
                .email(request.email())
                .phone(request.phone())
                .accountType(request.accountType())
                .status(AccountStatus.ACTIVE)
                .balance(request.initialDeposit())
                .dailyTransactionLimit(
                        request.accountType() == AccountType.SAVINGS
                        ? new BigDecimal("5000")
                        : new BigDecimal("10000")
                )
                .build();

        AccountEntity savedAccount = accountRepository.save(account);
        log.info("Account created successfully with account number: {}", savedAccount.getAccountNumber());

        return mapToResponse(savedAccount);
    }

    public AccountResponse getAccountByAccountNumber(String accountNumber) {
        log.info("Fetching account details for account number: {}", accountNumber);
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account with number " + accountNumber + " not found."));
        return mapToResponse(account);
    }

    public BigDecimal getBalanceByAccountNumber(String accountNumber) {
        log.info("Fetching balance for account number: {}", accountNumber);
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account with number " + accountNumber + " not found."));
        return account.getBalance();
    }

    public void blockAccount(String accountNumber) {
        log.info("Blocking account with account number: {}", accountNumber);
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account with number " + accountNumber + " not found."));
        account.setStatus(AccountStatus.BLOCKED);
        accountRepository.save(account);
        log.info("Account with account number: {} has been blocked.", accountNumber);
    }

    public void deductBalance(String accountNumber, BigDecimal amount) {
        log.info("Deducting amount: {} from account number: {}", amount, accountNumber);
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account with number " + accountNumber + " not found."));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account number: " + accountNumber + " is not active.");
        }

        if (account.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance in account number: " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.info("Amount: {} deducted successfully from account number: {}", amount, accountNumber);
    }

    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("Crediting amount: {} to account number: {}", amount, accountNumber);
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account with number " + accountNumber + " not found."));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new IllegalArgumentException("Account number: " + accountNumber + " is not active.");
        }

        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Amount: {} credited successfully to account number: {}", amount, accountNumber);
    }

    /**
     * Generates a unique 9-digit account number.
     * @return a string representing the unique account number.
     */
    private String generateAccountNumber() {
        String accountNumber;
        do {
            long number = this.secureRandom.nextLong(1_000_000_000L);
            accountNumber = String.format("%09d", number);
        } while (accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }

    private AccountResponse mapToResponse(AccountEntity account) {
        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getAccountOwnerName(),
                account.getEmail(),
                account.getPhone(),
                account.getAccountType().name(),
                account.getStatus().name(),
                account.getBalance().toString(),
                account.getDailyTransactionLimit().toString(),
                account.getCreatedAt()
        );
    }

}
