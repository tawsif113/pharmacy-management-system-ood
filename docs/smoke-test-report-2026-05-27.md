# PMS Exhaustive Smoke Test Report

- Date: 2026-05-27T20:56:26+06:00
- Base URL: http://localhost:8081
- Smoke user: `37a4af81-9df0-4d8e-b49f-80d0706f4eb4`
- Generated data suffix: `393a9f1c`
- Requests executed: 48
- Passed: 48
- Failed: 0
- Overall status: PASS

## Ordered workflow covered
- Master data: suppliers, customers, products, prescriptions
- Procurement: purchase orders -> approve/cancel -> goods receipts
- Inventory: batches, stock movements, low stock alerts, expiry alerts, adjustments, returns, write-offs

## Created resources
- supplier_id: `3d74ae31-0e90-403a-b0ec-7fc06050f9f1`
- customer_id: `dcddfa5f-8ea8-47d4-ac27-2ced6cfbf6f4`
- product_a_id: `9d134992-cc05-45a6-ba0c-40392d4a555b`
- product_b_id: `13d8fb80-611b-4baa-baf1-ca1cd0b86914`
- prescription_a_id: `60215e32-51d2-4222-8fc2-c65eed42469d`
- prescription_b_id: `cab144c1-2afb-4225-ba34-ed8e0029fdd5`
- purchase_order_1_id: `0303f6d6-d95f-45a6-b2ff-2a5e5d679dc1`
- po1_item_a_id: `f464c703-b0ce-4aa8-8727-a3ec62a86b66`
- po1_item_b_id: `04c9261f-3664-44e2-b776-f2f41a92ee4f`
- purchase_order_2_id: `90ae0507-b90f-4849-919d-bea08e5bb008`
- goods_receipt_id: `f57aa244-ae2a-4790-b7bd-235ff3a6a32c`
- batch_a_id: `34da2de4-62b5-4609-9ea7-e170fd41b5fa`
- batch_b_id: `d53513cf-aba8-4a21-8037-88a8ad6476a8`

## Database snapshot after smoke test
- users: 1
- suppliers: 7
- customers: 7
- products: 14
- prescriptions: 9
- purchase_orders: 12
- goods_receipts: 5
- batches: 7
- stock_movements: 13

## Notes
- The smoke test intentionally generated live database data.
- The workflow respects endpoint ordering where downstream calls depend on upstream records.
- For procurement and inventory operations, the existing smoke user row was reused from the local database.

## Master data endpoints

| # | Method | Endpoint | Status | Expected | Result |
|---:|---|---|---:|---|---|
| 1 | GET | `/products` | 200 | [200] | PASS |
| 2 | POST | `/suppliers` | 201 | [201] | PASS |
| 3 | GET | `/suppliers/3d74ae31-0e90-403a-b0ec-7fc06050f9f1` | 200 | [200] | PASS |
| 4 | PATCH | `/suppliers/3d74ae31-0e90-403a-b0ec-7fc06050f9f1` | 200 | [200] | PASS |
| 5 | GET | `/suppliers` | 200 | [200] | PASS |
| 6 | POST | `/customers` | 201 | [201] | PASS |
| 7 | GET | `/customers/dcddfa5f-8ea8-47d4-ac27-2ced6cfbf6f4` | 200 | [200] | PASS |
| 8 | PATCH | `/customers/dcddfa5f-8ea8-47d4-ac27-2ced6cfbf6f4` | 200 | [200] | PASS |
| 9 | GET | `/customers` | 200 | [200] | PASS |
| 10 | POST | `/products` | 201 | [201] | PASS |
| 11 | GET | `/products/9d134992-cc05-45a6-ba0c-40392d4a555b` | 200 | [200] | PASS |
| 12 | PATCH | `/products/9d134992-cc05-45a6-ba0c-40392d4a555b` | 200 | [200] | PASS |
| 13 | POST | `/products` | 201 | [201] | PASS |
| 14 | GET | `/products/13d8fb80-611b-4baa-baf1-ca1cd0b86914` | 200 | [200] | PASS |
| 15 | PATCH | `/products/13d8fb80-611b-4baa-baf1-ca1cd0b86914` | 200 | [200] | PASS |
| 16 | GET | `/products` | 200 | [200] | PASS |
| 17 | POST | `/prescriptions` | 201 | [201] | PASS |
| 18 | GET | `/prescriptions/60215e32-51d2-4222-8fc2-c65eed42469d` | 200 | [200] | PASS |
| 19 | PATCH | `/prescriptions/60215e32-51d2-4222-8fc2-c65eed42469d` | 200 | [200] | PASS |
| 20 | POST | `/prescriptions/60215e32-51d2-4222-8fc2-c65eed42469d/verify` | 200 | [200] | PASS |
| 21 | POST | `/prescriptions` | 201 | [201] | PASS |
| 22 | POST | `/prescriptions/cab144c1-2afb-4225-ba34-ed8e0029fdd5/reject` | 200 | [200] | PASS |
| 23 | GET | `/prescriptions/cab144c1-2afb-4225-ba34-ed8e0029fdd5` | 200 | [200] | PASS |
| 24 | GET | `/prescriptions` | 200 | [200] | PASS |

## Procurement endpoints

| # | Method | Endpoint | Status | Expected | Result |
|---:|---|---|---:|---|---|
| 1 | POST | `/purchase-orders` | 201 | [201] | PASS |
| 2 | GET | `/purchase-orders/0303f6d6-d95f-45a6-b2ff-2a5e5d679dc1` | 200 | [200] | PASS |
| 3 | GET | `/purchase-orders` | 200 | [200] | PASS |
| 4 | POST | `/purchase-orders/0303f6d6-d95f-45a6-b2ff-2a5e5d679dc1/approve` | 200 | [200] | PASS |
| 5 | POST | `/purchase-orders` | 201 | [201] | PASS |
| 6 | POST | `/purchase-orders/90ae0507-b90f-4849-919d-bea08e5bb008/cancel` | 200 | [200] | PASS |
| 7 | GET | `/purchase-orders/90ae0507-b90f-4849-919d-bea08e5bb008` | 200 | [200] | PASS |
| 8 | GET | `/purchase-orders` | 200 | [200] | PASS |
| 9 | POST | `/goods-receipts` | 201 | [201] | PASS |
| 10 | GET | `/goods-receipts/f57aa244-ae2a-4790-b7bd-235ff3a6a32c` | 200 | [200] | PASS |
| 11 | GET | `/goods-receipts` | 200 | [200] | PASS |

## Inventory endpoints

| # | Method | Endpoint | Status | Expected | Result |
|---:|---|---|---:|---|---|
| 1 | GET | `/inventory/batches` | 200 | [200] | PASS |
| 2 | GET | `/inventory/batches/34da2de4-62b5-4609-9ea7-e170fd41b5fa` | 200 | [200] | PASS |
| 3 | GET | `/inventory/batches` | 200 | [200] | PASS |
| 4 | GET | `/inventory/alerts/low-stock` | 200 | [200] | PASS |
| 5 | GET | `/inventory/alerts/expiring` | 200 | [200] | PASS |
| 6 | GET | `/inventory/alerts/expiring` | 200 | [200] | PASS |
| 7 | POST | `/inventory/batches/d53513cf-aba8-4a21-8037-88a8ad6476a8/adjust` | 200 | [200] | PASS |
| 8 | POST | `/inventory/batches/d53513cf-aba8-4a21-8037-88a8ad6476a8/return` | 200 | [200] | PASS |
| 9 | POST | `/inventory/batches/d53513cf-aba8-4a21-8037-88a8ad6476a8/write-off` | 200 | [200] | PASS |
| 10 | GET | `/inventory/stock-movements` | 200 | [200] | PASS |
| 11 | GET | `/inventory/stock-movements` | 200 | [200] | PASS |
| 12 | GET | `/inventory/stock-movements` | 200 | [200] | PASS |
| 13 | GET | `/inventory/stock-movements/6798ee88-839c-4891-93ce-542b712fb387` | 200 | [200] | PASS |
