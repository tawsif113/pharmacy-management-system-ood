# Sales Workflow: Draft, FEFO Confirmation, Cancel, and Void

This feature adds the sales/billing workflow for PMS.

## What it does
- Creates sales as draft invoices.
- Allocates stock using FEFO (first-expiry, first-out) at draft time so the sale can show the exact batches involved.
- Confirms a sale by validating stock, expiry, and prescription rules, then deducts stock and records stock movements.
- Cancels a draft sale without affecting stock.
- Voids a confirmed sale by restoring stock and writing reversal movements.
- Supports sales list and detail views for operational tracking.

## Why it matters
- This is the point where the pharmacy turns inventory into revenue.
- It enforces the clinical rule that prescription-required products cannot be sold without a valid prescription.
- It preserves batch traceability so every sold item can be traced back to a batch.
- It keeps the workflow practical: controllers stay thin, services own the rules, and the domain model carries the sale lifecycle.

## Key behavior
- Draft sales can be created and updated before confirmation.
- FEFO batch selection prefers the earliest expiry date, then the earliest receipt timestamp, then batch id.
- Confirmed sales update batch availability and create `stock_movements` rows with `type=sale`.
- Voiding a confirmed sale restores the sold quantities and creates reversal movements.
- Cancelled sales remain in the system for traceability instead of being hard deleted.

## Runtime contract
- Main endpoints:
  - `POST /sales`
  - `GET /sales`
  - `GET /sales/{id}`
  - `PATCH /sales/{id}`
  - `POST /sales/{id}/confirm`
  - `POST /sales/{id}/cancel`
  - `POST /sales/{id}/void`

## Verification
This feature was verified with:
- `./gradlew test`
- live HTTP smoke tests against the running app
- ordered workflow smoke checks covering:
  - supplier/customer/product/prescription setup
  - purchase order approval and goods receipt creation
  - inventory batch visibility
  - sale draft creation
  - FEFO split across multiple batches
  - sale confirmation
  - sale void reversal
  - draft sale cancellation

## Notes
- The smoke test intentionally generated live database data.
- The sales draft uses the current eligible batches for FEFO allocation so the user can see what will be sold before confirmation.
- Void/cancel paths are intentionally non-destructive.
