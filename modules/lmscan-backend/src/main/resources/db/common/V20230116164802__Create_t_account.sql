-- Table: public.account

-- DROP TABLE IF EXISTS public.account;

CREATE TABLE IF NOT EXISTS public.account
(
    id bigint NOT NULL,
    address character varying COLLATE pg_catalog."default",
    balance numeric(28,18),
    amount numeric(11,2),
    type character varying COLLATE pg_catalog."default",
    created_at bigint
)
TABLESPACE pg_default;
ALTER TABLE IF EXISTS public.account OWNER to playnomm;
