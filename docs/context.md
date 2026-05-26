# Pharmacy Management System Context

Date: 25th May

## Goal
Build a production-minded Pharmacy Management System for a rented pharmacy business. This is not a practice toy; it should be designed for real-world traceability, inventory safety, and operational workflows.

## Core Domains
1. Catalog
   - Product master data
   - Fields: id, sku/barcode, name, brand, genericName, category, dosageForm, strength, packSize, isPrescriptionRequired, reorderLevel, active

2. Inventory
   - Batch-level tracking with expiry dates
   - Stock movements as the source of truth
   - Fields:
     - Batch: id, productId, batchNumber, expiryDate, purchaseCost, sellingPrice, receivedQuantity, availableQuantity, supplierId, receivedAt, status
     - StockMovement: id, productId, batchId, type, quantity, referenceType, referenceId, reason, createdBy, createdAt

3. Sales
   - Sale/invoice flow with optional customer and prescription links
   - Fields:
     - Sale: id, invoiceNumber, customerId, prescriptionId, status, subtotal, discount, tax, total, paymentStatus, createdBy, createdAt
     - SaleItem: id, saleId, productId, batchId, quantity, unitPrice, discount, lineTotal
     - Customer: id, name, phone, address, dateOfBirth, notes
     - Prescription: id, customerId, doctorName, doctorRegistrationNo, issueDate, expiryDate, fileUrl, verificationStatus

4. Procurement
   - Supplier ordering and goods receipt flow
   - Fields:
     - Supplier: id, name, phone, email, address, active
     - PurchaseOrder: id, poNumber, supplierId, status, expectedDeliveryDate, totalEstimatedCost, createdBy, createdAt
     - PurchaseOrderItem: id, purchaseOrderId, productId, orderedQuantity, receivedQuantity, unitCost
     - GoodsReceipt: id, purchaseOrderId, receivedBy, receivedAt, notes

5. Identity + Audit
   - Roles, permissions, and change history
   - Fields:
     - User: id, username, fullName, email, passwordHash, active
     - Role: id, name, permissions
     - AuditLog: id, actorUserId, actionType, entityType, entityId, beforeJson, afterJson, timestamp, ipAddress

## Important Business Rules
- No negative stock
- No sale of expired stock
- Prescription-required products must validate against a valid prescription
- Every stock change must create a StockMovement and AuditLog
- No hard delete for critical records; use cancel/void/adjust
- Track stock at the batch level
- Use FEFO: First Expiry, First Out

## Core Workflows
### 1. Receive new stock
Supplier delivery -> PurchaseOrder -> GoodsReceipt -> Batch creation -> RECEIPT stock movement -> inventory update -> AuditLog

### 2. Sell medicine at the counter
Search product -> validate stock/expiry/prescription -> allocate earliest-expiring eligible batch -> create sale -> deduct stock -> invoice -> payment status -> AuditLog

### 3. Low-stock alert
Daily job checks available quantity against reorderLevel -> create alert if below threshold -> show in dashboard

### 4. Expiry monitoring
Nightly job scans batches -> flag near-expiry/expired items -> block expired sales -> write-off if needed -> AuditLog

### 5. Returns/adjustments
Create adjustment request -> choose reason -> approve if needed -> create ADJUSTMENT/RETURN/WRITE_OFF movement -> update inventory -> AuditLog

## Suggested Aggregate Boundaries
- Product Aggregate: Product
- Inventory Aggregate: Batch, StockMovement, inventory policy logic
- Sales Aggregate: Sale, SaleItem, payment logic
- Procurement Aggregate: Supplier, PurchaseOrder, PurchaseOrderItem, GoodsReceipt
- Identity/Audit Aggregate: User, Role, AuditLog

## MVP Scope
Start with:
- Product
- Batch
- StockMovement
- Sale
- SaleItem
- Supplier
- PurchaseOrder
- PurchaseOrderItem
- Customer
- AuditLog
- User
- Role

## Suggested Build Order
1. Core data model
2. Inventory flow
3. Sales flow
4. Procurement flow
5. Controls: prescription validation, permissions, audit trail, reports

## Next Recommended Step
Create the class diagram and aggregate design, then move into the database schema and API endpoints.

## Current Checkpoint
We have already completed the following:
- UML class diagram: `/home/kazimtr/Days/25thMay/pharmacy-mvp-uml.excalidraw`
- Database schema notes: `/home/kazimtr/Days/25thMay/database-schema.md`
- Initial PostgreSQL migration: `/home/kazimtr/Days/25thMay/migrations/001_initial_schema.sql`
- API endpoint spec: `/home/kazimtr/Days/25thMay/api-endpoints.md`

Current status:
- Domain model is mapped
- Database schema is drafted and migrated
- API surface is drafted and reviewed

Best next step when resuming:
- Start Spring Boot backend implementation from the migration + API spec
- First backend work should wire the core entities, repositories, and the inventory/sales workflows
