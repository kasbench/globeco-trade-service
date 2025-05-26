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
	order_id integer NOT NULL,
	portfolio_id char(24) NOT NULL,
	order_type char(10) NOT NULL,
	security_id char(24) NOT NULL,
	quantity decimal(18,8) NOT NULL,
	limit_price decimal(18,8),
	quantity_sent decimal(18,8) NOT NULL DEFAULT 0,
	trade_timestamp timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
	blotter_id integer,
	submitted boolean NOT NULL DEFAULT false,
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
	execution_timestamp timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
	execution_status_id integer NOT NULL,
	blotter_id integer,
	trade_type_id integer,
	trade_order_id integer NOT NULL,
	destination_id integer NOT NULL,
	quantity_ordered decimal(18,8),
	quantity_placed decimal(18,8) NOT NULL,
	quantity_filled decimal(18,8) NOT NULL DEFAULT 0,
	limit_price decimal(18,8),
	execution_service_id integer,
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
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: trade_type__fk | type: CONSTRAINT --
-- ALTER TABLE public.execution DROP CONSTRAINT IF EXISTS trade_type__fk CASCADE;
ALTER TABLE public.execution ADD CONSTRAINT trade_type__fk FOREIGN KEY (trade_type_id)
REFERENCES public.trade_type (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;
-- ddl-end --

-- object: trade_order_order_id_ndx | type: INDEX --
-- DROP INDEX IF EXISTS public.trade_order_order_id_ndx CASCADE;
CREATE UNIQUE INDEX trade_order_order_id_ndx ON public.trade_order
USING btree
(
	order_id
);
-- ddl-end --

-- object: blotter__fk | type: CONSTRAINT --
-- ALTER TABLE public.trade_order DROP CONSTRAINT IF EXISTS blotter__fk CASCADE;
ALTER TABLE public.trade_order ADD CONSTRAINT blotter__fk FOREIGN KEY (blotter_id)
REFERENCES public.blotter (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;
-- ddl-end --

-- object: trade_order__fk | type: CONSTRAINT --
-- ALTER TABLE public.execution DROP CONSTRAINT IF EXISTS trade_order__fk CASCADE;
ALTER TABLE public.execution ADD CONSTRAINT trade_order__fk FOREIGN KEY (trade_order_id)
REFERENCES public.trade_order (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: public.destination | type: TABLE --
-- DROP TABLE IF EXISTS public.destination CASCADE;
CREATE TABLE public.destination (
	id serial NOT NULL,
	abbreviation varchar(20) NOT NULL,
	description varchar(60) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT destination_pk PRIMARY KEY (id)
);
-- ddl-end --
ALTER TABLE public.destination OWNER TO postgres;
-- ddl-end --

-- object: destination__fk | type: CONSTRAINT --
-- ALTER TABLE public.execution DROP CONSTRAINT IF EXISTS destination__fk CASCADE;
ALTER TABLE public.execution ADD CONSTRAINT destination__fk FOREIGN KEY (destination_id)
REFERENCES public.destination (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;
-- ddl-end --

-- object: blotter__fk | type: CONSTRAINT --
-- ALTER TABLE public.execution DROP CONSTRAINT IF EXISTS blotter__fk CASCADE;
ALTER TABLE public.execution ADD CONSTRAINT blotter__fk FOREIGN KEY (blotter_id)
REFERENCES public.blotter (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;
-- ddl-end --

-- object: execution_service_id_ndx | type: INDEX --
-- DROP INDEX IF EXISTS public.execution_service_id_ndx CASCADE;
CREATE UNIQUE INDEX execution_service_id_ndx ON public.execution
USING btree
(
	execution_service_id
);
-- ddl-end --


