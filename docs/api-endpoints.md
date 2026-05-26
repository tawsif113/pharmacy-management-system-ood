# Pharmacy Management System MVP REST API Spec (first pass)

Source of truth: `context.md`, `database-schema.md`, `migrations/001_initial_schema.sql`

## API conventions
- IDs are UUIDs.
- Money fields use decimal/numeric(14,2).
- Dates are ISO-8601 (`YYYY-MM-DD` for dates, RFC3339 for timestamps).
- No hard deletes for critical records; use `active=false`, `cancelled`, or `void`.
- List endpoints should support `page`, `size`, `sort`, plus domain filters.
- Standard error handling: `400` validation, `401` auth, `403` permission denied, `404` not found, `409` uniqueness/state conflict, `422` business-rule failure (e.g. stock/prescription/expiry), `500` unexpected.
- Every inventory mutation must create a `stock_movements` row and an `audit_logs` row in the same transaction.

## 1) Auth / identity basics

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| POST | `/auth/login` | Authenticate user and issue tokens | `usernameOrEmail`, `password` | `accessToken`, `refreshToken`, `user`, `roles`, `permissions` | Reject inactive users; lockout/rate-limit recommended |
| POST | `/auth/refresh` | Rotate access token | `refreshToken` | `accessToken`, `refreshToken` | Reject expired/revoked tokens |
| POST | `/auth/logout` | Revoke refresh token | `refreshToken` | `success` | Treat as idempotent |
| GET | `/auth/me` | Return current principal | header auth only | `user`, `roles`, `permissions` | Use for UI bootstrap |
| GET | `/users` | List users | `page`, `size`, `search`, `active`, `roleId` | `items[]`, `page`, `total` | Admin-only |
| POST | `/users` | Create user | `username`, `fullName`, `email`, `password`, `roleIds[]`, `active` | `id`, `username`, `fullName`, `email`, `active`, `roles`, timestamps | Unique `username`/`email`; hash password server-side |
| GET | `/users/{id}` | Get user detail | path `id` | user fields, `roles` | Admin-only |
| PATCH | `/users/{id}` | Update user profile / status | `fullName`, `email`, `password?`, `active?` | updated user | Cannot delete; deactivation only |
| PUT | `/users/{id}/roles` | Replace assigned roles | `roleIds[]` | `userId`, `roleIds[]` | Admin-only; replace semantics |
| GET | `/roles` | List roles | `page`, `size`, `search`, `active` | `items[]`, `page`, `total` | Admin-only |
| POST | `/roles` | Create role | `name`, `permissions[]`, `active` | `id`, `name`, `permissions`, `active` | Unique `name` |
| PATCH | `/roles/{id}` | Update role | `name?`, `permissions?`, `active?` | updated role | Do not remove roles in use; deactivate instead |

## 2) Products

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/products` | Search catalog | `page`, `size`, `search`, `category`, `active`, `isPrescriptionRequired` | `items[]` with `id`, `skuBarcode`, `name`, `brand`, `genericName`, `category`, `dosageForm`, `strength`, `packSize`, `isPrescriptionRequired`, `reorderLevel`, `active` | Search by SKU/barcode/name recommended |
| POST | `/products` | Create product master | `skuBarcode`, `name`, `brand`, `genericName`, `category`, `dosageForm`, `strength`, `packSize`, `isPrescriptionRequired`, `reorderLevel`, `active` | created product + timestamps | Unique `skuBarcode`; `reorderLevel >= 0` |
| GET | `/products/{id}` | Product detail | path `id` | product fields plus stock summary (optional) | Consider including current available qty in response |
| PATCH | `/products/{id}` | Update product master | any editable master fields | updated product | No hard delete; deactivation via `active=false` |
| GET | `/products/{id}/batches` | Batch visibility for a product | path `id`, `status`, `expiredOnly`, `page`, `size` | `items[]` batches | Useful for FEFO and expiry review |
| GET | `/products/{id}/stock-summary` | Quick inventory snapshot | path `id` | `availableQuantity`, `reservedQuantity`, `expiredQuantity`, `nextExpiryDate`, `lowStockFlag` | Computed from batches |

## 3) Suppliers

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/suppliers` | List/search suppliers | `page`, `size`, `search`, `active` | `items[]` | Search by name/phone/email |
| POST | `/suppliers` | Create supplier | `name`, `phone`, `email`, `address`, `active` | supplier object | `name` required |
| GET | `/suppliers/{id}` | Supplier detail | path `id` | supplier object | Includes basic procurement counts if useful |
| PATCH | `/suppliers/{id}` | Update supplier | `name?`, `phone?`, `email?`, `address?`, `active?` | updated supplier | Prefer deactivation over delete |

## 4) Customers

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/customers` | List/search customers | `page`, `size`, `search`, `phone`, `dateOfBirth` | `items[]` | Search by name/phone |
| POST | `/customers` | Create customer/patient profile | `name`, `phone`, `address`, `dateOfBirth`, `notes` | customer object | Useful for prescription and sales linkage |
| GET | `/customers/{id}` | Customer detail | path `id` | customer + recent sales/prescriptions (optional) |  |
| PATCH | `/customers/{id}` | Update customer | `name?`, `phone?`, `address?`, `dateOfBirth?`, `notes?` | updated customer | No hard delete |
| GET | `/customers/{id}/prescriptions` | Customer prescription history | path `id`, `page`, `size`, `status` | `items[]` prescriptions | Helps sale validation |
| GET | `/customers/{id}/sales` | Customer purchase history | path `id`, `page`, `size` | `items[]` sales | Optional but practical |

## 5) Prescriptions

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/prescriptions` | List/search prescriptions | `page`, `size`, `customerId`, `verificationStatus`, `expiryBefore`, `expiryAfter` | `items[]` | Filter for expiring/invalid prescriptions |
| POST | `/prescriptions` | Create prescription record | `customerId`, `doctorName`, `doctorRegistrationNo`, `issueDate`, `expiryDate`, `fileUrl`, `verificationStatus` | prescription object | `expiryDate >= issueDate`; default status should usually be `pending` |
| GET | `/prescriptions/{id}` | Prescription detail | path `id` | prescription object | Include customer summary if needed |
| PATCH | `/prescriptions/{id}` | Update prescription metadata | `doctorName?`, `doctorRegistrationNo?`, `issueDate?`, `expiryDate?`, `fileUrl?`, `verificationStatus?` | updated prescription | Do not allow invalid date ranges |
| POST | `/prescriptions/{id}/verify` | Mark prescription verified | `verifiedBy?`, `notes?` | `id`, `verificationStatus=verified`, timestamps | Use for manual validation workflow |
| POST | `/prescriptions/{id}/reject` | Reject prescription | `reason` | `id`, `verificationStatus=rejected` | Keep audit trail; do not delete |

## 6) Purchase orders

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/purchase-orders` | List/search POs | `page`, `size`, `supplierId`, `status`, `poNumber`, `createdBy`, `dateFrom`, `dateTo` | `items[]` with PO header + line summaries | Standard procurement list view |
| POST | `/purchase-orders` | Create PO draft | `supplierId`, `expectedDeliveryDate`, `items[]` (`productId`, `orderedQuantity`, `unitCost`) | PO header + items | Status should start as `draft` |
| GET | `/purchase-orders/{id}` | PO detail | path `id` | PO header, items, receipt progress | Include `receivedQuantity` per item |
| PATCH | `/purchase-orders/{id}` | Update draft/approved PO fields | `expectedDeliveryDate?`, `items?`, `status?` | updated PO | Only mutable while draft/approved; enforce item quantity rules |
| POST | `/purchase-orders/{id}/approve` | Approve PO | `approvedBy?`, `notes?` | PO with `status=approved` | Block if invalid totals/empty items |
| POST | `/purchase-orders/{id}/cancel` | Cancel PO | `reason` | PO with `status=cancelled` | No delete; cancel only |
| GET | `/purchase-orders/{id}/items` | PO line detail | path `id` | `items[]` | Can be embedded in GET detail instead |

## 7) Goods receipts / batch receiving

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| POST | `/goods-receipts` | Receive supplier delivery and create/update batches atomically | `purchaseOrderId`, `receivedAt?`, `notes?`, `lines[]` (`purchaseOrderItemId`, `productId`, `batchNumber`, `expiryDate`, `purchaseCost`, `sellingPrice`, `receivedQuantity`) | `goodsReceiptId`, created `batchIds[]`, `movementIds[]`, updated PO status, receipt totals | Core workflow: receipt header + batch creation + stock movements + PO received qty updates in one transaction |
| GET | `/goods-receipts` | List receipts | `page`, `size`, `purchaseOrderId`, `receivedBy`, `dateFrom`, `dateTo` | `items[]` | Useful for traceability |
| GET | `/goods-receipts/{id}` | Receipt detail | path `id` | receipt header, lines/batches, linked PO | Can include batch and movement references |
| POST | `/goods-receipts/{id}/void` | Void a receipt if business rules allow | `reason` | receipt status + reversal references | If voiding is supported, must reverse inventory safely; otherwise omit from implementation |

Notes:
- A receipt should not exceed ordered quantities.
- `batchNumber` must be unique per `productId`.
- Receipt creates/updates `batches.availableQuantity` and `stock_movements(type=receipt)`.
- If partial receipt is allowed, PO becomes `partially_received`; otherwise `received`.

## 8) Inventory / batches / stock movements

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/inventory/batches` | Batch-level inventory view | `page`, `size`, `productId`, `supplierId`, `status`, `expiryBefore`, `expiryAfter`, `lowStockOnly` | `items[]` batches with `availableQuantity` | Main operational inventory screen |
| GET | `/inventory/batches/{id}` | Batch detail | path `id` | batch + movement history summary | Include expiry and availability |
| GET | `/inventory/stock-movements` | Inventory ledger | `page`, `size`, `productId`, `batchId`, `type`, `referenceType`, `referenceId`, `dateFrom`, `dateTo` | `items[]` movements | Read-only ledger; source of truth for traceability |
| POST | `/inventory/adjustments` | Manual stock correction | `batchId`, `quantity`, `direction` (`increase`/`decrease`), `reason`, `referenceType?`, `referenceId?` | movement id, updated batch qty | Must not drive stock below zero; creates `type=adjustment` |
| POST | `/inventory/returns` | Return stock to inventory | `batchId`, `quantity`, `reason`, `referenceType`, `referenceId` | movement id, updated batch qty | Use for customer/supplier returns as allowed |
| POST | `/inventory/write-offs` | Remove expired/damaged stock | `batchId`, `quantity`, `reason` | movement id, updated batch qty, batch status | Quantity must be available; sets/keeps batch `expired`/`depleted` as appropriate |
| GET | `/inventory/low-stock` | Low-stock checks / dashboard alert feed | `page`, `size`, `productId?` | `items[]` with `productId`, `availableQuantity`, `reorderLevel`, `lowStockFlag` | Computed by comparing product available stock vs `reorderLevel` |
| GET | `/inventory/expiry-alerts` | Expiry monitoring / dashboard alert feed | `page`, `size`, `days=30`, `status` (`near-expiry`/`expired`) | `items[]` with batch/product/expiry data | Use for daily scan output; expired batches must be blocked from sale |
Notes:
- FEFO allocation order: earliest `expiryDate`, then earliest `receivedAt`.
- No sale may use an expired batch.
- No operation may create negative stock.
- Adjustment/return/write-off should always emit both a stock movement and audit log.

## 9) Sales / sale items

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/sales` | List/search invoices | `page`, `size`, `invoiceNumber`, `customerId`, `prescriptionId`, `status`, `paymentStatus`, `dateFrom`, `dateTo` | `items[]` sales headers | Operational sales register |
| POST | `/sales` | Create sale draft | `customerId?`, `prescriptionId?`, `items[]` (`productId`, `quantity`), `discount`, `tax`, `notes?` | sale header with `status=draft` | Draft can be edited before confirmation |
| GET | `/sales/{id}` | Sale detail | path `id` | sale header, `saleItems[]`, payment summary | Include allocated batch info after confirm |
| PATCH | `/sales/{id}` | Update draft sale | `customerId?`, `prescriptionId?`, `items?`, `discount?`, `tax?` | updated sale | Only draft sales should be editable |
| POST | `/sales/{id}/confirm` | Finalize sale, allocate FEFO batches, deduct stock | `confirmedBy?`, `paymentStatus?` | `invoiceNumber`, `status=confirmed`, `saleItems[]` with `batchId`, totals, stock impact | This is the core “sell medicine with FEFO” workflow; validate stock, expiry, and prescription before commit |
| POST | `/sales/{id}/cancel` | Cancel a draft sale | `reason` | `status=cancelled`, `paymentStatus=void` or unchanged | Cancel only before confirmation if possible |
| POST | `/sales/{id}/void` | Void a confirmed sale with reversal | `reason` | `status=void`, reversal references | If enabled, must restore stock safely and log all reversals |
| GET | `/sales/{id}/items` | Sale item detail | path `id` | `saleItems[]` | Optional if GET sale embeds items |

Notes:
- Prescription-required products must not be confirmed without a valid prescription for the same customer.
- Sale confirmation must check prescription expiry/verification status and stock availability.
- FEFO allocation should select batches with `availableQuantity > 0`, not expired, sorted by `expiryDate ASC, receivedAt ASC, id ASC`.
- If multiple batches are needed, split the sale item into multiple batch allocations internally.
- Sale confirmation creates `sale_items`, `stock_movements(type=sale)`, and audit log entries in one transaction.

## 10) Audit logs

| Method | Path | Purpose | Key request fields | Key response fields | Validation / notes |
|---|---|---|---|---|---|
| GET | `/audit-logs` | Search immutable history | `page`, `size`, `actorUserId`, `actionType`, `entityType`, `entityId`, `dateFrom`, `dateTo` | `items[]` log rows | Read-only; admin/auditor-only |
| GET | `/audit-logs/{id}` | Audit log detail | path `id` | full log row with `beforeJson`/`afterJson` | No create/update/delete endpoints |
| GET | `/audit-logs/entity/{entityType}/{entityId}` | History for a single entity | path params + paging | `items[]` log rows | Very useful for product/batch/sale traceability |

## Workflow callouts

### Receive stock
Recommended flow:
1. `POST /purchase-orders`
2. `POST /purchase-orders/{id}/approve`
3. `POST /goods-receipts`
4. Backend creates/updates `batches`, adds `stock_movements(type=receipt)`, updates PO received quantities/status, and writes `audit_logs`.

### Sell medicine with FEFO
Recommended flow:
1. `POST /sales` as draft
2. On `POST /sales/{id}/confirm`, backend:
   - validates stock and expiry
   - validates prescription if any product requires it
   - allocates eligible batches by FEFO
   - creates `sale_items` with exact `batchId` values
   - deducts batch availability via `stock_movements(type=sale)`
   - writes `audit_logs`

### Prescription validation
- Manual maintenance via `POST /prescriptions/{id}/verify` and `POST /prescriptions/{id}/reject`.
- Sale confirmation must re-check customer match, verification status, and expiry date.

### Low-stock checks
- `GET /inventory/low-stock` provides the operational view.
- A daily scheduler can compute the same logic using `products.reorderLevel` and current batch availability.

### Expiry monitoring
- `GET /inventory/expiry-alerts` exposes near-expiry/expired batches for dashboards and review.
- Expired batches must be blocked from sales.
- Write-offs should use `POST /inventory/write-offs` with audit + movement logging.

### Adjustments / returns / write-offs
- Use `POST /inventory/adjustments`, `POST /inventory/returns`, and `POST /inventory/write-offs`.
- These endpoints must never allow negative stock and must always generate a movement plus audit entry.

## Implementation notes for Spring Boot
- Use controller groups aligned to these resources: `AuthController`, `UserController`, `RoleController`, `ProductController`, `SupplierController`, `CustomerController`, `PrescriptionController`, `PurchaseOrderController`, `GoodsReceiptController`, `InventoryController`, `SalesController`, `AuditLogController`.
- Keep `confirm sale` and `receive goods` as transactional service methods, not thin controllers.
- Use `POST /sales/{id}/confirm` and `POST /goods-receipts`; keep sale payment state updates within the sale resource if duplicates are a risk.
