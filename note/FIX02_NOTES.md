# EasyHome V2 – Fix02: synchronized state transitions

## Test failure report (2026-11-12)
- `MarketplaceFlowTest`: 5 tests, 2 failures; backend source compiled successfully.
- After `POST /api/admin/identity/{id}/review` returned `VERIFIED`, `POST /api/orders` answered 400 (`Cần xác thực CCCD trước khi mua gói đăng tin`).
- A separate test reached `POST /api/dev/orders/{id}/confirm` and received 400 after successfully creating the order, indicating a possible stale listing state.

## Code changes
1. Save and flush account verification and identity review updates explicitly.
2. Treat `identityStatus` as the authoritative verified state; keep `verified` synced for compatibility.
3. Save and flush critical listing/order transitions between HTTP calls.
4. Save and flush wallet balance and moderation outcomes in the same transaction.
5. Add HTTP and JPA assertions to expose any subsequent stale-state issue at its actual step.

## Verification requirements
In `backend`, run `mvn clean test`, then `mvn spring-boot:run` using the dedicated test database. Local static syntax checks do NOT establish that Maven integration tests pass. The original source's business rules, pricing and API URLs remain unchanged.

Do not push to GitHub or overwrite the existing working source before verifying this ZIP.
