CREATE TABLE clave_idempotencia (id VARCHAR(36) PRIMARY KEY, usuario VARCHAR(320) NOT NULL, clave VARCHAR(100) NOT NULL, huella VARCHAR(200) NOT NULL, pedido_id VARCHAR(36) REFERENCES pedido(id), creada TIMESTAMP NOT NULL, CONSTRAINT uk_usuario_clave UNIQUE(usuario, clave));
CREATE TABLE mensaje_procesado (id VARCHAR(200) PRIMARY KEY, creado TIMESTAMP NOT NULL);
CREATE TABLE consumo_evento (id VARCHAR(200) PRIMARY KEY, suscriptor VARCHAR(30) NOT NULL, payload TEXT NOT NULL, total NUMERIC(19,2), creado TIMESTAMP NOT NULL);
