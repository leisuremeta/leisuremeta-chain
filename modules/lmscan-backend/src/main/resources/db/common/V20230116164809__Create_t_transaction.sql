-- Table: public.transaction

-- DROP TABLE IF EXISTS public.transaction;

CREATE TABLE IF NOT EXISTS public.transaction
(
    id bigint NOT NULL GENERATED ALWAYS AS IDENTITY ( INCREMENT 1 START 1 MINVALUE 1 MAXVALUE 9223372036854775807 CACHE 1 ),
    hash character varying(64) COLLATE pg_catalog."default" NOT NULL,
    type character varying(32) COLLATE pg_catalog."default" NOT NULL,
    "from" character varying(64) COLLATE pg_catalog."default" NOT NULL,
    "to" character varying[] COLLATE pg_catalog."default" NOT NULL,
    value numeric(28,18) NOT NULL,
    block_hash character varying(64) COLLATE pg_catalog."default" NOT NULL,
    event_time bigint NOT NULL,
    created_at bigint NOT NULL,
    CONSTRAINT transaction_pkey PRIMARY KEY (id)
)
TABLESPACE pg_default;
ALTER TABLE IF EXISTS public.transaction OWNER to playnomm;
