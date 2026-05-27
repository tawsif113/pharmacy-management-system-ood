# Product Catalog Feature

This feature manages the pharmacy product master data.

What it does:
- creates new products
- updates product details
- lists products with search and simple filters
- returns one product by id

Why it matters:
- products are the foundation for procurement, inventory, and sales
- every other workflow needs a clean product master before it can work correctly

Current API surface:
- `POST /products`
- `GET /products`
- `GET /products/{id}`
- `PATCH /products/{id}`

Key design points:
- controllers stay thin
- service handles orchestration and validation
- repository handles persistence only
- the product entity remains the single source of truth for product master data

Current scope:
- no frontend
- no authentication logic yet
- no stock summary endpoint yet; that belongs to the inventory feature
