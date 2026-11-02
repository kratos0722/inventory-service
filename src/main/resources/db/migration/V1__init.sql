create table products (
    id                 bigint generated always as identity primary key,
    sku                varchar(64)    not null unique,
    name               varchar(255)   not null,
    description        varchar(1024),
    unit_price         numeric(12, 2) not null,
    quantity_available integer        not null default 0 check (quantity_available >= 0),
    quantity_reserved  integer        not null default 0 check (quantity_reserved >= 0),
    version            bigint         not null default 0,
    created_at         timestamp      not null default now(),
    updated_at         timestamp      not null default now()
);

create table reservations (
    id              bigint generated always as identity primary key,
    idempotency_key varchar(128) not null unique,
    product_id      bigint       not null references products (id),
    sku             varchar(64)  not null,
    quantity        integer      not null check (quantity > 0),
    status          varchar(32)  not null,
    created_at      timestamp    not null default now()
);

create index idx_reservations_product_id on reservations (product_id);
