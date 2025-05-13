-- ** Database generated with pgModeler (PostgreSQL Database Modeler).
-- ** pgModeler version: 1.2.0-beta1
-- ** PostgreSQL version: 17.0
-- ** Project Site: pgmodeler.io
-- ** Model Author: ---

-- ** Database creation must be performed outside a multi lined SQL file. 
-- ** These commands were put in this file only as a convenience.

-- object: new_database | type: DATABASE --
-- DROP DATABASE IF EXISTS new_database;
CREATE DATABASE new_database;
-- ddl-end --


SET search_path TO pg_catalog,public;
-- ddl-end --

-- object: public.trade_order | type: TABLE --
-- DROP TABLE IF EXISTS public.trade_order CASCADE;
CREATE TABLE public.trade_order (
	id serial NOT NULL,
	portfolio_id char(24) NOT NULL,
	quantity decimal(18,8) NOT NULL,
	trade_timestamp timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT trade_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.trade_order OWNER TO postgres;
-- ddl-end --

-- object: public.execution | type: TABLE --
-- DROP TABLE IF EXISTS public.execution CASCADE;
CREATE TABLE public.execution (
	id serial NOT NULL,
	trade_block_id integer NOT NULL,
	execution_status_id integer,
	trade_type_id integer,
	execution_timestamp timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
	quantity_placed decimal(18,8) NOT NULL,
	quantity_filled decimal(18,8) NOT NULL DEFAULT 0,
	limit_price decimal(18,8),
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT execution_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.execution OWNER TO postgres;
-- ddl-end --

-- object: public.blotter | type: TABLE --
-- DROP TABLE IF EXISTS public.blotter CASCADE;
CREATE TABLE public.blotter (
	id serial NOT NULL,
	abbreviation varchar(20) NOT NULL,
	name varchar(100) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT blotter_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.blotter OWNER TO postgres;
-- ddl-end --

-- object: public.trade_block | type: TABLE --
-- DROP TABLE IF EXISTS public.trade_block CASCADE;
CREATE TABLE public.trade_block (
	id serial NOT NULL,
	trade_status_id integer,
	blotter_id integer NOT NULL,
	order_type varchar(10) NOT NULL,
	trade_type_id integer NOT NULL,
	security_id char(24) NOT NULL,
	quantity_ordered decimal(18,8) NOT NULL,
	limit_price decimal(18,8) NOT NULL,
	quantity_placed decimal(18,8) NOT NULL DEFAULT 0,
	quantity_filled decimal(18,8) NOT NULL DEFAULT 0,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT trade_block_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.trade_block OWNER TO postgres;
-- ddl-end --

-- object: public.trade_block_allocation | type: TABLE --
-- DROP TABLE IF EXISTS public.trade_block_allocation CASCADE;
CREATE TABLE public.trade_block_allocation (
	id serial NOT NULL,
	version integer NOT NULL DEFAULT 1,
	trade_order_id integer NOT NULL,
	trade_block_id integer NOT NULL,
	CONSTRAINT trade_block_allocation_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.trade_block_allocation OWNER TO postgres;
-- ddl-end --

-- object: trade_order__fk | type: CONSTRAINT --
-- ALTER TABLE public.trade_block_allocation DROP CONSTRAINT IF EXISTS trade_order__fk CASCADE;
ALTER TABLE public.trade_block_allocation ADD CONSTRAINT trade_order__fk FOREIGN KEY (trade_order_id)
REFERENCES public.trade_order (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: trade_block__fk | type: CONSTRAINT --
-- ALTER TABLE public.trade_block_allocation DROP CONSTRAINT IF EXISTS trade_block__fk CASCADE;
ALTER TABLE public.trade_block_allocation ADD CONSTRAINT trade_block__fk FOREIGN KEY (trade_block_id)
REFERENCES public.trade_block (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: blotter__fk | type: CONSTRAINT --
-- ALTER TABLE public.trade_block DROP CONSTRAINT IF EXISTS blotter__fk CASCADE;
ALTER TABLE public.trade_block ADD CONSTRAINT blotter__fk FOREIGN KEY (blotter_id)
REFERENCES public.blotter (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: trade_block__fk | type: CONSTRAINT --
-- ALTER TABLE public.execution DROP CONSTRAINT IF EXISTS trade_block__fk CASCADE;
ALTER TABLE public.execution ADD CONSTRAINT trade_block__fk FOREIGN KEY (trade_block_id)
REFERENCES public.trade_block (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: public.trade_type | type: TABLE --
-- DROP TABLE IF EXISTS public.trade_type CASCADE;
CREATE TABLE public.trade_type (
	id serial NOT NULL,
	abbreviation varchar(10) NOT NULL,
	description varchar(60) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT trade_type_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.trade_type OWNER TO postgres;
-- ddl-end --

-- object: trade_type__fk | type: CONSTRAINT --
-- ALTER TABLE public.trade_block DROP CONSTRAINT IF EXISTS trade_type__fk CASCADE;
ALTER TABLE public.trade_block ADD CONSTRAINT trade_type__fk FOREIGN KEY (trade_type_id)
REFERENCES public.trade_type (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: public.trade_status | type: TABLE --
-- DROP TABLE IF EXISTS public.trade_status CASCADE;
CREATE TABLE public.trade_status (
	id serial NOT NULL,
	abbreviation varchar(20) NOT NULL,
	description varchar(60) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT trade_status_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.trade_status OWNER TO postgres;
-- ddl-end --

-- object: trade_status__fk | type: CONSTRAINT --
-- ALTER TABLE public.trade_block DROP CONSTRAINT IF EXISTS trade_status__fk CASCADE;
ALTER TABLE public.trade_block ADD CONSTRAINT trade_status__fk FOREIGN KEY (trade_status_id)
REFERENCES public.trade_status (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;
-- ddl-end --

-- object: public.execution_status | type: TABLE --
-- DROP TABLE IF EXISTS public.execution_status CASCADE;
CREATE TABLE public.execution_status (
	id serial NOT NULL,
	abbreviation varchar(20) NOT NULL,
	description varchar(60) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT execution_status_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.execution_status OWNER TO postgres;
-- ddl-end --

-- object: execution_status__fk | type: CONSTRAINT --
-- ALTER TABLE public.execution DROP CONSTRAINT IF EXISTS execution_status__fk CASCADE;
ALTER TABLE public.execution ADD CONSTRAINT execution_status__fk FOREIGN KEY (execution_status_id)
REFERENCES public.execution_status (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;
-- ddl-end --

-- object: trade_type__fk | type: CONSTRAINT --
-- ALTER TABLE public.execution DROP CONSTRAINT IF EXISTS trade_type__fk CASCADE;
ALTER TABLE public.execution ADD CONSTRAINT trade_type__fk FOREIGN KEY (trade_type_id)
REFERENCES public.trade_type (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;
-- ddl-end --


