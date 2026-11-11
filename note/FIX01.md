# EasyHome V2 Phase 1 - Fix01

## Maven compilation fix (2026-11-12)

- File: `backend/src/main/java/com/easyhome/service/Identity.java`
- Changed: `User finance(Jwt jwt)` -> `public User finance(Jwt jwt)`.
- Reason: `WalletApi` resides in `com.easyhome.api` and must access this method across packages.
- No API/business-logic changes.

## Verify on Windows

```powershell
cd backend
mvn clean test
mvn spring-boot:run
```

Note: GitHub Flyway metadata HTTP 401 is a warning in the supplied log; it did not cause the reported Java compiler failure. Full Maven build must be verified on your machine.
