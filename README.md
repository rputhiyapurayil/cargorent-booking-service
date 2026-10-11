# cargorent-booking-service

Bookings, availability check, price calculation, the rental status flow (approve → pickup → return), booking history and admin reports.

Part of **CarGoRent** – see [cargorent-infra](../cargorent-infra/README.md) for the architecture, how to run
the whole system, demo accounts and the Postman collection.

| | |
|---|---|
| Port | 8083 |
| Schema | `cargorent_bookings` (table `bookings`) |
| Swagger | http://localhost:8083/swagger-ui.html |
| Calls | vehicle-service (price, existence), notification-service (status notifications) |

## Endpoints

| Method | Path | Who |
|---|---|---|
| GET | `/bookings/quote?vehicleId=&from=&to=` | public |
| POST | `/bookings` · GET `/bookings/my`, `/bookings/{id}` · PATCH `/bookings/{id}/cancel` | customer |
| GET | `/bookings?status=&from=&to=&q=` · PATCH `/{id}/approve`, `/{id}/pickup`, `/{id}/return` | admin |
| GET | `/reports/summary?from=&to=` | admin |
| PATCH `/bookings/{id}/status` · GET `/bookings/booked-vehicle-ids`, `/bookings/user/{id}`, `/bookings/upcoming` | | internal only (blocked at gateway) |

**Pricing:** days × price per day; 10% off for 7+ days, 15% off for 30+ days.
**Status flow:** PENDING_PAYMENT → CONFIRMED → APPROVED → PICKED_UP → RETURNED (or CANCELLED). Unpaid bookings expire after 30 minutes.

## Run

```bash
# needs MySQL: cd ../cargorent-infra && docker compose -f docker-compose.mysql.yml up -d
mvn spring-boot:run
```

## Package layout

`com.cargorent.<service>` → `controller` · `service` · `repository` · `entity` · `dto` · `client` (calls to other services) · `common` (errors, caller identity) · `config`
