package com.bankingsystem.accountservice.controller;

import com.bankingsystem.accountservice.dto.AccountResponse;
import com.bankingsystem.accountservice.dto.CreateAccountRequest;
import com.bankingsystem.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@Slf4j
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        log.info("POST Request received to create a new account");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.accountService.createAccount(request));
    }

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccountByAccountNumber(@PathVariable String accountNumber) {
        log.info("GET Request received to fetch account details for account number: {}", accountNumber);
        return ResponseEntity.ok(this.accountService.getAccountByAccountNumber(accountNumber));
    }

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalanceByAccountNumber(@PathVariable String accountNumber) {
        log.info("GET Request received to fetch balance for account number: {}", accountNumber);
        return ResponseEntity.ok(this.accountService.getBalanceByAccountNumber(accountNumber));
    }

    @PutMapping("/{accountNumber}/block")
    public ResponseEntity<String> blockAccount(@PathVariable String accountNumber) {
        log.info("PUT Request received to block account with account number: {}", accountNumber);
        this.accountService.blockAccount(accountNumber);
        return ResponseEntity.ok("Account blocked successfully");
    }

    /**
     * SAGA STEP 1 - Deduct the amount from the source account.
     * This is the first step in the SAGA transaction for transferring money.
     * Called by the Transaction Service when a transfer request is initiated.
     * @param accountNumber The account number from which the amount will be deducted.
     * @param amount The amount to be deducted.
     * @return ResponseEntity with a success message.
     */
    @PutMapping("/{accountNumber}/deduct")
    public ResponseEntity<String> deductAmount(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        log.info("SAGA STEP 1 - Deducting amount: {} from account number: {}", amount, accountNumber);
        this.accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("Amount deducted successfully");
    }

    /**
     * SAGA STEP 4 - Compensate the deduction
     * This is the compensation step in the SAGA transaction for transferring money.
     * Called by the Transaction Service if:
     * - Fraud is detected in the transaction -> refund sender (undo step 1)
     * - The transaction is completed -> Credit receiver
     * @param accountNumber The account number to credit.
     * @param amount The amount to credit.
     * @return ResponseEntity with a success message.
     */
    @PutMapping("/{accountNumber}/credit")
    public ResponseEntity<String> creditBalance(@PathVariable String accountNumber, @RequestParam BigDecimal amount) {
        log.info("SAGA STEP 4 - Crediting amount: {} to account number: {}", amount, accountNumber);
        this.accountService.creditBalance(accountNumber, amount);
        return ResponseEntity.ok("Amount credited successfully");
    }

}
