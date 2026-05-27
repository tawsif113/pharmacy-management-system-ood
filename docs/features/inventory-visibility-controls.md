# Inventory Visibility and Controls

This feature adds the backend inventory layer that sits on top of procurement receiving.

What it covers
- Batch visibility
  - list batches with filtering and pagination
  - fetch batch details by id
- Stock movement ledger
  - list movement history with filters
  - fetch movement details by id
- Inventory alerts
  - low-stock product summaries
  - expiry alerts for batches nearing their expiry window
- Inventory controls
  - adjust batch quantities
  - record stock returns
  - record write-offs

Why it matters
- Operations need a live view of stock, not just purchase receipts.
- The warehouse team needs a simple control surface for corrections and stock loss.
- The system now exposes the inventory state that later sales workflows will consume.

Implementation notes
- Batches remain the source of truth for on-hand quantity.
- Every inventory-changing action writes a stock movement row.
- Low-stock alerts are computed from product reorder levels and batch availability.
- Expiry alerts are computed from batch expiry dates and remaining quantity.
- Controllers stay thin; the inventory service owns the orchestration and validation.

Verification
- Full unit test suite passed.
- Live smoke tests against the running app passed for batch reads, movement reads, low-stock alerts, expiry alerts, and inventory actions.
