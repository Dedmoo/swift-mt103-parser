package com.mehmetserin.swift.service;

import com.mehmetserin.swift.model.Mt103Models.ParsedMt103;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class Mt103Parser {

    private static final Pattern TAG_PATTERN =
            Pattern.compile(":(\\d{2}[A-Z]?):(.*?)(?=(?:\\r?\\n:\\d{2}[A-Z]?:)|(?:\\r?\\n-)|\\z)",
                    Pattern.DOTALL);

    private static final DateTimeFormatter YYMMDD = DateTimeFormatter.ofPattern("yyMMdd");

    public ParsedMt103 parse(String rawMessage) {
        if (rawMessage == null || rawMessage.isBlank()) {
            throw new IllegalArgumentException("Message is empty.");
        }

        String textBlock = extractTextBlock(rawMessage);
        Map<String, String> tags = extractTags(textBlock);

        requireTag(tags, "20");
        requireTag(tags, "32A");
        requireTag(tags, "59");

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
                clean(tags.get("71A")));
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
            tags.put(matcher.group(1), matcher.group(2).trim());
        }
        if (tags.isEmpty()) {
            throw new IllegalArgumentException("No SWIFT tags found in message.");
        }
        return tags;
    }

    private record Field32A(String valueDate, String currency, BigDecimal amount) {
    }

    private Field32A parse32A(String field) {
        // Format: 6!n (date) 3!a (currency) 15d (amount with comma decimal)
        Matcher m = Pattern.compile("^(\\d{6})([A-Z]{3})([\\d,\\.]+)$").matcher(field.replaceAll("\\s", ""));
        if (!m.matches()) {
            throw new IllegalArgumentException("Invalid :32A: field: " + field);
        }
        LocalDate date = LocalDate.parse(m.group(1), YYMMDD);
        String currency = m.group(2);
        String amountRaw = m.group(3).replace(".", "").replace(',', '.');
        BigDecimal amount = new BigDecimal(amountRaw);
        return new Field32A(date.toString(), currency, amount);
    }

    private void requireTag(Map<String, String> tags, String tag) {
        if (!tags.containsKey(tag) || tags.get(tag).isBlank()) {
            throw new IllegalArgumentException("Mandatory tag :" + tag + ": is missing.");
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
