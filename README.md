# SwiftMt103Parser

[![CI](https://github.com/Dedmoo/SwiftMt103Parser/actions/workflows/ci.yml/badge.svg)](https://github.com/Dedmoo/SwiftMt103Parser/actions/workflows/ci.yml)

SwiftMt103Parser is an educational Spring Boot service that extracts a practical subset of MT103 fields into JSON. It is a portfolio project, not a complete ISO 15022 implementation and not suitable for payment processing without a SWIFT-compliant validation and integration layer.

## Supported subset

The parser accepts either plain MT103 text tags or a message containing a `{4: ... -}` text block.

| Tag | JSON field | Requirement |
| --- | --- | --- |
| `:20:` | `senderReference` | Required, 1-16 basic characters on one line |
| `:23B:` | `bankOperationCode` | Optional |
| `:32A:` | `valueDate`, `currency`, `amount` | Required |
| `:50K:`, `:50A:`, `:50F:` | `orderingCustomer` | Optional |
| `:59:` or `:59A:` | `beneficiaryCustomer` | One is required |
| `:70:` | `remittanceInfo` | Optional |
| `:71A:` | `detailsOfCharges` | Optional |
| `:52A:` | `orderingInstitution` | Optional |
| `:57A:` | `accountWithInstitution` | Optional |
| `:72:` | `senderToReceiverInformation` | Optional |

## Validation matrix

This is a deliberately small, documented validation set. It is not the ISO 15022 field specification.

| Field | Length and basic charset checks |
| --- | --- |
| `:20:` | 1-16 characters, one line, letters, digits, spaces, and `/?:().,'+-{}` only |
| `:23B:` | Exactly four uppercase letters |
| `:32A:` | `YYMMDDCCCAMOUNT`, where the date is valid, the amount has 1-15 whole digits and an optional comma fraction |
| `:50K:`, `:50A:`, `:50F:`, `:59:`, `:59A:`, `:70:`, `:72:` | Up to four lines, each at most 35 characters, using letters, digits, spaces, and `/?:().,'+-{}` |
| `:71A:` | `BEN`, `OUR`, or `SHA` |
| `:52A:`, `:57A:` | Up to two lines, each at most 35 basic characters; the final line must be an uppercase BIC8 or BIC11 |

Every parsed tag may occur once only. A repeated tag is rejected instead of silently replacing the earlier value.

For `:32A:`, the supported currency allowlist is `AED`, `AUD`, `BRL`, `CAD`, `CHF`, `CNY`, `CZK`, `DKK`, `EUR`, `GBP`, `HKD`, `HUF`, `INR`, `JPY`, `MXN`, `NOK`, `NZD`, `PLN`, `QAR`, `RON`, `SAR`, `SEK`, `SGD`, `TRY`, `USD`, and `ZAR`. `JPY` must have no fractional amount; the other listed currencies allow up to two fractional digits. The amount is returned as a JSON number using a decimal point.

## Limits

- This is an educational subset of MT103, not full ISO 15022 validation.
- It validates only the BIC8/BIC11 shape in `:52A:` and `:57A:`. It does not validate BIC registration, account formats, conditional field rules, or network headers.
- It does not connect to SWIFTNet, store messages, support other MT types, or convert messages to ISO 20022/MX.
- Requests cap `message` at 35,000 characters. API responses include basic browser-facing security headers.

## Run

```bash
./mvnw test
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd`.

## API

| Method | Path | Purpose |
| --- | --- | --- |
| `POST` | `/api/swift/mt103/parse` | Parse one MT103 message |
| `GET` | `/api/swift/mt103/health` | Return `SwiftMt103Parser` health |

```bash
curl -X POST http://localhost:8083/api/swift/mt103/parse \
  -H "Content-Type: application/json" \
  -d '{"message":":20:REF123\n:23B:CRED\n:32A:260720EUR12345,67\n:59:BENEFICIARY LTD"}'
```

Validation failures return HTTP 400 and identify the missing or malformed tag. Fixture messages under `src/test/resources/mt103` form the parser regression corpus.

## Documentation

- [Architecture](docs/architecture.md)
- [UML](docs/uml.md)
- [Static documentation index](docs/index.html)

## License

MIT, see [LICENSE](LICENSE).
