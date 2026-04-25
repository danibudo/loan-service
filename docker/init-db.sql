-- user_service database is created by the POSTGRES_DB environment variable.
-- This script creates the remaining databases and their users.

CREATE USER auth_user WITH PASSWORD 'changeme';
CREATE DATABASE auth_service OWNER auth_user;

CREATE DATABASE catalog_service;

CREATE DATABASE loan_service;