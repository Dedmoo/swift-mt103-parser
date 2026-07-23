package com.mehmetserin.swift.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class Mt103Models {

    public record ParseRequest(
            @NotBlank(message = "message must not be blank")
            @Size(max = 35_000, message = "message must not exceed 35000 characters")
            String message) {
    }

    public record ParsedMt103(
            String senderReference,     // :20:
            String bankOperationCode,   // :23B:
            String valueDate,           // :32A: date (yyyy-MM-dd)
            String currency,            // :32A: currency
            BigDecimal amount,          // :32A: amount
            String orderingCustomer,    // :50K: / :50A: / :50F:
            String beneficiaryCustomer, // :59: / :59A:
            String remittanceInfo,      // :70:
            String detailsOfCharges,    // :71A:
            String orderingInstitution, // :52A:
            String accountWithInstitution, // :57A:
            String senderToReceiverInformation) { // :72:
    }
}
