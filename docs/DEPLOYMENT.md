# Hosting and deployment

This is a backend-only Spring Boot service. It supports Docker deployments to Render or other container hosts and exposes its API through Swagger/OpenAPI.

## Local verification

Use JDK 21 and the Maven Wrapper included in the repository:

```powershell
.\mvnw.cmd -B clean verify
.\mvnw.cmd spring-boot:run
```

On macOS/Linux, replace `.\mvnw.cmd` with `./mvnw`.

Local routes:

- Health: `http://localhost:8080/actuator/health`
- Swagger: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`

## Render Docker setup

The repository contains a multi-stage Dockerfile that builds using JDK 21 and runs the JAR with a non-root user.

1. Create a **Web Service** and connect this GitHub repository.
2. Select the `main` branch and **Docker** as the runtime, with `./Dockerfile` at the root.
3. Choose the instance type based on required availability and cost.
4. Configure the health check as `/actuator/health`.
5. Optionally set the environment flags below.
6. Deploy and verify the returned HTTPS URL and Swagger's server selector. The server URL should refer to the public HTTPS origin, not localhost.

Current demonstration environment:

- [Live Swagger documentation](https://dispatch-load-balancer-0lce.onrender.com/swagger-ui/index.html)
- [Service health](https://dispatch-load-balancer-0lce.onrender.com/actuator/health)
- [OpenAPI specification](https://dispatch-load-balancer-0lce.onrender.com/v3/api-docs)

## Demo data and configuration

Default storage is an in-memory H2 database that is recreated on restart. An empty `GET /api/dispatch/plan` result is valid when no records are loaded.

For an **ephemeral demonstration deployment**, set the following in your hosting dashboard:

```text
SEED_SAMPLE_DATA=true
DELETE_ENDPOINTS_ENABLED=false
```

- `SEED_SAMPLE_DATA` defaults to `false`. When true, the application seeds the official synthetic 30-order / 5-vehicle sample from classpath resources once per application startup.
- `DELETE_ENDPOINTS_ENABLED` defaults to `true`, so local API management works. When false, all four DELETE routes return `403` in the existing JSON error format.
- Other required POST APIs remain open; this is **not** a substitute for authentication. Demo data can still be changed by an API caller while the process runs.

For manual sample loading, run from the repository root (PowerShell):

```powershell
$base = "https://dispatch-load-balancer-0lce.onrender.com"
curl.exe -X POST "$base/api/dispatch/orders" -H "Content-Type: application/json" --data-binary "@samples/orders.json"
curl.exe -X POST "$base/api/dispatch/vehicles" -H "Content-Type: application/json" --data-binary "@samples/vehicles.json"
(Invoke-RestMethod "$base/api/dispatch/plan").summary | Format-List
```

With the packaged sample, the default greedy plan reports 30 total orders, 22 assigned, 8 unassigned, 490 units of allocated load and 73.42 km. The alternative `?strategy=distance-aware` retains the same assigned freight.

## Optional PostgreSQL persistence

For persistence across restarts, use the `postgres` Spring profile and a PostgreSQL 15+ database. Set secrets in the hosting dashboard, **never commit real passwords**:

```text
SPRING_PROFILES_ACTIVE=postgres
DATABASE_URL=jdbc:postgresql://HOST:5432/DATABASE
DB_USERNAME=YOUR_DB_USERNAME
DB_PASSWORD=YOUR_DB_PASSWORD
```

The `DATABASE_URL` must use the JDBC `jdbc:postgresql://` scheme. This application's schema is initialized using `schema.sql`; H2 uses `MERGE ... KEY` and PostgreSQL uses `INSERT ... ON CONFLICT` for batched upserts. The default CI suite tests H2; run separate PostgreSQL integration tests before relying on that profile in production.

## Security and operating constraints

- There is **no authentication or authorization**. The server should use synthetic data only; public POST endpoints are writable. Protect or disable public write endpoints before using any real customer data.
- Default H2 storage does not survive application restarts or Render free-tier sleep cycles. Opt-in seeding restores synthetic demo records on **startup**, not continuously after mutation.
- Haversine measures great-circle distance, not real roads or driving time.
- The routing algorithms are deterministic heuristics and do not claim a globally optimal solution.

References: [Render Docker guide](https://render.com/docs/docker) · [Spring Boot reverse-proxy documentation](https://docs.spring.io/spring-boot/how-to/webserver.html).
