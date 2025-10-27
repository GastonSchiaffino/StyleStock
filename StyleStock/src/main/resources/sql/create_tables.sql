-- ============================================
-- Schema mejorado para StyleStock
-- ============================================

-- Tabla de productos con auditoría
CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    descripcion TEXT NOT NULL,
    precio REAL NOT NULL CHECK(precio >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK(stock >= 0),
    stock_minimo INTEGER DEFAULT 5,
    categoria TEXT,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Tabla de clientes con validaciones
CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    direccion TEXT,
    telefono TEXT,
    cuit TEXT,
    email TEXT,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Tabla de facturas
CREATE TABLE IF NOT EXISTS facturas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_factura TEXT UNIQUE,
    cliente_id INTEGER NOT NULL,
    fecha TEXT NOT NULL DEFAULT (date('now', 'localtime')),
    subtotal REAL NOT NULL DEFAULT 0,
    descuento REAL DEFAULT 0,
    total REAL NOT NULL DEFAULT 0,
    tipo TEXT DEFAULT 'A' CHECK(tipo IN ('A', 'B', 'C')),
    estado TEXT DEFAULT 'EMITIDA' CHECK(estado IN ('EMITIDA', 'ANULADA', 'PAGADA')),
    notas TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT
);

-- Tabla de detalle de factura
CREATE TABLE IF NOT EXISTS detalle_factura (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    factura_id INTEGER NOT NULL,
    producto_id INTEGER NOT NULL,
    cantidad INTEGER NOT NULL CHECK(cantidad > 0),
    precio_unitario REAL NOT NULL CHECK(precio_unitario >= 0),
    descuento REAL DEFAULT 0 CHECK(descuento >= 0),
    subtotal REAL NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(factura_id) REFERENCES facturas(id) ON DELETE CASCADE,
    FOREIGN KEY(producto_id) REFERENCES productos(id) ON DELETE RESTRICT
);

-- Tabla de movimientos de stock (auditoría)
CREATE TABLE IF NOT EXISTS movimientos_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id INTEGER NOT NULL,
    tipo TEXT NOT NULL CHECK(tipo IN ('INGRESO', 'EGRESO', 'AJUSTE', 'VENTA')),
    cantidad INTEGER NOT NULL,
    stock_anterior INTEGER NOT NULL,
    stock_nuevo INTEGER NOT NULL,
    referencia TEXT, -- ID de factura o motivo
    observaciones TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(producto_id) REFERENCES productos(id) ON DELETE RESTRICT
);

-- Tabla de configuración
CREATE TABLE IF NOT EXISTS configuracion (
    clave TEXT PRIMARY KEY,
    valor TEXT NOT NULL,
    descripcion TEXT,
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- ÍNDICES para optimizar consultas
-- ============================================

CREATE INDEX IF NOT EXISTS idx_productos_codigo ON productos(codigo);
CREATE INDEX IF NOT EXISTS idx_productos_activo ON productos(activo);
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoria);
CREATE INDEX IF NOT EXISTS idx_productos_stock ON productos(stock);

CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre);
CREATE INDEX IF NOT EXISTS idx_clientes_cuit ON clientes(cuit);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);

CREATE INDEX IF NOT EXISTS idx_facturas_fecha ON facturas(fecha);
CREATE INDEX IF NOT EXISTS idx_facturas_cliente ON facturas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_facturas_estado ON facturas(estado);
CREATE INDEX IF NOT EXISTS idx_facturas_numero ON facturas(numero_factura);

CREATE INDEX IF NOT EXISTS idx_detalle_factura ON detalle_factura(factura_id);
CREATE INDEX IF NOT EXISTS idx_detalle_producto ON detalle_factura(producto_id);

CREATE INDEX IF NOT EXISTS idx_movimientos_producto ON movimientos_stock(producto_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_stock(created_at);
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo ON movimientos_stock(tipo);

-- ============================================
-- TRIGGERS para auditoría automática
-- ============================================

-- Trigger para actualizar updated_at en productos
CREATE TRIGGER IF NOT EXISTS trg_productos_updated_at
AFTER UPDATE ON productos
FOR EACH ROW
BEGIN
    UPDATE productos SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- Trigger para actualizar updated_at en clientes
CREATE TRIGGER IF NOT EXISTS trg_clientes_updated_at
AFTER UPDATE ON clientes
FOR EACH ROW
BEGIN
    UPDATE clientes SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- Trigger para actualizar updated_at en facturas
CREATE TRIGGER IF NOT EXISTS trg_facturas_updated_at
AFTER UPDATE ON facturas
FOR EACH ROW
BEGIN
    UPDATE facturas SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- Trigger para generar número de factura automáticamente
CREATE TRIGGER IF NOT EXISTS trg_generar_numero_factura
AFTER INSERT ON facturas
FOR EACH ROW
WHEN NEW.numero_factura IS NULL
BEGIN
    UPDATE facturas 
    SET numero_factura = printf('%s-%08d', NEW.tipo, NEW.id)
    WHERE id = NEW.id;
END;

-- ============================================
-- VISTAS útiles para reportes
-- ============================================

-- Vista de productos con stock bajo
CREATE VIEW IF NOT EXISTS v_productos_stock_bajo AS
SELECT 
    id, codigo, descripcion, precio, stock, stock_minimo,
    (stock_minimo - stock) as faltante
FROM productos
WHERE stock < stock_minimo AND activo = 1;

-- Vista de facturas con detalles
CREATE VIEW IF NOT EXISTS v_facturas_completas AS
SELECT 
    f.id, f.numero_factura, f.fecha, f.total, f.estado,
    c.nombre as cliente_nombre, c.cuit as cliente_cuit,
    COUNT(df.id) as cantidad_items
FROM facturas f
INNER JOIN clientes c ON f.cliente_id = c.id
LEFT JOIN detalle_factura df ON f.id = df.factura_id
GROUP BY f.id;

-- Vista de movimientos de stock con detalles
CREATE VIEW IF NOT EXISTS v_movimientos_completos AS
SELECT 
    m.id, m.tipo, m.cantidad, m.stock_anterior, m.stock_nuevo,
    m.referencia, m.observaciones, m.created_at,
    p.codigo, p.descripcion as producto_descripcion
FROM movimientos_stock m
INNER JOIN productos p ON m.producto_id = p.id;

-- ============================================
-- DATOS INICIALES de configuración
-- ============================================

INSERT OR IGNORE INTO configuracion (clave, valor, descripcion) VALUES
('empresa_nombre', 'Style Stock', 'Nombre de la empresa'),
('empresa_direccion', '', 'Dirección de la empresa'),
('empresa_telefono', '', 'Teléfono de la empresa'),
('empresa_cuit', '', 'CUIT de la empresa'),
('factura_pie', 'Gracias por su compra', 'Texto al pie de la factura'),
('stock_alerta', '5', 'Nivel de stock para alertar'),
('backup_auto', '1', 'Activar backup automático (1=Sí, 0=No)'),
('backup_dias', '7', 'Días entre backups automáticos'),
('ultima_version', '1.0.0', 'Versión del esquema de BD');