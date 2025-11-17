#!/bin/bash
set -e

# 14logis MSA Database Initialization Script
# Services: user, hub, company, product, order, delivery, notification

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "postgres" <<-EOSQL
    CREATE DATABASE ${USER_DB:-oneforlogis_user};
    CREATE DATABASE ${HUB_DB:-oneforlogis_hub};
    CREATE DATABASE ${COMPANY_DB:-oneforlogis_company};
    CREATE DATABASE ${PRODUCT_DB:-oneforlogis_product};
    CREATE DATABASE ${ORDER_DB:-oneforlogis_order};
    CREATE DATABASE ${DELIVERY_DB:-oneforlogis_delivery};
    CREATE DATABASE ${NOTIFICATION_DB:-oneforlogis_notification};
EOSQL

echo "All 7 microservice databases created successfully"