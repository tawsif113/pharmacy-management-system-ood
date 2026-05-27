BEGIN;

ALTER TABLE pms.stock_movements
    ADD COLUMN IF NOT EXISTS delta_quantity integer;

UPDATE pms.stock_movements
SET delta_quantity = CASE
    WHEN type IN ('receipt', 'return', 'adjustment') THEN quantity
    WHEN type IN ('sale', 'write_off', 'cancel') THEN -quantity
    ELSE quantity
END
WHERE delta_quantity IS NULL;

ALTER TABLE pms.stock_movements
    ALTER COLUMN delta_quantity SET NOT NULL;

ALTER TABLE pms.stock_movements
    DROP CONSTRAINT IF EXISTS stock_movements_delta_quantity_nonzero;

ALTER TABLE pms.stock_movements
    ADD CONSTRAINT stock_movements_delta_quantity_nonzero CHECK (delta_quantity <> 0);

COMMIT;
