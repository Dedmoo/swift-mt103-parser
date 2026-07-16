package com.mehmetserin.swift.model;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class Mt103Models {

    public record ParseRequest(@NotBlank String message) {
    }

    public record ParsedMt103(
            String senderReference,     // :20:
            String bankOperationCode,   // :23B:
            String valueDate,           // :32A: date (yyyy-MM-dd)
            String currency,            // :32A: currency
            BigDecimal amount,          // :32A: amount
            String orderingCustomer,    // :50K: / :50A:
            String beneficiaryCustomer, // :59:
            String remittanceInfo,      // :70:
            String detailsOfCharges) {  // :71A:
    }
}
