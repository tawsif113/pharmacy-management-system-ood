# Master Data: Suppliers, Customers, and Prescriptions

This backend feature group adds the shared master data that the procurement and sales workflows depend on.

## What it covers

### Suppliers
Suppliers are the source records for purchasing stock.
The API supports:
- `POST /suppliers`
- `GET /suppliers`
- `GET /suppliers/{id}`
- `PATCH /suppliers/{id}`

Supplier fields managed here:
- name
- phone
- email
- address
- active

Business behavior:
- supplier names are required
- no hard delete
- inactive suppliers stay in the system for traceability
- list search can filter by name, phone, or email

### Customers
Customers are the patient or buyer profiles linked to sales and prescriptions.
The API supports:
- `POST /customers`
- `GET /customers`
- `GET /customers/{id}`
- `PATCH /customers/{id}`

Customer fields managed here:
- name
- phone
- address
- date of birth
- notes

Business behavior:
- customer names are required
- no hard delete
- list search can filter by name, phone, and date of birth
- customer data stays simple so sales and prescription validation can reuse it later

### Prescriptions
Prescriptions are the controlled records used to support prescription-only medicine sales.
The API supports:
- `POST /prescriptions`
- `GET /prescriptions`
- `GET /prescriptions/{id}`
- `PATCH /prescriptions/{id}`
- `POST /prescriptions/{id}/verify`
- `POST /prescriptions/{id}/reject`

Prescription fields managed here:
- customer
- doctor name
- doctor registration number
- issue date
- expiry date
- file URL
- verification status

Business behavior:
- a prescription must belong to an existing customer
- doctor name is required
- issue date and expiry date are required
- expiry date cannot be before issue date
- default status is `pending`
- verify/reject actions update the status explicitly
- list filtering supports customer, verification status, and expiry window filters

## Why this feature group matters

This group forms the backbone for the rest of the system:
- procurement needs suppliers
- sales need customers
- prescription-only sales need prescriptions
- future inventory and sales checks can use these records without duplicating business data

## Design notes

- Controllers stay thin and only handle HTTP translation.
- Services own validation and workflow rules.
- Repositories handle persistence and query filtering.
- The implementation stays intentionally compact and backend-first so the next workflow features can build on stable master data.
