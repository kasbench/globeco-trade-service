-- Flyway migration: Initial schema for GlobeCo Trade Service
-- Generated from trade-service.sql

SET search_path TO pg_catalog,public;

-- object: public.trade_order | type: TABLE --
CREATE TABLE public.trade_order (
	id serial NOT NULL,
	order_id integer NOT NULL,
	portfolio_id char(24) NOT NULL,
	order_type char(10) NOT NULL,
	security_id char(24) NOT NULL,
	quantity decimal(18,8) NOT NULL,
	limit_price decimal(18,8),
	trade_timestamp timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
	version integer NOT NULL DEFAULT 1,
	blotter_id integer,
	CONSTRAINT trade_pk PRIMARY KEY (id)
);

-- object: public.execution | type: TABLE --
CREATE TABLE public.execution (
	id serial NOT NULL,
	execution_timestamp timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
	execution_status_id integer NOT NULL,
	blotter_id integer,
	trade_type_id integer,
	trade_order_id integer NOT NULL,
	destination_id integer NOT NULL,
	quantity_ordered smallint,
	quantity_placed decimal(18,8) NOT NULL,
	quantity_filled decimal(18,8) NOT NULL DEFAULT 0,
	limit_price decimal(18,8),
	version integer NOT NULL DEFAULT 1,
	execution_service_id integer NULL,
	CONSTRAINT execution_pk PRIMARY KEY (id)
);

-- object: public.blotter | type: TABLE --
CREATE TABLE public.blotter (
	id serial NOT NULL,
	abbreviation varchar(20) NOT NULL,
	name varchar(100) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT blotter_pk PRIMARY KEY (id)
);

-- object: public.trade_type | type: TABLE --
CREATE TABLE public.trade_type (
	id serial NOT NULL,
	abbreviation varchar(10) NOT NULL,
	description varchar(60) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT trade_type_pk PRIMARY KEY (id)
);

-- object: public.execution_status | type: TABLE --
CREATE TABLE public.execution_status (
	id serial NOT NULL,
	abbreviation varchar(20) NOT NULL,
	description varchar(60) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT execution_status_pk PRIMARY KEY (id)
);

-- object: execution_status__fk | type: CONSTRAINT --
ALTER TABLE public.execution ADD CONSTRAINT execution_status__fk FOREIGN KEY (execution_status_id)
REFERENCES public.execution_status (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;

-- object: trade_type__fk | type: CONSTRAINT --
ALTER TABLE public.execution ADD CONSTRAINT trade_type__fk FOREIGN KEY (trade_type_id)
REFERENCES public.trade_type (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;

-- object: trade_order_order_id_ndx | type: INDEX --
CREATE UNIQUE INDEX trade_order_order_id_ndx ON public.trade_order
USING btree
(
	order_id
);

-- object: blotter__fk | type: CONSTRAINT --
ALTER TABLE public.trade_order ADD CONSTRAINT blotter__fk FOREIGN KEY (blotter_id)
REFERENCES public.blotter (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;

-- object: trade_order__fk | type: CONSTRAINT --
ALTER TABLE public.execution ADD CONSTRAINT trade_order__fk FOREIGN KEY (trade_order_id)
REFERENCES public.trade_order (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;

-- object: public.destination | type: TABLE --
CREATE TABLE public.destination (
	id serial NOT NULL,
	abbreviation varchar(20) NOT NULL,
	description varchar(60) NOT NULL,
	version integer NOT NULL DEFAULT 1,
	CONSTRAINT destination_pk PRIMARY KEY (id)
);

-- object: destination__fk | type: CONSTRAINT --
ALTER TABLE public.execution ADD CONSTRAINT destination__fk FOREIGN KEY (destination_id)
REFERENCES public.destination (id) MATCH FULL
ON DELETE RESTRICT ON UPDATE CASCADE;

-- object: blotter__fk | type: CONSTRAINT --
ALTER TABLE public.execution ADD CONSTRAINT blotter__fk FOREIGN KEY (blotter_id)
REFERENCES public.blotter (id) MATCH FULL
ON DELETE SET NULL ON UPDATE CASCADE;

CREATE INDEX execution_service_id_ndx ON public.execution (execution_service_id); 