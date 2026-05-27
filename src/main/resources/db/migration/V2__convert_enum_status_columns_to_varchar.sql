-- Convert PostgreSQL enum-backed status columns to varchar so the application's
-- string converters can persist and read them consistently in dev/runtime.

BEGIN;

ALTER TABLE pms.prescriptions
    ALTER COLUMN verification_status TYPE varchar(20)
    USING verification_status::text;

ALTER TABLE pms.purchase_orders
    ALTER COLUMN status TYPE varchar(30)
    USING status::text;

ALTER TABLE pms.batches
    ALTER COLUMN status TYPE varchar(30)
    USING status::text;

ALTER TABLE pms.stock_movements
    ALTER COLUMN type TYPE varchar(30)
    USING type::text;

ALTER TABLE pms.sales
    ALTER COLUMN status TYPE varchar(30)
    USING status::text;

ALTER TABLE pms.sales
    ALTER COLUMN payment_status TYPE varchar(30)
    USING payment_status::text;

ALTER TABLE pms.audit_logs
    ALTER COLUMN action_type TYPE varchar(40)
    USING action_type::text;

COMMIT;
