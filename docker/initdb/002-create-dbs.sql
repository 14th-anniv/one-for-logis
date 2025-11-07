CREATE DATABASE oneforlogis_user;
CREATE DATABASE oneforlogis_hub;
CREATE DATABASE oneforlogis_order;
CREATE DATABASE oneforlogis_company;
CREATE DATABASE product_db;
CREATE DATABASE notification_db;

GRANT ALL PRIVILEGES ON DATABASE oneforlogis_user  TO admin;
GRANT ALL PRIVILEGES ON DATABASE oneforlogis_hub  TO admin;
GRANT ALL PRIVILEGES ON DATABASE oneforlogis_order TO admin;
GRANT ALL PRIVILEGES ON DATABASE oneforlogis_company TO admin;
GRANT ALL PRIVILEGES ON DATABASE product_db TO admin;
GRANT ALL PRIVILEGES ON DATABASE notification_db TO admin;