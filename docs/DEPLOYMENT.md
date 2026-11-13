# Deployment and FreightFox submission guide

This repository contains a **backend-only** API. The three required endpoints remain:

- `POST /api/dispatch/orders`
- `POST /api/dispatch/vehicles`
- `GET /api/dispatch/plan`

The application is not deployed merely because the source is on GitHub. The evaluation email separately requests a **live deployed URL**, accessible **Swagger documentation**, and a **Loom demonstration**.

## 1. Verify locally

Use **Java 21**. From the repository root, run:

```powershell
java -version
.\mvnw.cmd -B clean verify
.\mvnw.cmd spring-boot:run
```

On Linux/macOS, use `./mvnw` instead of `.\mvnw.cmd`.

Verify:
- `http://localhost:8080/actuator/health` returns an `UP` status.
- `http://localhost:8080/swagger-ui.html` displays the API.
- `http://localhost:8080/v3/api-docs` returns an OpenAPI JSON document.

In **another** PowerShell window at the repository root:

```powershell
curl.exe -X POST "http://localhost:8080/api/dispatch/orders" -H "Content-Type: application/json" --data-binary "@samples/orders.json"
curl.exe -X POST "http://localhost:8080/api/dispatch/vehicles" -H "Content-Type: application/json" --data-binary "@samples/vehicles.json"
curl.exe "http://localhost:8080/api/dispatch/plan"
curl.exe "http://localhost:8080/api/dispatch/plan?strategy=distance-aware"
```

The 30-order sample expects 22 assigned, 8 unassigned, a 490 kg assigned load and **73.42 km** under the default greedy strategy.

## 2. Deploy a Docker web service on Render

A GitHub connection is required to deploy a **private repository**. Authorize access to `ashh-droid/dispatch-load-balancer`; there is **no need to make the source public** unless your assessment reviewer explicitly requires it.

1. In the Render dashboard, select **New → Web Service**, connect the GitHub repo, and choose the `main` branch.
2. Choose the **Docker** runtime. The `Dockerfile` is in the repository root. There is no separate frontend or build/start command to configure.
3. Review the chosen service plan and costs before creating it. Render may sleep/scale down instances depending on the plan.
4. Keep the default H2 profile for the quickest assessment demonstration (no database credentials necessary). The app reads Render's `PORT` environment variable; it falls back to 8080 locally.
5. Configure the **Health Check Path** as `/actuator/health`.
6. Deploy and open the Render-provided HTTPS URL. Check `/actuator/health`, `/v3/api-docs`, and `/swagger-ui.html`. The Swagger server selector should use the public HTTPS host, not `localhost:8080`.

**Important:** H2 is **in-memory** in the default configuration. Every restart/redeploy clears the uploaded orders and vehicles. After deployment, upload the sample input to the hosted API again before demonstrating `GET /plan`. An HTTPS URL and a successful health check do **not** by themselves prove the sample plan works.

### Optional persistent PostgreSQL

For data that must survive restarts, configure a supported PostgreSQL server and set these service environment variables:

```text
SPRING_PROFILES_ACTIVE=postgres
DATABASE_URL=jdbc:postgresql://HOST:5432/DATABASE
DB_USERNAME=YOUR_DB_USERNAME
DB_PASSWORD=YOUR_DB_PASSWORD
```

Set these in Render's **secret environment variable settings**, not in GitHub. `DATABASE_URL` must be a **JDBC URL** with the `jdbc:postgresql://` prefix; some hosting dashboards provide a `postgresql://` URL, which is not directly interchangeable. The PostgreSQL code path is not covered by the default H2 integration test suite. Test it before relying on persistent deployment.

## 3. Verify the live assessment contract

Substitute your actual deployed origin for `https://YOUR-SERVICE.onrender.com`:

```powershell
$base = "https://YOUR-SERVICE.onrender.com"
curl.exe "$base/actuator/health"
curl.exe -X POST "$base/api/dispatch/orders" -H "Content-Type: application/json" --data-binary "@samples/orders.json"
curl.exe -X POST "$base/api/dispatch/vehicles" -H "Content-Type: application/json" --data-binary "@samples/vehicles.json"
curl.exe "$base/api/dispatch/plan"
```

Before recording, verify that Swagger's **Try it out** buttons execute requests to the **deployed origin**. Use the included Postman collection for additional validation/error cases.

## 4. Record and submit

Keep the Loom demonstration focused (approximately 3–5 minutes):

1. Show the repository's build checks, `README.md`, and clean project structure.
2. Open **public HTTPS Swagger** and `/actuator/health`.
3. Upload official sample orders and vehicles; show 22 assigned and 8 unassigned with capacity constraints.
4. Show the alternative strategy and one invalid-payload response, explaining the trade-offs honestly.
5. End with the deployed URL and a brief description of the algorithm's limitations.

Submit the **repository URL**, **live application URL**, **Swagger URL**, and **Loom URL** in the assessment form. If the repository is private, add only the exact evaluator accounts explicitly required by FreightFox; do not expose secrets or personal credentials.

## Demo-only security and operational limits

The assignment API intentionally has **no authentication**. Anyone who knows the hosted URL can create/update/delete assessment records, including the extra bulk-reset endpoints. Use **synthetic sample data only**, not real customer information. For a production system, add authentication and authorization, request throttling and persistent storage before public exposure. The algorithm uses great-circle Haversine distance, not actual drivable roads, and does not claim a globally optimal vehicle-routing solution.

Official platform references: [Render Docker deployment](https://render.com/docs/docker), [Render deployment troubleshooting](https://render.com/docs/troubleshooting-deploys), and [Spring Boot reverse-proxy handling](https://docs.spring.io/spring-boot/how-to/webserver.html).
