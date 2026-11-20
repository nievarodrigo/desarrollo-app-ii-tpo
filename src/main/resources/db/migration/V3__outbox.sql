CREATE TABLE evento_outbox (id VARCHAR(36) PRIMARY KEY, tipo VARCHAR(50) NOT NULL, destino VARCHAR(20) NOT NULL, payload TEXT NOT NULL, creado TIMESTAMP NOT NULL, publicado TIMESTAMP, intentos INTEGER NOT NULL);
CREATE INDEX ix_outbox_pendiente ON evento_outbox(publicado, creado);
