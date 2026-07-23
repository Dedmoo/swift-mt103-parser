# SwiftMt103Parser

[![CI](https://github.com/Dedmoo/SwiftMt103Parser/actions/workflows/ci.yml/badge.svg)](https://github.com/Dedmoo/SwiftMt103Parser/actions/workflows/ci.yml)

SwiftMt103Parser is an educational Spring Boot service that extracts a practical subset of MT103 fields into JSON. It is a portfolio project, not a complete ISO 15022 implementation and not suitable for payment processing without a SWIFT-compliant validation and integration layer.

## What it supports

The parser accepts either plain MT103 text tags or a message containing a `{4: ... -}` text block.

| Tag | JSON field | Status |
| --- | --- | --- |
| `:20:` | `senderReference` | Required |
| `:23B:` | `bankOperationCode` | Optional |
| `:32A:` | `valueDate`, `currency`, `amount` | Required |
| `:50K:`, `:50A:`, `:50F:` | `orderingCustomer` | Optional |
| `:59:` | `beneficiaryCustomer` | Required |
| `:70:` | `remittanceInfo` | Optional |
| `:71A:` | `detailsOfCharges` | Optional |
| `:52A:` | `orderingInstitution` | Optional |
| `:57A:` | `accountWithInstitution` | Optional |
| `:72:` | `senderToReceiverInformation` | Optional |

`:32A:` values must have the `YYMMDDCCCAMOUNT` shape, such as `260720EUR12345,67`. The amount is returned as a JSON number using a decimal point.

## Limits

- This is an educational subset of MT103, not full ISO 15022 validation.
- It does not validate BICs, country-specific account formats, conditional field rules, character sets, duplicate tags, or network headers.
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
