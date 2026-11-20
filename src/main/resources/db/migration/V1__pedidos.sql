CREATE TABLE pedido (id VARCHAR(36) PRIMARY KEY, estado VARCHAR(40) NOT NULL, cliente VARCHAR(320), total NUMERIC(19,2) NOT NULL);
CREATE TABLE pedido_item (pedido_id VARCHAR(36) NOT NULL REFERENCES pedido(id), posicion INTEGER NOT NULL, producto BIGINT NOT NULL, cantidad INTEGER NOT NULL, PRIMARY KEY(pedido_id, posicion));
