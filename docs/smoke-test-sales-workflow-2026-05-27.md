# PMS Sales Workflow Smoke Test Report

- Date: 2026-05-27T21:41:42+06:00
- Base URL: http://localhost:8081
- Smoke user: `37a4af81-9df0-4d8e-b49f-80d0706f4eb4`
- Generated data suffix: `64513fa0`
- Requests executed: 41
- Passed: 41
- Failed: 0
- Overall status: PASS

## Ordered workflow covered
- Supplier/customer/product/prescription setup
- Procurement: purchase orders -> approvals -> goods receipts -> batches
- Inventory: batch visibility, alerts, stock movements
- Sales: draft -> FEFO allocation -> confirm -> void, plus draft -> cancel

## Created resources
- sale_draft_rx_id: `3d8da51f-bbbb-45bc-b250-0dc738361bc7`
- sale_confirmed_rx_id: `3d8da51f-bbbb-45bc-b250-0dc738361bc7`
- sale_voided_rx_id: `3d8da51f-bbbb-45bc-b250-0dc738361bc7`
- sale_draft_otc_id: `c793cd6a-8af1-4131-ae80-6fa03dbbef99`

## Database snapshot after smoke test
- users: 1
- suppliers: 9
- customers: 9
- products: 18
- prescriptions: 11
- purchase_orders: 18
- goods_receipts: 8
- batches: 10
- stock_movements: 20
- sales: 2
- sale_items: 3

## Notes
- The smoke test intentionally generated live database data.
- FEFO allocation was verified by creating a draft sale that split one product across two batches.
- The confirmed sale was then voided to verify stock restoration and reversal movements.
- A second sale draft was cancelled to verify the draft-only cancellation path.

## Sales endpoints

| # | Method | Endpoint | Status | Expected | Result |
|---:|---|---|---:|---|---|
| 1 | POST | `/sales` | 201 | [201] | PASS |
| 2 | GET | `/sales/3d8da51f-bbbb-45bc-b250-0dc738361bc7` | 200 | [200] | PASS |
| 3 | GET | `/sales` | 200 | [200] | PASS |
| 4 | POST | `/sales/3d8da51f-bbbb-45bc-b250-0dc738361bc7/confirm` | 200 | [200] | PASS |
| 5 | GET | `/sales/3d8da51f-bbbb-45bc-b250-0dc738361bc7` | 200 | [200] | PASS |
| 6 | GET | `/sales` | 200 | [200] | PASS |
| 7 | POST | `/sales/3d8da51f-bbbb-45bc-b250-0dc738361bc7/void` | 200 | [200] | PASS |
| 8 | GET | `/sales/3d8da51f-bbbb-45bc-b250-0dc738361bc7` | 200 | [200] | PASS |
| 9 | GET | `/sales` | 200 | [200] | PASS |
| 10 | POST | `/sales` | 201 | [201] | PASS |
| 11 | POST | `/sales/c793cd6a-8af1-4131-ae80-6fa03dbbef99/cancel` | 200 | [200] | PASS |
| 12 | GET | `/sales/c793cd6a-8af1-4131-ae80-6fa03dbbef99` | 200 | [200] | PASS |
| 13 | GET | `/sales` | 200 | [200] | PASS |

## Inventory endpoints used by sales flow

| # | Method | Endpoint | Status | Expected | Result |
|---:|---|---|---:|---|---|
| 1 | GET | `/inventory/batches` | 200 | [200] | PASS |
| 2 | GET | `/inventory/batches/0edf3642-4d4a-4ce6-9f40-626acd22f22f` | 200 | [200] | PASS |
| 3 | GET | `/inventory/batches/a7487dc8-64e2-4a98-bf9f-15a2d855d696` | 200 | [200] | PASS |
| 4 | GET | `/inventory/batches/9b987d4f-b807-4776-9cf0-f74dbe11f569` | 200 | [200] | PASS |
| 5 | GET | `/inventory/alerts/low-stock` | 200 | [200] | PASS |
| 6 | GET | `/inventory/alerts/expiring` | 200 | [200] | PASS |
| 7 | GET | `/inventory/stock-movements` | 200 | [200] | PASS |
| 8 | GET | `/inventory/stock-movements` | 200 | [200] | PASS |
| 9 | GET | `/inventory/batches/0edf3642-4d4a-4ce6-9f40-626acd22f22f` | 200 | [200] | PASS |
| 10 | GET | `/inventory/batches/a7487dc8-64e2-4a98-bf9f-15a2d855d696` | 200 | [200] | PASS |
| 11 | GET | `/inventory/batches/9b987d4f-b807-4776-9cf0-f74dbe11f569` | 200 | [200] | PASS |

## Upstream procurement/master-data endpoints

| # | Method | Endpoint | Status | Expected | Result |
|---:|---|---|---:|---|---|
| 1 | GET | `/products` | 200 | [200] | PASS |
| 2 | POST | `/suppliers` | 201 | [201] | PASS |
| 3 | POST | `/customers` | 201 | [201] | PASS |
| 4 | POST | `/products` | 201 | [201] | PASS |
| 5 | POST | `/products` | 201 | [201] | PASS |
| 6 | PATCH | `/products/4a7390b3-0feb-4eb5-a894-85095dd0140f` | 200 | [200] | PASS |
| 7 | POST | `/prescriptions` | 201 | [201] | PASS |
| 8 | POST | `/prescriptions/4ae3278d-768c-45dc-bd00-d685d468ff8c/verify` | 200 | [200] | PASS |
| 9 | POST | `/purchase-orders` | 201 | [201] | PASS |
| 10 | POST | `/purchase-orders/b854c901-1d1a-4dde-a1a5-3667102991f0/approve` | 200 | [200] | PASS |
| 11 | POST | `/purchase-orders` | 201 | [201] | PASS |
| 12 | POST | `/purchase-orders/6492d8a8-61ea-44bc-a651-fd5f3412bdc4/approve` | 200 | [200] | PASS |
| 13 | POST | `/purchase-orders` | 201 | [201] | PASS |
| 14 | POST | `/purchase-orders/2b01dfec-e78b-4fc3-a385-40ad46967cf5/approve` | 200 | [200] | PASS |
| 15 | POST | `/goods-receipts` | 201 | [201] | PASS |
| 16 | POST | `/goods-receipts` | 201 | [201] | PASS |
| 17 | POST | `/goods-receipts` | 201 | [201] | PASS |
