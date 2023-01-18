-- SEQUENCE: public.nft_id_seq
-- DROP SEQUENCE IF EXISTS public.nft_id_seq;
CREATE SEQUENCE IF NOT EXISTS public.nft_id_seq
    INCREMENT 1
    START 1
    MINVALUE 1
    MAXVALUE 9223372036854775807
    CACHE 1;
ALTER SEQUENCE public.nft_id_seq OWNER TO playnomm;

-- Table: public.nft
-- DROP TABLE IF EXISTS public.nft;
CREATE TABLE IF NOT EXISTS public.nft
(
    id bigint NOT NULL DEFAULT nextval('nft_id_seq'::regclass),
    token_id bigint,
    tx_hash character varying(64) COLLATE pg_catalog."default" NOT NULL,
    rarity character varying(32) COLLATE pg_catalog."default",
    owner character varying(40) COLLATE pg_catalog."default",
    action character varying(32) COLLATE pg_catalog."default",
    "from" character varying(40) COLLATE pg_catalog."default",
    "to" character varying(40) COLLATE pg_catalog."default",
    event_time bigint,
    created_at bigint,
    CONSTRAINT nft_pkey PRIMARY KEY (tx_hash)
)
TABLESPACE pg_default;
ALTER TABLE IF EXISTS public.nft OWNER to playnomm;
ALTER SEQUENCE IF EXISTS public.nft_id_seq OWNED BY nft.id
