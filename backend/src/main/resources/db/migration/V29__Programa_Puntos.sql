ALTER TABLE pedidos ADD COLUMN puntos_canjeados integer NOT NULL DEFAULT 0;
ALTER TABLE pedidos ADD COLUMN descuento_puntos_ars numeric(19,2) NOT NULL DEFAULT 0;

CREATE TABLE puntos_creditos (
    id bigserial PRIMARY KEY,
    usuario_id bigint NOT NULL REFERENCES usuarios(id),
    pedido_id bigint NOT NULL UNIQUE REFERENCES pedidos(id),
    puntos_otorgados integer NOT NULL CHECK (puntos_otorgados > 0),
    puntos_disponibles integer NOT NULL CHECK (puntos_disponibles >= 0),
    acreditado_en timestamp NOT NULL,
    vence_en timestamp NOT NULL,
    estado varchar(20) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);
CREATE INDEX idx_puntos_creditos_usuario_vigencia ON puntos_creditos(usuario_id, estado, vence_en);

CREATE TABLE canjes_puntos (
    id bigserial PRIMARY KEY,
    usuario_id bigint NOT NULL REFERENCES usuarios(id),
    pedido_id bigint NOT NULL UNIQUE REFERENCES pedidos(id),
    puntos integer NOT NULL CHECK (puntos > 0),
    estado varchar(20) NOT NULL,
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);
CREATE INDEX idx_canjes_puntos_usuario ON canjes_puntos(usuario_id, created_at DESC);

CREATE TABLE canjes_puntos_items (
    id bigserial PRIMARY KEY,
    canje_id bigint NOT NULL REFERENCES canjes_puntos(id),
    credito_id bigint NOT NULL REFERENCES puntos_creditos(id),
    puntos integer NOT NULL CHECK (puntos > 0),
    created_at timestamp NOT NULL,
    updated_at timestamp NOT NULL
);
