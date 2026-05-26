# Pharmacy Management System Database Schema

Goal: turn the UML/domain model into a production-minded relational schema with strong traceability, batch-level inventory control, FEFO allocation, and auditability.

Recommended database: PostgreSQL
Primary key strategy: UUIDs (`gen_random_uuid()`)

---

## Design principles

- Batch-level inventory is the source of truth for stock availability.
- Stock changes are recorded through immutable `stock_movements` rows.
- Critical records are never hard deleted; they are cancelled, voided, deactivated, or adjusted.
- Sale allocation must use FEFO: earliest expiry first, then earliest receipt.
- Prescription-required products must be validated before sale finalization.
- Every inventory mutation must create both a `stock_movements` row and an `audit_logs` row.

---

## Enums / controlled values

Use PostgreSQL enums or constrained text columns for these values:

- `batch_status`: `available`, `reserved`, `depleted`, `expired`, `void`
- `stock_movement_type`: `receipt`, `sale`, `return`, `adjustment`, `write_off`, `cancel`
- `sale_status`: `draft`, `confirmed`, `cancelled`, `void`
- `payment_status`: `unpaid`, `partial`, `paid`, `refunded`, `void`
- `purchase_order_status`: `draft`, `approved`, `partially_received`, `received`, `cancelled`
- `verification_status`: `pending`, `verified`, `rejected`, `expired`
- `action_type`: free text or enum such as `CREATE`, `UPDATE`, `CANCEL`, `VOID`, `RECEIPT`, `SALE`, `ADJUSTMENT`

---

## Tables

### 1) `roles`

Purpose: role catalog and permission bundle.

Columns:
- `id` UUID PK
- `name` varchar(100) not null unique
- `permissions` jsonb not null default `'[]'::jsonb`
- `active` boolean not null default true
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Indexes / constraints:
- unique index on `name`

Notes:
- Store permissions as JSONB for flexibility, e.g. `[{"code":"SALE_CREATE"}]`.

---

### 2) `users`

Purpose: application users who create sales, receive goods, and perform inventory actions.

Columns:
- `id` UUID PK
- `username` varchar(80) not null unique
- `full_name` varchar(160) not null
- `email` varchar(160) not null unique
- `password_hash` text not null
- `active` boolean not null default true
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Indexes / constraints:
- unique index on `username`
- unique index on `email`

---

### 3) `user_roles`

Purpose: many-to-many mapping between users and roles.

Columns:
- `user_id` UUID not null FK -> `users(id)` on delete restrict
- `role_id` UUID not null FK -> `roles(id)` on delete restrict
- `assigned_at` timestamptz not null default now()

Primary key:
- (`user_id`, `role_id`)

Indexes:
- index on `role_id`

---

### 4) `products`

Purpose: catalog master data.

Columns:
- `id` UUID PK
- `sku_barcode` varchar(120) not null unique
- `name` varchar(200) not null
- `brand` varchar(120)
- `generic_name` varchar(160)
- `category` varchar(120)
- `dosage_form` varchar(80)
- `strength` varchar(80)
- `pack_size` varchar(80)
- `is_prescription_required` boolean not null default false
- `reorder_level` integer not null default 0
- `active` boolean not null default true
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Constraints:
- `reorder_level >= 0`

Indexes:
- unique index on `sku_barcode`
- index on `name`
- index on `category`
- index on `active`

---

### 5) `suppliers`

Purpose: procurement partner master data.

Columns:
- `id` UUID PK
- `name` varchar(200) not null
- `phone` varchar(40)
- `email` varchar(160)
- `address` text
- `active` boolean not null default true
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Indexes:
- index on `name`
- index on `active`

---

### 6) `customers`

Purpose: customer and patient profile used by sales and prescriptions.

Columns:
- `id` UUID PK
- `name` varchar(200) not null
- `phone` varchar(40)
- `address` text
- `date_of_birth` date
- `notes` text
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Indexes:
- index on `phone`
- index on `name`

---

### 7) `prescriptions`

Purpose: prescription validation record linked to a customer.

Columns:
- `id` UUID PK
- `customer_id` UUID not null FK -> `customers(id)` on delete restrict
- `doctor_name` varchar(200) not null
- `doctor_registration_no` varchar(120)
- `issue_date` date not null
- `expiry_date` date not null
- `file_url` text
- `verification_status` varchar(20) not null
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Constraints:
- `expiry_date >= issue_date`
- `verification_status in ('pending','verified','rejected','expired')`

Indexes:
- index on `customer_id`
- index on `expiry_date`
- index on `verification_status`

Notes:
- A sale may reference a prescription only if the prescription is valid for the customer and not expired.

---

### 8) `purchase_orders`

Purpose: supplier ordering header.

Columns:
- `id` UUID PK
- `po_number` varchar(50) not null unique
- `supplier_id` UUID not null FK -> `suppliers(id)` on delete restrict
- `status` varchar(30) not null
- `expected_delivery_date` date
- `total_estimated_cost` numeric(14,2) not null default 0
- `created_by` UUID not null FK -> `users(id)` on delete restrict
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Constraints:
- `status in ('draft','approved','partially_received','received','cancelled')`
- `total_estimated_cost >= 0`

Indexes:
- unique index on `po_number`
- index on `supplier_id`
- index on `status`
- index on `created_by`

---

### 9) `purchase_order_items`

Purpose: order line items.

Columns:
- `id` UUID PK
- `purchase_order_id` UUID not null FK -> `purchase_orders(id)` on delete restrict
- `product_id` UUID not null FK -> `products(id)` on delete restrict
- `ordered_quantity` integer not null
- `received_quantity` integer not null default 0
- `unit_cost` numeric(14,2) not null
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Constraints:
- `ordered_quantity > 0`
- `received_quantity >= 0`
- `received_quantity <= ordered_quantity`
- `unit_cost >= 0`
- unique (`purchase_order_id`, `product_id`) if you want one product per PO line

Indexes:
- index on `purchase_order_id`
- index on `product_id`

---

### 10) `goods_receipts`

Purpose: formal receipt event for a purchase order.

Columns:
- `id` UUID PK
- `purchase_order_id` UUID not null FK -> `purchase_orders(id)` on delete restrict
- `received_by` UUID not null FK -> `users(id)` on delete restrict
- `received_at` timestamptz not null default now()
- `notes` text
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Indexes:
- index on `purchase_order_id`
- index on `received_by`
- index on `received_at`

Notes:
- In the app layer, a goods receipt should atomically create/update batches and stock movements.

---

### 11) `batches`

Purpose: batch-level inventory record, the core stock unit.

Columns:
- `id` UUID PK
- `product_id` UUID not null FK -> `products(id)` on delete restrict
- `supplier_id` UUID not null FK -> `suppliers(id)` on delete restrict
- `purchase_order_item_id` UUID FK -> `purchase_order_items(id)` on delete set null
- `goods_receipt_id` UUID FK -> `goods_receipts(id)` on delete set null
- `batch_number` varchar(120) not null
- `expiry_date` date not null
- `purchase_cost` numeric(14,2) not null
- `selling_price` numeric(14,2) not null
- `received_quantity` integer not null
- `available_quantity` integer not null
- `received_at` timestamptz not null default now()
- `status` varchar(20) not null
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Constraints:
- `received_quantity > 0`
- `available_quantity >= 0`
- `available_quantity <= received_quantity`
- `purchase_cost >= 0`
- `selling_price >= 0`
- `status in ('available','reserved','depleted','expired','void')`
- unique (`product_id`, `batch_number`)

Indexes:
- unique index on (`product_id`, `batch_number`)
- index on `product_id`
- index on `supplier_id`
- index on `expiry_date`
- index on `status`
- composite index on (`product_id`, `status`, `expiry_date`, `received_at`) for FEFO allocation

Notes:
- `available_quantity` is a cached balance and must be updated in the same transaction as stock movements.
- FEFO selection order: `expiry_date ASC, received_at ASC, id ASC`.

---

### 12) `stock_movements`

Purpose: immutable inventory ledger.

Columns:
- `id` UUID PK
- `product_id` UUID not null FK -> `products(id)` on delete restrict
- `batch_id` UUID not null FK -> `batches(id)` on delete restrict
- `type` varchar(20) not null
- `quantity` integer not null
- `reference_type` varchar(50) not null
- `reference_id` UUID not null
- `reason` text
- `created_by` UUID not null FK -> `users(id)` on delete restrict
- `created_at` timestamptz not null default now()

Constraints:
- `quantity > 0`
- `type in ('receipt','sale','return','adjustment','write_off','cancel')`

Indexes:
- index on `batch_id`
- index on `product_id`
- index on (`reference_type`, `reference_id`)
- index on `created_by`
- index on `created_at`

Notes:
- A sale movement should reduce batch quantity.
- A receipt movement should increase batch quantity.
- Return/adjustment/write-off must also be represented here.

---

### 13) `sales`

Purpose: invoice / checkout header.

Columns:
- `id` UUID PK
- `invoice_number` varchar(50) not null unique
- `customer_id` UUID FK -> `customers(id)` on delete set null
- `prescription_id` UUID FK -> `prescriptions(id)` on delete set null
- `status` varchar(20) not null
- `subtotal` numeric(14,2) not null default 0
- `discount` numeric(14,2) not null default 0
- `tax` numeric(14,2) not null default 0
- `total` numeric(14,2) not null default 0
- `payment_status` varchar(20) not null
- `created_by` UUID not null FK -> `users(id)` on delete restrict
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Constraints:
- `status in ('draft','confirmed','cancelled','void')`
- `payment_status in ('unpaid','partial','paid','refunded','void')`
- `subtotal >= 0`
- `discount >= 0`
- `tax >= 0`
- `total >= 0`

Indexes:
- unique index on `invoice_number`
- index on `customer_id`
- index on `prescription_id`
- index on `created_by`
- index on `created_at`
- index on `status`

Notes:
- A prescription is optional at schema level, but the application must require it for prescription-only products.

---

### 14) `sale_items`

Purpose: line items tied to exact batches for traceability.

Columns:
- `id` UUID PK
- `sale_id` UUID not null FK -> `sales(id)` on delete restrict
- `product_id` UUID not null FK -> `products(id)` on delete restrict
- `batch_id` UUID not null FK -> `batches(id)` on delete restrict
- `quantity` integer not null
- `unit_price` numeric(14,2) not null
- `discount` numeric(14,2) not null default 0
- `line_total` numeric(14,2) not null
- `created_at` timestamptz not null default now()
- `updated_at` timestamptz not null default now()

Constraints:
- `quantity > 0`
- `unit_price >= 0`
- `discount >= 0`
- `line_total >= 0`

Indexes:
- index on `sale_id`
- index on `product_id`
- index on `batch_id`

Notes:
- The batch association is mandatory so the system can prove which expiry lot was sold.

---

### 15) `audit_logs`

Purpose: immutable change history for critical business actions.

Columns:
- `id` UUID PK
- `actor_user_id` UUID not null FK -> `users(id)` on delete restrict
- `action_type` varchar(40) not null
- `entity_type` varchar(60) not null
- `entity_id` UUID not null
- `before_json` jsonb
- `after_json` jsonb
- `timestamp` timestamptz not null default now()
- `ip_address` inet

Indexes:
- index on `actor_user_id`
- index on (`entity_type`, `entity_id`)
- index on `timestamp`
- index on `action_type`

Notes:
- Use this for all stock changes, sales changes, procurement changes, and critical admin edits.

---

## Relationship summary

- `products` 1 -> many `batches`
- `suppliers` 1 -> many `batches`
- `batches` 1 -> many `stock_movements`
- `products` 1 -> many `stock_movements`
- `users` 1 -> many `stock_movements`
- `customers` 1 -> many `sales`
- `customers` 1 -> many `prescriptions`
- `prescriptions` 0..1 -> many `sales` (optional link from sale side)
- `users` 1 -> many `sales`
- `users` 1 -> many `purchase_orders`
- `purchase_orders` 1 -> many `purchase_order_items`
- `purchase_orders` 1 -> many `goods_receipts`
- `purchase_order_items` 1 -> many `batches` if you ever receive the same PO line in multiple batches
- `sales` 1 -> many `sale_items`
- `products` 1 -> many `sale_items`
- `batches` 1 -> many `sale_items`
- `users` many <-> many `roles` through `user_roles`
- `users` 1 -> many `audit_logs`

---

## Transaction rules to enforce in application code

1. Receive stock
- Create `goods_receipts`
- Create or update `batches`
- Insert a `stock_movements` row of type `receipt`
- Update `purchase_order_items.received_quantity`
- Insert `audit_logs` row
- Commit as one transaction

2. Sell medicine
- Validate stock is available and not expired
- Validate prescription if required
- Choose batches using FEFO
- Insert `sales` and `sale_items`
- Reduce `batches.available_quantity`
- Insert `stock_movements` rows of type `sale`
- Insert `audit_logs` row(s)
- Commit as one transaction

3. Adjust / return / write-off
- Create a movement with a valid reason
- Update batch quantity
- Log audit details
- Never hard delete

---

## Suggested migration order

1. `roles`, `users`, `user_roles`
2. `products`, `suppliers`, `customers`
3. `prescriptions`
4. `purchase_orders`, `purchase_order_items`, `goods_receipts`
5. `batches`, `stock_movements`
6. `sales`, `sale_items`
7. `audit_logs`

---

## Next step

Convert this schema into SQL migrations and then define the first API endpoints.
