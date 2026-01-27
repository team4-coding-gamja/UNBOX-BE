#!/bin/bash

# RDS endpoint and credentials
RDS_HOST="unbox-dev-common-db.crmw2cqokxc4.ap-northeast-2.rds.amazonaws.com"
RDS_PORT="5432"
ADMIN_USER="unbox_admin"
ADMIN_PASSWORD="l[:9+q01Roc4cqM?"

# Service passwords from SSM
PRODUCT_PASSWORD="v_%hHva&jt=_:aM$"
TRADE_PASSWORD="HQWMgXIS{BRI-T6="
ORDER_PASSWORD="v[qyB{ahI!i0PS_G"
PAYMENT_PASSWORD="BPandb4Stq(z>Q07"

echo "Creating databases and users..."

# Create databases
PGPASSWORD="$ADMIN_PASSWORD" psql -h "$RDS_HOST" -p "$RDS_PORT" -U "$ADMIN_USER" -d postgres <<EOF
-- Create databases
CREATE DATABASE unbox_product;
CREATE DATABASE unbox_trade;
CREATE DATABASE unbox_order;
CREATE DATABASE unbox_payment;

-- Create users
CREATE USER unbox_product WITH PASSWORD '$PRODUCT_PASSWORD';
CREATE USER unbox_trade WITH PASSWORD '$TRADE_PASSWORD';
CREATE USER unbox_order WITH PASSWORD '$ORDER_PASSWORD';
CREATE USER unbox_payment WITH PASSWORD '$PAYMENT_PASSWORD';

-- Grant database privileges
GRANT ALL PRIVILEGES ON DATABASE unbox_product TO unbox_product;
GRANT ALL PRIVILEGES ON DATABASE unbox_trade TO unbox_trade;
GRANT ALL PRIVILEGES ON DATABASE unbox_order TO unbox_order;
GRANT ALL PRIVILEGES ON DATABASE unbox_payment TO unbox_payment;
EOF

echo "Granting schema privileges for each database..."

# Grant schema privileges for product
PGPASSWORD="$ADMIN_PASSWORD" psql -h "$RDS_HOST" -p "$RDS_PORT" -U "$ADMIN_USER" -d unbox_product <<EOF
GRANT ALL ON SCHEMA public TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_product;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_product;
ALTER SCHEMA public OWNER TO unbox_product;
EOF

# Grant schema privileges for trade
PGPASSWORD="$ADMIN_PASSWORD" psql -h "$RDS_HOST" -p "$RDS_PORT" -U "$ADMIN_USER" -d unbox_trade <<EOF
GRANT ALL ON SCHEMA public TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_trade;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_trade;
ALTER SCHEMA public OWNER TO unbox_trade;
EOF

# Grant schema privileges for order
PGPASSWORD="$ADMIN_PASSWORD" psql -h "$RDS_HOST" -p "$RDS_PORT" -U "$ADMIN_USER" -d unbox_order <<EOF
GRANT ALL ON SCHEMA public TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_order;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_order;
ALTER SCHEMA public OWNER TO unbox_order;
EOF

# Grant schema privileges for payment
PGPASSWORD="$ADMIN_PASSWORD" psql -h "$RDS_HOST" -p "$RDS_PORT" -U "$ADMIN_USER" -d unbox_payment <<EOF
GRANT ALL ON SCHEMA public TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON TABLES TO unbox_payment;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT ALL ON SEQUENCES TO unbox_payment;
ALTER SCHEMA public OWNER TO unbox_payment;
EOF

echo "Done! All databases and users created successfully."
