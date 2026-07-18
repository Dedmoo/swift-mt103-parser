package com.mehmetserin.swift;

import com.mehmetserin.swift.model.Mt103Models.ParsedMt103;
import com.mehmetserin.swift.service.Mt103Parser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mt103ParserTest {

    private final Mt103Parser parser = new Mt103Parser();

    private static final String SAMPLE = """
            {1:F01BANKTRISAXXX0000000000}{2:I103BANKDEFFXXXXN}{4:
            :20:REF1234567890
            :23B:CRED
            :32A:260720EUR12345,67
            :50K:/TR000000000000000000000000
            ACME EXPORT LTD
            ISTANBUL
            :59:/DE00000000000000000000
            MUELLER GMBH
            BERLIN
            :70:INVOICE 2026-42
            :71A:SHA
            -}
            """;

    @Test
    void parse_extractsCoreFields() {
        ParsedMt103 parsed = parser.parse(SAMPLE);
        assertEquals("REF1234567890", parsed.senderReference());
        assertEquals("CRED", parsed.bankOperationCode());
        assertEquals("2026-07-20", parsed.valueDate());
        assertEquals("EUR", parsed.currency());
        assertEquals(0, parsed.amount().compareTo(new BigDecimal("12345.67")));
        assertEquals("SHA", parsed.detailsOfCharges());
    }

    @Test
    void parse_extractsPartyLines() {
        ParsedMt103 parsed = parser.parse(SAMPLE);
        assertTrue(parsed.orderingCustomer().contains("ACME EXPORT LTD"));
        assertTrue(parsed.beneficiaryCustomer().contains("MUELLER GMBH"));
        assertTrue(parsed.remittanceInfo().contains("INVOICE 2026-42"));
    }

    @Test
    void parse_worksWithoutSwiftBlocks() {
        String plain = """
                :20:PLAINREF
                :23B:CRED
                :32A:260101USD1000,00
                :59:BENEFICIARY NAME
                """;
        ParsedMt103 parsed = parser.parse(plain);
        assertEquals("PLAINREF", parsed.senderReference());
        assertEquals("USD", parsed.currency());
        assertEquals(0, parsed.amount().compareTo(new BigDecimal("1000.00")));
    }

    @Test
    void parse_missingMandatoryTag_throws() {
        String missing = """
                :20:REFONLY
                :23B:CRED
                """;
        assertThrows(IllegalArgumentException.class, () -> parser.parse(missing));
    }

    @Test
    void parse_invalid32A_throws() {
        String bad = """
                :20:REF
                :32A:BADFIELD
                :59:NAME
                """;
        assertThrows(IllegalArgumentException.class, () -> parser.parse(bad));
    }

    @Test
    void parse_emptyMessage_throws() {
        assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
    }
}
