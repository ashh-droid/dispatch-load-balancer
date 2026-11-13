# Dispatch Load Balancer

[![Java CI](https://github.com/ashh-droid/dispatch-load-balancer/actions/workflows/ci.yml/badge.svg)](https://github.com/ashh-droid/dispatch-load-balancer/actions/workflows/ci.yml)


Java 21 / Spring Boot REST API for priority-aware, capacity-constrained delivery planning. Supports deterministic vehicle assignment, Haversine distances, optional 2-opt route improvement, explicit unassigned-order reasons, pagination, and batched database upserts.

**The optimization is heuristic, not globally optimal.** The Haversine formula estimates great-circle distance, not road kilometres, traffic or ETA. This API demonstrates routing logic with synthetic data and does not include authentication; deployment with real customer data would require access controls.

**Live API:** [Open the service](https://dispatch-load-balancer-0lce.onrender.com/) · [Swagger documentation](https://dispatch-load-balancer-0lce.onrender.com/swagger-ui/index.html) · [Health status](https://dispatch-load-balancer-0lce.onrender.com/actuator/health) · [OpenAPI JSON](https://dispatch-load-balancer-0lce.onrender.com/v3/api-docs)

The deployed service uses a free-tier, in-memory demonstration database. It may restart or sleep when idle; see [deployment notes](docs/DEPLOYMENT.md) for the optional sample-data seeding configuration. The project is API-only. The root URL redirects to Swagger; there is no separate application frontend.

## Quick start

Requires **JDK 21**. The repository includes the official Maven Wrapper; Maven does not need to be installed separately. First-time builds require internet access to resolve dependencies and, if necessary, download Maven.

```bash
# macOS / Linux
./mvnw -B verify
./mvnw spring-boot:run

# Windows PowerShell
.\\mvnw.cmd -B verify
.\\mvnw.cmd spring-boot:run
```

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

Baseline planning complexity is **O(n log n + n × m)** for n orders and m vehicles. The optional **distance-aware** strategy applies up to three bounded 2-opt sweeps per route with 3–1,000 stops. Each candidate reversal compares only the changed boundary edges, making a sweep O(k²) in the route's stop count k. The strategy preserves every assignment and accepts only a strictly shorter route. Routes above 1,000 stops retain the greedy sequence to bound work.

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

**22 orders / 490 kg assigned, 8 LOW orders unassigned, 73.42 km total distance.** Distance-aware route reordering also gives **73.42 km** on this sample because each vehicle visits at most two distinct locations. The automated test suite separately verifies improvement on a 200-stop zigzag route and non-regression on seeded random cases. These results are not a proof of global optimality; zero Haversine km reflects duplicated sample coordinates.

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

`verify` runs unit, MVC, H2 repository, full application, ArchUnit layer-rule and seeded property-style scenario tests. JaCoCo enforces an **80% line-coverage threshold for the algorithm and distance packages**.

**Coverage (verified local run):** 90% instruction, 80% branch, and approximately 92% line coverage overall. The CI badge reflects the latest automated build; new revisions should be checked with `./mvnw verify` and `target/site/jacoco/index.html`. Coverage checks target the strategy and distance packages.

Important tests: official sample regression, Haversine calculations, capacity invariants, deterministic tie breaking, unassigned reasons, bad content types (415/406), invalid field types, malformed requests, invalid strategy names, CRUD flows and 200 seeded-random scenarios for the two algorithms. A performance-tagged test also checks a large order set, but deliberately does not impose unstable wall-clock CI limits.

## Deployment

The multi-stage Dockerfile builds the JAR and runs the service as a **non-root** user. It listens on `${PORT:8080}` and supports HTTPS reverse-proxy headers. The [live Swagger page](https://dispatch-load-balancer-0lce.onrender.com/swagger-ui/index.html) runs on Render. By default H2 is in memory; `SEED_SAMPLE_DATA=true` opts into loading the packaged synthetic data on startup, so a sleeping/restarted demo has predictable sample results. `DELETE_ENDPOINTS_ENABLED=false` disables all DELETE endpoints with a structured 403 response on shared hosting. Both flags are optional; see the [deployment guide](docs/DEPLOYMENT.md). PostgreSQL can be configured for persistent storage.

## Future production improvements

Authentication and authorization (especially for reset endpoints), database migrations, managed PostgreSQL verification, real road-network distance, time windows, return-to-depot, snapshot-based plan caching, background scheduling and a mathematically grounded objective that balances service level against cost.
