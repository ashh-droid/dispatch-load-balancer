# FreightFox — Dispatch Load Balancer

Backend-only Java 21 / Spring Boot 3.4 REST API for capacity-constrained delivery planning. This project implements the FreightFox assignment's required endpoints and adds tested algorithm options, explicit infeasibility reporting, pagination and data reset for repeated demonstrations.

**The optimization is heuristic, not globally optimal.** The Haversine formula estimates great-circle distance, not road kilometres, traffic or ETA. The public endpoints intentionally have no authentication per the assessment scope; do not expose this API as a real multi-user logistics service without authentication and authorization.

## Quick start

Requirements: **Java 21** and **Maven 3.9+**; internet access is needed the first time Maven retrieves dependencies.

```bash
mvn -B verify
mvn spring-boot:run
```

Windows PowerShell uses the same `mvn` commands after Java and Maven are added to `PATH`. This repository does not ship a handmade Maven bootstrapper. To generate the **official Maven Wrapper** on a machine with Maven and an internet connection, run `mvn -N wrapper:wrapper -Dmaven=3.9.11 -Dtype=only-script` and commit the generated `mvnw`, `mvnw.cmd` and `.mvn` changes.

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

Use Postman collection `postman/Dispatch-Load-Balancer.postman_collection.json`, or run:

```bash
curl -X POST http://localhost:8080/api/dispatch/orders -H 'Content-Type: application/json' --data-binary @samples/orders.json
curl -X POST http://localhost:8080/api/dispatch/vehicles -H 'Content-Type: application/json' --data-binary @samples/vehicles.json
curl http://localhost:8080/api/dispatch/plan
curl 'http://localhost:8080/api/dispatch/plan?strategy=distance-aware'
```

PowerShell users should call `curl.exe`, not the `curl` alias. By default H2 runs in memory and resets on application restart; no external database is necessary.

## API endpoints

| Method | Route | Description |
|---|---|---|
| POST | `/api/dispatch/orders` | Validate and upsert orders using `{"orders":[...]}` |
| POST | `/api/dispatch/vehicles` | Validate and upsert vehicles using `{"vehicles":[...]}` |
| GET | `/api/dispatch/plan` | Baseline priority-first greedy plan |
| GET | `/api/dispatch/plan?strategy=distance-aware` | Greedy allocation plus limited per-vehicle 2-opt route improvements |
| GET | `/api/dispatch/orders?page=0&size=50&priority=HIGH` | Paginated, optional priority filter |
| GET | `/api/dispatch/orders/{id}` | Retrieve one order |
| DELETE | `/api/dispatch/orders/{id}` | Delete one order (`204`) |
| DELETE | `/api/dispatch/orders` | Clear orders (`204`), useful for testing |
| GET | `/api/dispatch/vehicles?page=0&size=50` | Paginated fleet |
| GET | `/api/dispatch/vehicles/{id}` | Retrieve one vehicle |
| DELETE | `/api/dispatch/vehicles/{id}` | Delete one vehicle (`204`) |
| DELETE | `/api/dispatch/vehicles` | Clear fleet (`204`), useful for testing |

The three required endpoint paths and original request/response fields are retained. The `GET /plan` output also includes `unassignedOrders` with typed reasons, route stop `sequence`, `distanceFromPreviousKm`, per-vehicle `remainingCapacity` / `utilizationPercent`, and `summary.strategy`. Distance strings such as `"14.44 km"` remain unchanged. Other HTTP responses are consistent JSON errors (except `204` responses).

**Sample response** (abbreviated, illustrative):

```json
{
  "dispatchPlan": [
    { "vehicleId": "VEH001", "totalLoad": 100, "totalDistance": "14.44 km",
      "assignedOrders": [{"orderId":"ORD026", "latitude":28.7041, "longitude":77.1025,
        "address":"Karol Bagh, Delhi, India", "packageWeight":25, "priority":"HIGH",
        "sequence":1, "distanceFromPreviousKm":0.0}],
      "remainingCapacity":0, "utilizationPercent":100.0 }
  ],
  "unassignedOrders": [{"orderId":"ORD016", "packageWeight":30, "priority":"LOW",
    "address":"Sector 18, Noida, Uttar Pradesh, India", "reason":"INSUFFICIENT_REMAINING_CAPACITY"}],
  "summary": {"totalOrders":30, "assignedOrders":22, "unassignedOrders":8,
    "totalLoad":490, "totalCapacity":500, "totalDistance":"73.42 km", "strategy":"greedy"}
}
```

## Algorithm and business rules

1. Process orders by HIGH, MEDIUM, LOW; within the same priority sort heaviest first, then order ID ascending.
2. Choose the nearest vehicle with sufficient *remaining* capacity, using Haversine distance from its current route endpoint; break ties by vehicle ID.
3. Update route, load and endpoint. Report unassigned orders as `NO_VEHICLES_AVAILABLE`, `EXCEEDS_MAX_VEHICLE_CAPACITY`, or `INSUFFICIENT_REMAINING_CAPACITY`.
4. Distances run from initial vehicle coordinates through its assigned stops; do not include a return-to-depot leg. HIGH first means allocated fleet capacity first, not delivered first.

Baseline planning complexity is **O(n log n + n × m)** for n orders and m vehicles. The optional **distance-aware** strategy applies up to three sweeps of 2-opt reversals to each route containing 3–60 stops. It never changes which orders are delivered or vehicle loading, and only accepts a strictly shorter route; it can be slower on large route groups. Above 60 stops per vehicle, it deliberately keeps the greedy sequence.

**Do not confuse fewer km with a better outcome if fewer orders are delivered.** This implementation does not drop LOW orders to improve its distance metric. Future configurable business trade-offs could be evaluated separately.

## Official 30-order assignment sample

The supplied data contains **645 kg of freight** and just **500 kg total capacity**, so some orders must remain unassigned. The deterministic greedy allocation produces:

| Vehicle | Assigned kg / capacity | Km | Stops |
|---|---:|---:|---|
| VEH001 | 100 / 100 | 14.44 | 5 HIGH |
| VEH002 | 80 / 80 | 0.00 | 2 HIGH, 1 MEDIUM |
| VEH003 | 115 / 120 | 24.74 | 1 HIGH, 3 MEDIUM, 1 LOW |
| VEH004 | 90 / 90 | 19.80 | 3 HIGH, 1 MEDIUM |
| VEH005 | 105 / 110 | 14.44 | 1 HIGH, 3 MEDIUM, 1 LOW |
| **Unassigned** | **155** | — | **8 LOW** |

**22 orders / 490 kg assigned, 8 LOW orders unassigned, 73.42 km total distance.** Distance-aware route reordering also gives **73.42 km** on this sample; no improvement is claimed. These values are independently checked by compiling the Java algorithm and running it on the official input. They are not a proof of optimality; zero Haversine km reflects duplicated sample coordinates.

## Design and persistence

- `strategy/` stores Java-only planner inputs/results, priority-first greedy allocator, bounded 2-opt route optimization, and typed unassigned reasons. This layer has no dependence on web DTOs or JPA.
- `mapper/` converts domain routes into the public JSON contract and formats kilometres.
- `controller/` handles HTTP and OpenAPI annotations; `service/` handles orchestration, validation of duplicated IDs and data management.
- `repository/` offers Spring Data JPA for reads and JDBC batch upserts for writes. H2 uses `MERGE .. KEY`; PostgreSQL uses `INSERT .. ON CONFLICT`. The two dialect-specific statements are intentional.
- Weights/capacities use `BigDecimal`, while geographic calculations use `double`. API request IDs reject surrounding whitespace to prevent accidental duplicates.
- H2 is the default in-memory database. `SPRING_PROFILES_ACTIVE=postgres` activates PostgreSQL 15+; configure `DATABASE_URL` as a JDBC URL, `DB_USERNAME` and `DB_PASSWORD`.

## Testing and CI

```bash
mvn -B verify
# inspect target/site/jacoco/index.html
```

`verify` runs unit, MVC, H2 repository, full application, ArchUnit layer-rule and seeded property-style scenario tests. JaCoCo is configured to report coverage and enforce an **80% line-coverage threshold for the algorithm and distance packages**. A GitHub Actions workflow runs the same command on pushes to `main` and pull requests; check the actual workflow outcome in GitHub before adding a green status badge or reporting a coverage percentage.

Important tests: official sample regression, Haversine calculations, capacity invariants, deterministic tie breaking, unassigned reasons, bad content types (415/406), invalid field types, malformed requests, invalid strategy names, CRUD flows and 200 seeded-random scenarios for the two algorithms. A performance-tagged test also checks a large order set, but deliberately does not impose unstable wall-clock CI limits.

## Deployment

The included multi-stage Dockerfile compiles the JAR and runs it under a **non-root** user. The backend listens on `${PORT:8080}`. A hosted application needs a public HTTPS URL and accessible Swagger UI, as specified in the assignment email. Do not claim deployment until the running endpoint is verified. The local H2 database resets whenever the container restarts; production persistence should use PostgreSQL.

## Future production improvements

Authentication and authorization (especially for reset endpoints), database migrations, managed PostgreSQL verification, real road-network distance, time windows, return-to-depot, snapshot-based plan caching, background scheduling and a mathematically grounded objective that balances service level against cost.
