# Transaction FX Conversion Service

A Spring Boot REST API for storing purchase transactions in USD and converting their amounts to target currencies using U.S. Treasury exchange rates.

---

## 1. Getting Started

### Prerequisites
- **Java 21**
- **Maven 3.9+** (or use the provided Maven Wrapper `./mvnw`)
- **Docker** (optional, required for PostgreSQL profile)

### Running Locally (Default: H2 In-Memory DB)
By default, the application runs using an in-memory H2 database, requiring zero setup.

1. **Build and compile:**
   ```bash
   ./mvnw clean compile
   ```
2. **Start the application:**
   ```bash
   ./mvnw spring-boot:run
   ```
3. **Access points:**
   - **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
   - **H2 Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:mem:transactionsdb`, Username: `sa`, Password: empty)

### Running with PostgreSQL (Production-like Setup)
To run the service against a PostgreSQL instance:

1. **Spin up the database container:**
   ```bash
   docker compose up -d
   ```
2. **Start the application with the `postgres` profile:**
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

---

## 2. Core Design Decisions & Assumptions

### External API Integration (U.S. Treasury)
* **API Endpoint**: Rates are sourced from the official *U.S. Treasury Reporting Rates of Exchange* endpoint.
* **Closest Rate Resolution**: If no exact rate matches a transaction date, the application queries the Treasury API for the date range from the transaction date back to 6 months prior. By sorting the API results descending by effective date (`sort=-effective_date`), the service retrieves the closest historical rate prior to or on the transaction date as the first record in a single request (no day-by-day API scanning).

### Performance & Caching
* **Bulk Conversion Cache**: When converting bulk transactions (`GET /api/v1/transactions/conversions`), the application groups transactions by their unique transaction dates. It queries the Treasury API once per unique date and caches the result for that batch, drastically reducing external HTTP roundtrips and API latency.
* **Resiliency**: If an exchange rate cannot be found for a transaction date within the 6-month window, the individual transaction response reports a clear error message in the bulk list, allowing the rest of the transactions to convert successfully rather than crashing the entire response.

### Persistence & Migrations
* **Schema Evolution**: Database migrations are version-controlled using **Liquibase** (located under `src/main/resources/db/changelog`). 
* **Dynamic Profiles**: The application maintains a clean separation between development (H2 in-memory, Hibernate `validate`, schema seeded via Liquibase) and PostgreSQL profiles via Spring config files.

### Precision & Validation
* **Rounding**: Converted purchase amounts are rounded to exactly two decimal places using standard half-up rounding (`RoundingMode.HALF_UP`) to ensure financial accuracy.
* **Schema Validation**: Global controller validation enforces that descriptions are between 3 and 50 characters, purchase amounts are positive (>= 0.01) with up to 2 decimal places, and transaction dates cannot be in the future.
* **Naming Convention**: A snake_case JSON property naming strategy is configured globally to match REST standard guidelines.

### Requirements Traceability & API Mapping
To ensure robust alignment with the technical constraints in the email prompt and the follow-up clarifications:
* **API Path Versioning**: The service follows industry production standards by using API versioned routing (`/api/v1/transactions`).
* **Treasury API Precision Mapping**: The U.S. Treasury database maps exchange rates using unique country-currency pair descriptions rather than standalone ISO currency codes (e.g., distinguishing different nations using the same currency name). To guarantee 100% precise resolution, the `/conversions` API accepts both `target_country` (2-letter ISO) and `target_currency` (3-letter ISO) parameters.
* **Bulk Currency Conversion**: In line with the follow-up clarification favoring bulk list conversion ("Preferred to list multiple transactions with currency conversion applied is preferred"), conversion is orchestrated cleanly in bulk under `GET /api/v1/transactions/conversions` with optional error messaging per record.

---

## 3. API Reference

The service exposes versioned REST endpoints utilizing a `snake_case` JSON property naming strategy. For full interactive schemas, validation rules, and live testing, run the application and visit the **[Swagger UI](http://localhost:8080/swagger-ui.html)**.

| HTTP Method | Path | Description | Key Parameters / Request Body |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/transactions` | Store a new purchase transaction in USD | Request body (`description`, `amount`, `transaction_date`) |
| **GET** | `/api/v1/transactions` | Retrieve all purchase transactions in USD | None |
| **GET** | `/api/v1/transactions/{id}` | Retrieve a single transaction by UUID | `id` (path parameter) |
| **GET** | `/api/v1/transactions/conversions` | Retrieve transactions converted to target currency | `target_country` (query, e.g. `CA`), `target_currency` (query, e.g. `CAD`) |

---

## 4. Verification & Code Quality

### Running Tests
The test suite includes extensive controller request validations, transaction mapping checks, mock integration verifications, and custom roundoff logic checks.
```bash
./mvnw test
```

### Spotless Code Style Enforcement
The codebase adheres strictly to the **Google Java Format** style via Spotless.
* **Check code style compliance:**
  ```bash
  ./mvnw spotless:check
  ```
* **Auto-apply correct formatting:**
  ```bash
  ./mvnw spotless:apply
  ```
