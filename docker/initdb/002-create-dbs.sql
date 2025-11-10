-- DEPRECATED: This file is no longer used
-- Use scripts/init-databases.sql instead (referenced in docker-compose-team.yml)
--
-- 14logis MSA Database Initialization Script (Legacy)
-- Services: user, hub, company, product, order, delivery, notification

CREATE DATABASE oneforlogis_user;
CREATE DATABASE oneforlogis_hub;
CREATE DATABASE oneforlogis_company;
CREATE DATABASE oneforlogis_product;
CREATE DATABASE oneforlogis_order;
CREATE DATABASE oneforlogis_delivery;
CREATE DATABASE oneforlogis_notification;

-- Note: GRANT statements removed (use default PostgreSQL user permissions)