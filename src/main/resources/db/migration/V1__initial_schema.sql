-- Pharmacy Management System initial PostgreSQL migration
-- Source of truth: database-schema.md
-- Production-minded baseline with UUID primary keys, enum status types,
-- referential integrity, lookup indexes, and updated_at maintenance.

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE SCHEMA IF NOT EXISTS pms;
SET search_path TO pms;

-- -----------------------------------------------------------------------------
-- Enum types
-- -----------------------------------------------------------------------------
DO $$ BEGIN
    CREATE TYPE batch_status_enum AS ENUM ('available', 'reserved', 'depleted', 'expired', 'void');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE stock_movement_type_enum AS ENUM ('receipt', 'sale', 'return', 'adjustment', 'write_off', 'cancel');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE sale_status_enum AS ENUM ('draft', 'confirmed', 'cancelled', 'void');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE payment_status_enum AS ENUM ('unpaid', 'partial', 'paid', 'refunded', 'void');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE purchase_order_status_enum AS ENUM ('draft', 'approved', 'partially_received', 'received', 'cancelled');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE verification_status_enum AS ENUM ('pending', 'verified', 'rejected', 'expired');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$ BEGIN
    CREATE TYPE action_type_enum AS ENUM ('CREATE', 'UPDATE', 'CANCEL', 'VOID', 'RECEIPT', 'SALE', 'ADJUSTMENT');
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

-- -----------------------------------------------------------------------------
-- Updated-at trigger helper
-- -----------------------------------------------------------------------------
CREATE OR REPLACE FUNCTION set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = now();
    RETURN NEW;
END;
$$;

-- -----------------------------------------------------------------------------
-- Core identity / access control
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS roles (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(100) NOT NULL UNIQUE,
    permissions jsonb NOT NULL DEFAULT '[]'::jsonb,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS users (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    username varchar(80) NOT NULL UNIQUE,
    full_name varchar(160) NOT NULL,
    email varchar(160) NOT NULL UNIQUE,
    password_hash text NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    role_id uuid NOT NULL REFERENCES roles(id) ON DELETE RESTRICT,
    assigned_at timestamptz NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_user_roles_role_id ON user_roles (role_id);

-- -----------------------------------------------------------------------------
-- Catalog / master data
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sku_barcode varchar(120) NOT NULL UNIQUE,
    name varchar(200) NOT NULL,
    brand varchar(120),
    generic_name varchar(160),
    category varchar(120),
    dosage_form varchar(80),
    strength varchar(80),
    pack_size varchar(80),
    is_prescription_required boolean NOT NULL DEFAULT false,
    reorder_level integer NOT NULL DEFAULT 0,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT products_reorder_level_nonnegative CHECK (reorder_level >= 0)
);

CREATE TABLE IF NOT EXISTS suppliers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(200) NOT NULL,
    phone varchar(40),
    email varchar(160),
    address text,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS customers (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    name varchar(200) NOT NULL,
    phone varchar(40),
    address text,
    date_of_birth date,
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_products_name ON products (name);
CREATE INDEX IF NOT EXISTS idx_products_category ON products (category);
CREATE INDEX IF NOT EXISTS idx_products_active ON products (active);
CREATE INDEX IF NOT EXISTS idx_suppliers_name ON suppliers (name);
CREATE INDEX IF NOT EXISTS idx_suppliers_active ON suppliers (active);
CREATE INDEX IF NOT EXISTS idx_customers_phone ON customers (phone);
CREATE INDEX IF NOT EXISTS idx_customers_name ON customers (name);

-- -----------------------------------------------------------------------------
-- Clinical validation
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS prescriptions (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id uuid NOT NULL REFERENCES customers(id) ON DELETE RESTRICT,
    doctor_name varchar(200) NOT NULL,
    doctor_registration_no varchar(120),
    issue_date date NOT NULL,
    expiry_date date NOT NULL,
    file_url text,
    verification_status verification_status_enum NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT prescriptions_expiry_after_issue CHECK (expiry_date >= issue_date)
);

CREATE INDEX IF NOT EXISTS idx_prescriptions_customer_id ON prescriptions (customer_id);
CREATE INDEX IF NOT EXISTS idx_prescriptions_expiry_date ON prescriptions (expiry_date);
CREATE INDEX IF NOT EXISTS idx_prescriptions_verification_status ON prescriptions (verification_status);

-- -----------------------------------------------------------------------------
-- Procurement
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS purchase_orders (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    po_number varchar(50) NOT NULL UNIQUE,
    supplier_id uuid NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    status purchase_order_status_enum NOT NULL,
    expected_delivery_date date,
    total_estimated_cost numeric(14,2) NOT NULL DEFAULT 0,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT purchase_orders_total_estimated_cost_nonnegative CHECK (total_estimated_cost >= 0)
);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id uuid NOT NULL REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    ordered_quantity integer NOT NULL,
    received_quantity integer NOT NULL DEFAULT 0,
    unit_cost numeric(14,2) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT purchase_order_items_ordered_quantity_positive CHECK (ordered_quantity > 0),
    CONSTRAINT purchase_order_items_received_quantity_nonnegative CHECK (received_quantity >= 0),
    CONSTRAINT purchase_order_items_received_not_exceed_ordered CHECK (received_quantity <= ordered_quantity),
    CONSTRAINT purchase_order_items_unit_cost_nonnegative CHECK (unit_cost >= 0),
    CONSTRAINT purchase_order_items_unique_product_per_po UNIQUE (purchase_order_id, product_id)
);

CREATE TABLE IF NOT EXISTS goods_receipts (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id uuid NOT NULL REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    received_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    received_at timestamptz NOT NULL DEFAULT now(),
    notes text,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier_id ON purchase_orders (supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_status ON purchase_orders (status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_created_by ON purchase_orders (created_by);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_purchase_order_id ON purchase_order_items (purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_purchase_order_items_product_id ON purchase_order_items (product_id);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_purchase_order_id ON goods_receipts (purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_received_by ON goods_receipts (received_by);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_received_at ON goods_receipts (received_at);

-- -----------------------------------------------------------------------------
-- Inventory
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS batches (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    supplier_id uuid NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    purchase_order_item_id uuid REFERENCES purchase_order_items(id) ON DELETE SET NULL,
    goods_receipt_id uuid REFERENCES goods_receipts(id) ON DELETE SET NULL,
    batch_number varchar(120) NOT NULL,
    expiry_date date NOT NULL,
    purchase_cost numeric(14,2) NOT NULL,
    selling_price numeric(14,2) NOT NULL,
    received_quantity integer NOT NULL,
    available_quantity integer NOT NULL,
    received_at timestamptz NOT NULL DEFAULT now(),
    status batch_status_enum NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT batches_received_quantity_positive CHECK (received_quantity > 0),
    CONSTRAINT batches_available_quantity_nonnegative CHECK (available_quantity >= 0),
    CONSTRAINT batches_available_le_received CHECK (available_quantity <= received_quantity),
    CONSTRAINT batches_purchase_cost_nonnegative CHECK (purchase_cost >= 0),
    CONSTRAINT batches_selling_price_nonnegative CHECK (selling_price >= 0),
    CONSTRAINT batches_unique_product_batch UNIQUE (product_id, batch_number)
);

CREATE TABLE IF NOT EXISTS stock_movements (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_id uuid NOT NULL REFERENCES batches(id) ON DELETE RESTRICT,
    type stock_movement_type_enum NOT NULL,
    quantity integer NOT NULL,
    reference_type varchar(50) NOT NULL,
    reference_id uuid NOT NULL,
    reason text,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT stock_movements_quantity_positive CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_batches_product_id ON batches (product_id);
CREATE INDEX IF NOT EXISTS idx_batches_supplier_id ON batches (supplier_id);
CREATE INDEX IF NOT EXISTS idx_batches_expiry_date ON batches (expiry_date);
CREATE INDEX IF NOT EXISTS idx_batches_status ON batches (status);
CREATE INDEX IF NOT EXISTS idx_batches_fefo_lookup ON batches (product_id, status, expiry_date, received_at);
CREATE INDEX IF NOT EXISTS idx_stock_movements_batch_id ON stock_movements (batch_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_product_id ON stock_movements (product_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_reference ON stock_movements (reference_type, reference_id);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_by ON stock_movements (created_by);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements (created_at);

-- -----------------------------------------------------------------------------
-- Sales
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS sales (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    invoice_number varchar(50) NOT NULL UNIQUE,
    customer_id uuid REFERENCES customers(id) ON DELETE SET NULL,
    prescription_id uuid REFERENCES prescriptions(id) ON DELETE SET NULL,
    status sale_status_enum NOT NULL,
    subtotal numeric(14,2) NOT NULL DEFAULT 0,
    discount numeric(14,2) NOT NULL DEFAULT 0,
    tax numeric(14,2) NOT NULL DEFAULT 0,
    total numeric(14,2) NOT NULL DEFAULT 0,
    payment_status payment_status_enum NOT NULL,
    created_by uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT sales_subtotal_nonnegative CHECK (subtotal >= 0),
    CONSTRAINT sales_discount_nonnegative CHECK (discount >= 0),
    CONSTRAINT sales_tax_nonnegative CHECK (tax >= 0),
    CONSTRAINT sales_total_nonnegative CHECK (total >= 0)
);

CREATE TABLE IF NOT EXISTS sale_items (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    sale_id uuid NOT NULL REFERENCES sales(id) ON DELETE RESTRICT,
    product_id uuid NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    batch_id uuid NOT NULL REFERENCES batches(id) ON DELETE RESTRICT,
    quantity integer NOT NULL,
    unit_price numeric(14,2) NOT NULL,
    discount numeric(14,2) NOT NULL DEFAULT 0,
    line_total numeric(14,2) NOT NULL,
    created_at timestamptz NOT NULL DEFAULT now(),
    updated_at timestamptz NOT NULL DEFAULT now(),
    CONSTRAINT sale_items_quantity_positive CHECK (quantity > 0),
    CONSTRAINT sale_items_unit_price_nonnegative CHECK (unit_price >= 0),
    CONSTRAINT sale_items_discount_nonnegative CHECK (discount >= 0),
    CONSTRAINT sale_items_line_total_nonnegative CHECK (line_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_sales_customer_id ON sales (customer_id);
CREATE INDEX IF NOT EXISTS idx_sales_prescription_id ON sales (prescription_id);
CREATE INDEX IF NOT EXISTS idx_sales_created_by ON sales (created_by);
CREATE INDEX IF NOT EXISTS idx_sales_created_at ON sales (created_at);
CREATE INDEX IF NOT EXISTS idx_sales_status ON sales (status);
CREATE INDEX IF NOT EXISTS idx_sale_items_sale_id ON sale_items (sale_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_product_id ON sale_items (product_id);
CREATE INDEX IF NOT EXISTS idx_sale_items_batch_id ON sale_items (batch_id);

-- -----------------------------------------------------------------------------
-- Audit trail
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id uuid NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    action_type action_type_enum NOT NULL,
    entity_type varchar(60) NOT NULL,
    entity_id uuid NOT NULL,
    before_json jsonb,
    after_json jsonb,
    "timestamp" timestamptz NOT NULL DEFAULT now(),
    ip_address inet
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs (entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs ("timestamp");
CREATE INDEX IF NOT EXISTS idx_audit_logs_action_type ON audit_logs (action_type);

-- -----------------------------------------------------------------------------
-- Triggers for updated_at
-- -----------------------------------------------------------------------------
DROP TRIGGER IF EXISTS trg_roles_updated_at ON roles;
CREATE TRIGGER trg_roles_updated_at
BEFORE UPDATE ON roles
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_users_updated_at ON users;
CREATE TRIGGER trg_users_updated_at
BEFORE UPDATE ON users
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_products_updated_at ON products;
CREATE TRIGGER trg_products_updated_at
BEFORE UPDATE ON products
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_suppliers_updated_at ON suppliers;
CREATE TRIGGER trg_suppliers_updated_at
BEFORE UPDATE ON suppliers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_customers_updated_at ON customers;
CREATE TRIGGER trg_customers_updated_at
BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_prescriptions_updated_at ON prescriptions;
CREATE TRIGGER trg_prescriptions_updated_at
BEFORE UPDATE ON prescriptions
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_purchase_orders_updated_at ON purchase_orders;
CREATE TRIGGER trg_purchase_orders_updated_at
BEFORE UPDATE ON purchase_orders
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_purchase_order_items_updated_at ON purchase_order_items;
CREATE TRIGGER trg_purchase_order_items_updated_at
BEFORE UPDATE ON purchase_order_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_goods_receipts_updated_at ON goods_receipts;
CREATE TRIGGER trg_goods_receipts_updated_at
BEFORE UPDATE ON goods_receipts
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_batches_updated_at ON batches;
CREATE TRIGGER trg_batches_updated_at
BEFORE UPDATE ON batches
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sales_updated_at ON sales;
CREATE TRIGGER trg_sales_updated_at
BEFORE UPDATE ON sales
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

DROP TRIGGER IF EXISTS trg_sale_items_updated_at ON sale_items;
CREATE TRIGGER trg_sale_items_updated_at
BEFORE UPDATE ON sale_items
FOR EACH ROW EXECUTE FUNCTION set_updated_at();

COMMIT;

-- Notes:
-- - FEFO allocation (expiry_date, received_at, id) is enforced in application logic.
-- - Prescription validity checks are application responsibilities, though the schema
--   preserves the necessary data for validation and auditability.
-- - Every stock mutation should write stock_movements and audit_logs in the same transaction.
