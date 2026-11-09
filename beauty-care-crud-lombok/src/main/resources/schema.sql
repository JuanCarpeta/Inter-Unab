-- Tabla de items para intercambio. IF NOT EXISTS la hace segura de ejecutar en cada arranque.
CREATE TABLE IF NOT EXISTS item (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    descripcion TEXT,
    categoria TEXT NOT NULL,
    estado TEXT NOT NULL,
    fecha_publicacion TIMESTAMP NOT NULL,
    publicado_por TEXT
);

-- Tabla de fotos de items (una item puede tener varias fotos)
CREATE TABLE IF NOT EXISTS item_foto (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_id INTEGER NOT NULL,
    nombre_archivo TEXT NOT NULL,
    ruta_archivo TEXT NOT NULL,
    es_principal BOOLEAN NOT NULL DEFAULT 0,
    fecha_subida TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (item_id) REFERENCES item(id) ON DELETE CASCADE
);

-- Cuentas de estudiantes UNAB (solo correos @unab.edu.co). Contrasena en BCrypt.
CREATE TABLE IF NOT EXISTS usuario (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email TEXT NOT NULL UNIQUE,
    nombre TEXT,
    password_hash TEXT NOT NULL,
    fecha_registro TIMESTAMP NOT NULL
);

-- Propuestas de intercambio (trueque) entre dos items. Sin dinero de por medio.
CREATE TABLE IF NOT EXISTS intercambio (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    item_ofrecido_id INTEGER NOT NULL,
    item_solicitado_id INTEGER NOT NULL,
    mensaje TEXT,
    estado TEXT NOT NULL DEFAULT 'PENDIENTE',
    fecha_propuesta TIMESTAMP NOT NULL,
    fecha_respuesta TIMESTAMP,
    FOREIGN KEY (item_ofrecido_id) REFERENCES item(id),
    FOREIGN KEY (item_solicitado_id) REFERENCES item(id),
    CHECK (item_ofrecido_id <> item_solicitado_id)
);

-- Mensajes del chat de cada propuesta de intercambio
CREATE TABLE IF NOT EXISTS intercambio_mensaje (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    intercambio_id INTEGER NOT NULL,
    autor TEXT NOT NULL,
    texto TEXT NOT NULL,
    fecha TIMESTAMP NOT NULL,
    FOREIGN KEY (intercambio_id) REFERENCES intercambio(id) ON DELETE CASCADE
);
