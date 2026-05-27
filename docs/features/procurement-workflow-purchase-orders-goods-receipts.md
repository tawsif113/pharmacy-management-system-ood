# Procurement Workflow: Purchase Orders and Goods Receipts

This backend feature group handles the buying side of the pharmacy.

## What it does

### Purchase orders
The API supports:
- `POST /purchase-orders`
- `GET /purchase-orders`
- `GET /purchase-orders/{id}`
- `POST /purchase-orders/{id}/approve`
- `POST /purchase-orders/{id}/cancel`

Business behavior:
- purchase orders are created as drafts
- supplier, creator user, and products must already exist
- inactive suppliers or products are rejected
- duplicate products inside one purchase order are rejected
- totals are calculated from ordered quantity × unit cost
- approved orders can move forward to receiving
- cancelled orders stop the workflow cleanly

### Goods receipts
The API supports:
- `POST /goods-receipts`
- `GET /goods-receipts`
- `GET /goods-receipts/{id}`

Business behavior:
- receipts can only be created for approved or partially received purchase orders
- each receipt line targets a specific purchase order item
- received quantity cannot exceed the remaining ordered quantity
- receipt creation creates batch records and matching stock movements
- purchase order status moves to partially received or received based on the item completion state

## Why it matters

This feature is the bridge between master data and live inventory:
- purchase orders start the procurement request
- goods receipts turn approved orders into real stock
- batches and stock movements are created as the inventory trail for future stock visibility and sales

## Design notes

- controllers stay thin and only translate HTTP into service calls
- services own validation, workflow rules, and status transitions
- the domain model owns the state transition helpers for approval, cancellation, and receipt tracking
- runtime responses hydrate the needed associations before mapping to DTOs so the API can safely serialize nested purchase-order and receipt data

## Verification

This feature was verified with:
- `./gradlew test`
- live smoke tests against the running app on port `8081`
- purchase order create/get/list/approve/cancel
- goods receipt create/get/list
- end-to-end receipt flow that updates the purchase order status to received
