-- Table: public.block

-- DROP TABLE IF EXISTS public.block;

CREATE TABLE IF NOT EXISTS public.block
(
    id bigint NOT NULL,
    "number" bigint NOT NULL,
    hash character varying COLLATE pg_catalog."default" NOT NULL,
    parent_hash character varying COLLATE pg_catalog."default",
    tx_count bigint NOT NULL,
    event_time bigint NOT NULL,
    created_at bigint NOT NULL,
    CONSTRAINT block_pkey PRIMARY KEY (id)
)
TABLESPACE pg_default;
ALTER TABLE IF EXISTS public.block OWNER to playnomm;
COMMENT ON COLUMN public.block.event_time IS '블록 발생시간';
