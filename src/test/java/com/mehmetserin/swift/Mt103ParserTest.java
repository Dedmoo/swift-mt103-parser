package com.mehmetserin.swift;

import com.mehmetserin.swift.model.Mt103Models.ParsedMt103;
import com.mehmetserin.swift.service.Mt103Parser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.core.io.ClassPathResource;

import java.math.BigDecimal;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Mt103ParserTest {

    private final Mt103Parser parser = new Mt103Parser();

    @ParameterizedTest(name = "{0}")
    @MethodSource("validFixtures")
    void parse_validFixture_extractsCoreFields(String fixture, String reference, String currency, String amount)
            throws IOException {
        ParsedMt103 parsed = parser.parse(readFixture(fixture));
        assertEquals(reference, parsed.senderReference());
        assertEquals(currency, parsed.currency());
        assertEquals(0, parsed.amount().compareTo(new BigDecimal(amount)));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidFixtures")
    void parse_invalidFixture_reportsSpecificValidationError(String fixture, String expectedError) throws IOException {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> parser.parse(readFixture(fixture)));
        assertTrue(exception.getMessage().contains(expectedError));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("partyFixtures")
    void parse_partyFixture_flattensMultilineValues(String fixture) throws IOException {
        ParsedMt103 parsed = parser.parse(readFixture(fixture));
        assertTrue(parsed.orderingCustomer().contains("ACME EXPORT LTD"));
        assertTrue(parsed.beneficiaryCustomer().contains("MUELLER GMBH"));
        assertTrue(parsed.remittanceInfo().contains("INVOICE 2026-42"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("optionalTagFixtures")
    void parse_optionalTags_exposesValues(String fixture) throws IOException {
        ParsedMt103 parsed = parser.parse(readFixture(fixture));
        assertEquals("ORDRTRISXXX", parsed.orderingInstitution());
        assertEquals("ACCTDEFFXXX", parsed.accountWithInstitution());
        assertEquals("/INS/ROUTING INFORMATION", parsed.senderToReceiverInformation());
    }

    @Test
    void parse_emptyMessage_reportsValidationError() {
        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> parser.parse("   "));
        assertEquals("Message is empty.", exception.getMessage());
    }

    private static Stream<Arguments> validFixtures() {
        return Stream.of(
                Arguments.of("valid-envelope.txt", "ENVELOPEREF01", "EUR", "12345.67"),
                Arguments.of("valid-plain.txt", "PLAINREF01", "USD", "1000.00"),
                Arguments.of("valid-optional-tags.txt", "OPTIONALREF01", "TRY", "5000.25"));
    }

    private static Stream<Arguments> invalidFixtures() {
        return Stream.of(
                Arguments.of("missing-20.txt", "Mandatory tag :20: is missing."),
                Arguments.of("missing-59.txt", "Mandatory tag :59: is missing."),
                Arguments.of("invalid-32a-format.txt", "Invalid :32A: field."),
                Arguments.of("invalid-32a-date.txt", "Invalid :32A: value date"));
    }

    private static Stream<Arguments> partyFixtures() {
        return Stream.of(Arguments.of("valid-envelope.txt"));
    }

    private static Stream<Arguments> optionalTagFixtures() {
        return Stream.of(Arguments.of("valid-optional-tags.txt"));
    }

    private static String readFixture(String fixture) throws IOException {
        return new ClassPathResource("mt103/" + fixture).getContentAsString(StandardCharsets.UTF_8);
    }
}
