package com.mehmetserin.swift.service;

import com.mehmetserin.swift.model.Mt103Models.ParsedMt103;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Mt103Parser {

    private static final Pattern TAG_PATTERN =
            Pattern.compile(":(\\d{2}[A-Z]?):(.*?)(?=(?:\\r?\\n:\\d{2}[A-Z]?:)|(?:\\r?\\n-)|\\z)",
                    Pattern.DOTALL);

    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");
    private static final Pattern FIELD_32A_PATTERN =
            Pattern.compile("^(\\d{6})([A-Z]{3})(\\d{1,15}(?:,\\d{1,2})?)$");
    private static final Pattern BASIC_FIELD_CHARSET =
            Pattern.compile("^[A-Za-z0-9/?:().,'+\\-{} \\r\\n]+$");
    private static final Pattern BIC_PATTERN =
            Pattern.compile("^[A-Z]{4}[A-Z]{2}[A-Z0-9]{2}(?:[A-Z0-9]{3})?$");
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "AED", "AUD", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP",
            "HKD", "HUF", "INR", "JPY", "MXN", "NOK", "NZD", "PLN", "QAR", "RON",
            "SAR", "SEK", "SGD", "TRY", "USD", "ZAR");
    private static final int MAX_REFERENCE_LENGTH = 16;
    private static final int MAX_PARTY_LINES = 4;
    private static final int MAX_FREE_TEXT_LINES = 4;
    private static final int MAX_LINE_LENGTH = 35;
    private static final int MAX_INSTITUTION_LINES = 2;

    public ParsedMt103 parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("Message is empty.");
        }

        String textBlock = extractTextBlock(rawMessage);
        Map<String, String> tags = extractTags(textBlock);

        requireTag(tags, "20");
        requireTag(tags, "32A");
        requireOneOf(tags, "59", "59A");
        validateCoreTags(tags);

        String field32A = tags.get("32A").trim();
        Field32A parsed32A = parse32A(field32A);

        String orderingCustomer = firstNonNull(tags.get("50K"), tags.get("50A"), tags.get("50F"));
        String beneficiary = firstNonNull(tags.get("59"), tags.get("59A"));

        return new ParsedMt103(
                clean(tags.get("20")),
                clean(tags.get("23B")),
                parsed32A.valueDate(),
                parsed32A.currency(),
                parsed32A.amount(),
                clean(orderingCustomer),
                clean(beneficiary),
                clean(tags.get("70")),
                clean(tags.get("71A")),
                clean(tags.get("52A")),
                clean(tags.get("57A")),
                clean(tags.get("72")));
    }

    private String extractTextBlock(String raw) {
        // If message contains SWIFT blocks, isolate block 4 {4:...-}
        int idx = raw.indexOf("{4:");
        if (idx >= 0) {
            int end = raw.indexOf("-}", idx);
            String block = end > idx ? raw.substring(idx + 3, end) : raw.substring(idx + 3);
            return block.trim();
        }
        return raw.trim();
    }

    private Map<String, String> extractTags(String textBlock) {
        var tags = new LinkedHashMap<String, String>();
        Matcher matcher = TAG_PATTERN.matcher(textBlock);
        while (matcher.find()) {
            String tag = matcher.group(1);
            if (tags.containsKey(tag)) {
                throw new IllegalArgumentException("Duplicate tag :" + tag + ": is not supported.");
            }
            tags.put(tag, matcher.group(2).trim());
        }
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("No SWIFT tags found in message.");
        }
        return tags;
    }

    private record Field32A(String valueDate, String currency, BigDecimal amount) {
    }

    private Field32A parse32A(String field) {
        Matcher m = FIELD_32A_PATTERN.matcher(field);
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Invalid :32A: field. Expected YYMMDDCCCAMOUNT with an optional comma decimal separator; received '" + field + "'.");
        }
        LocalDate date;
        try {
            date = LocalDate.parse(m.group(1), YYMMDD);
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException(
                    "Invalid :32A: value date '" + m.group(1) + "'. Expected a valid YYMMDD date.", exception);
        }
        String currency = m.group(2);
        String amountToken = m.group(3);
        if (!SUPPORTED_CURRENCIES.contains(currency)) {
            throw new IllegalArgumentException("Unsupported :32A: currency '" + currency + "' in this educational subset.");
        }
        validateAmountScale(currency, amountToken);
        String amountRaw = amountToken.replace(',', '.');
        BigDecimal amount = new BigDecimal(amountRaw);
        return new Field32A(date.toString(), currency, amount);
    }

    private void validateAmountScale(String currency, String amountToken) {
        int fractionDigits = amountToken.contains(",") ? amountToken.length() - amountToken.indexOf(',') - 1 : 0;
        int allowedFractionDigits = "JPY".equals(currency) ? 0 : 2;
        if (fractionDigits > allowedFractionDigits) {
            throw new IllegalArgumentException(
                    "Invalid :32A: amount scale for " + currency + ". This subset allows "
                            + allowedFractionDigits + " fraction digits.");
        }
    }

    private void requireTag(Map<String, String> tags, String tag) {
        if (!tags.containsKey(tag) || tags.get(tag).isBlank()) {
            throw new IllegalArgumentException("Mandatory tag :" + tag + ": is missing.");
        }
    }

    private void requireOneOf(Map<String, String> tags, String... tagOptions) {
        for (String tag : tagOptions) {
            if (tags.containsKey(tag) && !tags.get(tag).isBlank()) {
                return;
            }
        }
        throw new IllegalArgumentException("Mandatory beneficiary tag :59: or :59A: is missing.");
    }

    private void validateCoreTags(Map<String, String> tags) {
        validateReference(tags.get("20"));
        validateBankOperationCode(tags.get("23B"));
        validatePartyField(tags, "50K");
        validatePartyField(tags, "50A");
        validatePartyField(tags, "50F");
        validatePartyField(tags, "59");
        validatePartyField(tags, "59A");
        validateFreeTextField(tags, "70");
        validateCharges(tags.get("71A"));
        validateInstitution(tags, "52A");
        validateInstitution(tags, "57A");
        validateFreeTextField(tags, "72");
    }

    private void validateReference(String value) {
        if (value.length() > MAX_REFERENCE_LENGTH
                || !BASIC_FIELD_CHARSET.matcher(value).matches()
                || value.contains("\r")
                || value.contains("\n")) {
            throw new IllegalArgumentException(":20: must contain 1-" + MAX_REFERENCE_LENGTH + " basic characters on one line.");
        }
    }

    private void validateBankOperationCode(String value) {
        if (value != null && !value.matches("[A-Z]{4}")) {
            throw new IllegalArgumentException(":23B: must contain exactly four uppercase letters.");
        }
    }

    private void validatePartyField(Map<String, String> tags, String tag) {
        String value = tags.get(tag);
        if (value != null) {
            validateMultilineField(tag, value, MAX_PARTY_LINES);
        }
    }

    private void validateFreeTextField(Map<String, String> tags, String tag) {
        String value = tags.get(tag);
        if (value != null) {
            validateMultilineField(tag, value, MAX_FREE_TEXT_LINES);
        }
    }

    private void validateCharges(String value) {
        if (value != null && !value.matches("BEN|OUR|SHA")) {
            throw new IllegalArgumentException(":71A: must be one of BEN, OUR, or SHA.");
        }
    }

    private void validateInstitution(Map<String, String> tags, String tag) {
        String value = tags.get(tag);
        if (value == null) {
            return;
        }
        validateMultilineField(tag, value, MAX_INSTITUTION_LINES);
        String[] lines = value.split("\\R");
        String bic = lines[lines.length - 1].trim();
        if (!BIC_PATTERN.matcher(bic).matches()) {
            throw new IllegalArgumentException(":" + tag + ": institution BIC must be a valid uppercase BIC8 or BIC11.");
        }
    }

    private void validateMultilineField(String tag, String value, int maxLines) {
        if (value.isBlank() || !BASIC_FIELD_CHARSET.matcher(value).matches()) {
            throw new IllegalArgumentException(":" + tag + ": contains unsupported characters.");
        }
        String[] lines = value.split("\\R", -1);
        if (lines.length > maxLines) {
            throw new IllegalArgumentException(":" + tag + ": exceeds " + maxLines + " lines.");
        }
        for (String line : lines) {
            if (line.length() > MAX_LINE_LENGTH) {
                throw new IllegalArgumentException(":" + tag + ": line length must not exceed " + MAX_LINE_LENGTH + " characters.");
            }
        }
    }

    private String firstNonNull(String... values) {
        for (String v : values) {
            if (v != null && !v.isBlank()) {
                return v;
            }
        }
        return null;
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("\\r?\\n", " ").trim();
    }
}
