CREATE TABLE IF NOT EXISTS dispatch_orders (
  order_id VARCHAR(64) PRIMARY KEY,
  latitude DOUBLE PRECISION NOT NULL,
  longitude DOUBLE PRECISION NOT NULL,
  address VARCHAR(512) NOT NULL,
  package_weight NUMERIC(15,3) NOT NULL,
  priority VARCHAR(6) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE IF NOT EXISTS dispatch_vehicles (
  vehicle_id VARCHAR(64) PRIMARY KEY,
  capacity NUMERIC(15,3) NOT NULL,
  current_latitude DOUBLE PRECISION NOT NULL,
  current_longitude DOUBLE PRECISION NOT NULL,
  current_address VARCHAR(512) NOT NULL,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_dispatch_orders_priority ON dispatch_orders (priority);
