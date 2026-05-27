# PMS Backend Roadmap

Goal: build the Pharmacy Management System backend feature-by-feature until the business workflows in `docs/context.md` are usable end to end.

Approach:
- Keep the work backend-only until the backend is functionally complete.
- Build one grouped feature at a time.
- After each grouped feature, write a short feature doc in `docs/features/` that explains what the feature does and why it exists.
- Keep controllers thin, services orchestration-focused, and domain objects responsible for business meaning.
- Avoid speculative layers and avoid over-engineering.

Review notes:
- The docs already define the target domain and workflows clearly.
- The codebase currently has the core entities/repositories and a subset of procurement domain behavior, but it still lacks the API layer and most working CRUD/workflow endpoints.
- The plan below is sequenced so each phase unlocks the next one without forcing frontend work.

Planned phases:

1. API foundation + product catalog
- Add a minimal API structure, request/response DTOs, and exception handling.
- Implement product CRUD/search and product stock-summary endpoints.
- Write a feature doc for product catalog management.

2. Supplier, customer, and prescription master data
- Implement supplier CRUD.
- Implement customer CRUD.
- Implement prescription CRUD + verify/reject.
- Write a feature doc for master-data management.

3. Procurement workflow
- Implement purchase order create/list/detail/approve/cancel.
- Implement goods receipt creation around the existing domain model.
- Write a feature doc for procurement and receiving.

4. Inventory visibility and controls
- Implement batch listing/detail.
- Implement stock movement ledger.
- Implement low-stock and expiry alert endpoints.
- Implement adjustment, return, and write-off actions.
- Write a feature doc for inventory control.

5. Sales workflow
- Implement sale draft/create/list/detail.
- Implement FEFO confirmation and invoice numbering.
- Implement cancel/void paths where supported.
- Write a feature doc for sales and billing.

6. Identity, auth, and authorization
- Implement login/me/logout/refresh.
- Implement user and role management.
- Add permission checks to the API.
- Write a feature doc for access control.

7. Audit trail
- Persist and expose audit log read endpoints.
- Ensure major mutations create audit records.
- Write a feature doc for traceability and auditability.

8. Backend polish
- Tighten validation and error responses.
- Add integration tests for the main workflows.
- Fill gaps in docs where the code diverges from the original spec.

Execution rule:
- Do not start frontend work until all backend phases above are complete.
- After each phase, run the test suite and verify the feature doc exists.
