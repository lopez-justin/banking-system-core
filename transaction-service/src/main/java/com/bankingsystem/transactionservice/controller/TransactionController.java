package com.bankingsystem.transactionservice.controller;

import com.bankingsystem.transactionservice.dto.TransactionResponse;
import com.bankingsystem.transactionservice.dto.TransferRequest;
import com.bankingsystem.transactionservice.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/transactions")
@Slf4j
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(@Valid @RequestBody TransferRequest request) {
        log.info("POST Request received to transfer amount");
        return ResponseEntity.status(HttpStatus.CREATED).body(this.transactionService.transfer(request));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(@PathVariable String transactionId) {
        log.info("GET Request received to fetch transaction details");
        return ResponseEntity.ok(this.transactionService.getTransaction(transactionId));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionsByAccountNumber(@PathVariable String accountNumber) {
        log.info("GET Request received to fetch transaction history");
        return ResponseEntity.ok(this.transactionService.getTransactionHistory(accountNumber));
    }

    @PostMapping("/{transactionId}/verify-otp")
    public ResponseEntity<TransactionResponse> verifyOTP(@PathVariable String transactionId, @RequestParam String otp) {
        log.info("POST Request received to verify OTP");
        return ResponseEntity.ok(this.transactionService.verifyOTP(transactionId, otp));
    }

}
