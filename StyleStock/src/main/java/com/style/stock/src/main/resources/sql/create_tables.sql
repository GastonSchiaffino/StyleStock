-- ============================================
-- StyleStock v2.0 - Schema CORREGIDO
-- ============================================

-- ============================================
-- TABLAS DE CONFIGURACIÓN
-- ============================================

CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    descripcion TEXT,
    requiere_variantes INTEGER DEFAULT 1,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS atributos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    tipo TEXT NOT NULL CHECK(tipo IN ('TEXTO', 'LISTA', 'NUMERO', 'COLOR')) DEFAULT 'LISTA',
    descripcion TEXT,
    orden INTEGER DEFAULT 0,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS valores_atributo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    atributo_id INTEGER NOT NULL,
    valor TEXT NOT NULL,
    codigo_hex TEXT,
    orden INTEGER DEFAULT 0,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(atributo_id) REFERENCES atributos(id) ON DELETE CASCADE,
    UNIQUE(atributo_id, valor)
);

CREATE TABLE IF NOT EXISTS categoria_atributos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    categoria_id INTEGER NOT NULL,
    atributo_id INTEGER NOT NULL,
    requerido INTEGER DEFAULT 1,
    orden INTEGER DEFAULT 0,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(categoria_id) REFERENCES categorias(id) ON DELETE CASCADE,
    FOREIGN KEY(atributo_id) REFERENCES atributos(id) ON DELETE CASCADE,
    UNIQUE(categoria_id, atributo_id)
);

-- ============================================
-- TABLAS DE PRODUCTOS
-- ============================================

CREATE TABLE IF NOT EXISTS productos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    codigo TEXT NOT NULL UNIQUE,
    nombre TEXT NOT NULL,
    descripcion TEXT,
    categoria_id INTEGER NOT NULL,
    marca TEXT,
    precio_costo REAL DEFAULT 0 CHECK(precio_costo >= 0),
    precio_minorista REAL NOT NULL CHECK(precio_minorista >= 0),
    precio_mayorista REAL NOT NULL CHECK(precio_mayorista >= 0),
    imagen_url TEXT,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(categoria_id) REFERENCES categorias(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS variantes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    producto_id INTEGER NOT NULL,
    sku TEXT NOT NULL UNIQUE,
    codigo_barras TEXT,
    precio_costo REAL DEFAULT 0 CHECK(precio_costo >= 0),
    precio_minorista REAL NOT NULL CHECK(precio_minorista >= 0),
    precio_mayorista REAL NOT NULL CHECK(precio_mayorista >= 0),
    stock INTEGER NOT NULL DEFAULT 0 CHECK(stock >= 0),
    stock_minimo INTEGER DEFAULT 5,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(producto_id) REFERENCES productos(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS variante_atributos (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    variante_id INTEGER NOT NULL,
    atributo_id INTEGER NOT NULL,
    valor TEXT NOT NULL,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(variante_id) REFERENCES variantes(id) ON DELETE CASCADE,
    FOREIGN KEY(atributo_id) REFERENCES atributos(id) ON DELETE RESTRICT,
    UNIQUE(variante_id, atributo_id)
);

-- ============================================
-- TABLAS DE CLIENTES
-- ============================================

CREATE TABLE IF NOT EXISTS tipos_cliente (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    usa_precio_mayorista INTEGER DEFAULT 0,
    descripcion TEXT,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    apellido TEXT,
    direccion TEXT,
    telefono TEXT,
    email TEXT,
    cuit TEXT,
    tipo_cliente_id INTEGER DEFAULT 1,
    notas TEXT,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(tipo_cliente_id) REFERENCES tipos_cliente(id) ON DELETE RESTRICT
);

-- ============================================
-- TABLAS DE VENTAS/FACTURACIÓN
-- ============================================

CREATE TABLE IF NOT EXISTS metodos_pago (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    requiere_cuotas INTEGER DEFAULT 0,
    comision_porcentaje REAL DEFAULT 0 CHECK(comision_porcentaje >= 0),
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

CREATE TABLE IF NOT EXISTS ventas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_comprobante TEXT UNIQUE,
    cliente_id INTEGER NOT NULL,
    tipo_cliente_id INTEGER NOT NULL,
    fecha TEXT NOT NULL DEFAULT (date('now', 'localtime')),
    hora TEXT NOT NULL DEFAULT (time('now', 'localtime')),
    subtotal REAL NOT NULL DEFAULT 0 CHECK(subtotal >= 0),
    descuento REAL DEFAULT 0 CHECK(descuento >= 0),
    total REAL NOT NULL DEFAULT 0 CHECK(total >= 0),
    tipo_comprobante TEXT DEFAULT 'TICKET' CHECK(tipo_comprobante IN ('TICKET', 'FACTURA_A', 'FACTURA_B', 'FACTURA_C')),
    tipo_venta TEXT DEFAULT 'MINORISTA' CHECK(tipo_venta IN ('MINORISTA', 'MAYORISTA')),
    estado TEXT DEFAULT 'COMPLETADA' CHECK(estado IN ('COMPLETADA', 'ANULADA', 'PENDIENTE')),
    notas TEXT,
    vendedor TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT,
    FOREIGN KEY(tipo_cliente_id) REFERENCES tipos_cliente(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS detalle_venta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id INTEGER NOT NULL,
    variante_id INTEGER NOT NULL,
    cantidad INTEGER NOT NULL CHECK(cantidad > 0),
    precio_unitario REAL NOT NULL CHECK(precio_unitario >= 0),
    precio_tipo TEXT NOT NULL CHECK(precio_tipo IN ('MINORISTA', 'MAYORISTA')),
    descuento REAL DEFAULT 0 CHECK(descuento >= 0),
    subtotal REAL NOT NULL CHECK(subtotal >= 0),
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(venta_id) REFERENCES ventas(id) ON DELETE CASCADE,
    FOREIGN KEY(variante_id) REFERENCES variantes(id) ON DELETE RESTRICT
);

CREATE TABLE IF NOT EXISTS pagos_venta (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    venta_id INTEGER NOT NULL,
    metodo_pago_id INTEGER NOT NULL,
    monto REAL NOT NULL CHECK(monto > 0),
    cuotas INTEGER DEFAULT 1 CHECK(cuotas > 0),
    comision REAL DEFAULT 0 CHECK(comision >= 0),
    observaciones TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(venta_id) REFERENCES ventas(id) ON DELETE CASCADE,
    FOREIGN KEY(metodo_pago_id) REFERENCES metodos_pago(id) ON DELETE RESTRICT
);

-- ============================================
-- TABLA DE AUDITORÍA
-- ============================================

CREATE TABLE IF NOT EXISTS movimientos_stock (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    variante_id INTEGER NOT NULL,
    tipo TEXT NOT NULL CHECK(tipo IN ('INGRESO', 'EGRESO', 'AJUSTE', 'VENTA', 'DEVOLUCION')),
    cantidad INTEGER NOT NULL,
    stock_anterior INTEGER NOT NULL,
    stock_nuevo INTEGER NOT NULL,
    referencia TEXT,
    observaciones TEXT,
    usuario TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(variante_id) REFERENCES variantes(id) ON DELETE RESTRICT
);

-- ============================================
-- CONFIGURACIÓN
-- ============================================

CREATE TABLE IF NOT EXISTS configuracion (
    clave TEXT PRIMARY KEY,
    valor TEXT NOT NULL,
    descripcion TEXT,
    tipo TEXT DEFAULT 'TEXTO' CHECK(tipo IN ('TEXTO', 'NUMERO', 'BOOLEAN')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- ÍNDICES (CORREGIDOS)
-- ============================================

-- Productos
CREATE INDEX IF NOT EXISTS idx_productos_codigo ON productos(codigo);
CREATE INDEX IF NOT EXISTS idx_productos_categoria ON productos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_productos_activo ON productos(activo);
CREATE INDEX IF NOT EXISTS idx_productos_marca ON productos(marca);

-- Variantes
CREATE INDEX IF NOT EXISTS idx_variantes_sku ON variantes(sku);
CREATE INDEX IF NOT EXISTS idx_variantes_producto ON variantes(producto_id);
CREATE INDEX IF NOT EXISTS idx_variantes_activo ON variantes(activo);
CREATE INDEX IF NOT EXISTS idx_variantes_stock ON variantes(stock);
CREATE INDEX IF NOT EXISTS idx_variantes_codigo_barras ON variantes(codigo_barras);

-- Atributos
CREATE INDEX IF NOT EXISTS idx_variante_atributos_variante ON variante_atributos(variante_id);
CREATE INDEX IF NOT EXISTS idx_variante_atributos_atributo ON variante_atributos(atributo_id);
CREATE INDEX IF NOT EXISTS idx_valores_atributo_atributo ON valores_atributo(atributo_id);

-- Clientes
CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre);
CREATE INDEX IF NOT EXISTS idx_clientes_telefono ON clientes(telefono);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);
CREATE INDEX IF NOT EXISTS idx_clientes_tipo ON clientes(tipo_cliente_id);

-- Ventas
CREATE INDEX IF NOT EXISTS idx_ventas_numero ON ventas(numero_comprobante);
CREATE INDEX IF NOT EXISTS idx_ventas_cliente ON ventas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha ON ventas(fecha);
CREATE INDEX IF NOT EXISTS idx_ventas_estado ON ventas(estado);

-- Detalles y Pagos
CREATE INDEX IF NOT EXISTS idx_detalle_venta ON detalle_venta(venta_id);
CREATE INDEX IF NOT EXISTS idx_detalle_variante ON detalle_venta(variante_id);
CREATE INDEX IF NOT EXISTS idx_pagos_venta ON pagos_venta(venta_id);
CREATE INDEX IF NOT EXISTS idx_pagos_metodo ON pagos_venta(metodo_pago_id);

-- Movimientos
CREATE INDEX IF NOT EXISTS idx_movimientos_variante ON movimientos_stock(variante_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo ON movimientos_stock(tipo);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_stock(created_at);

-- ============================================
-- TRIGGERS (CORREGIDOS)
-- ============================================

CREATE TRIGGER IF NOT EXISTS trg_productos_updated_at
AFTER UPDATE ON productos
FOR EACH ROW
BEGIN
    UPDATE productos SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS trg_variantes_updated_at
AFTER UPDATE ON variantes
FOR EACH ROW
BEGIN
    UPDATE variantes SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS trg_categorias_updated_at
AFTER UPDATE ON categorias
FOR EACH ROW
BEGIN
    UPDATE categorias SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS trg_clientes_updated_at
AFTER UPDATE ON clientes
FOR EACH ROW
BEGIN
    UPDATE clientes SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS trg_ventas_updated_at
AFTER UPDATE ON ventas
FOR EACH ROW
BEGIN
    UPDATE ventas SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS trg_generar_numero_comprobante
AFTER INSERT ON ventas
FOR EACH ROW
WHEN NEW.numero_comprobante IS NULL
BEGIN
    UPDATE ventas 
    SET numero_comprobante = printf('%s-%08d', NEW.tipo_comprobante, NEW.id)
    WHERE id = NEW.id;
END;

CREATE TRIGGER IF NOT EXISTS trg_movimiento_venta
AFTER INSERT ON detalle_venta
FOR EACH ROW
BEGIN
    INSERT INTO movimientos_stock (variante_id, tipo, cantidad, stock_anterior, stock_nuevo, referencia)
    SELECT 
        v.id,
        'VENTA',
        NEW.cantidad,
        v.stock,
        v.stock - NEW.cantidad,
        'Venta #' || (SELECT numero_comprobante FROM ventas WHERE id = NEW.venta_id)
    FROM variantes v
    WHERE v.id = NEW.variante_id;
    
    UPDATE variantes 
    SET stock = stock - NEW.cantidad
    WHERE id = NEW.variante_id;
END;

CREATE TRIGGER IF NOT EXISTS trg_revertir_stock_anulacion
AFTER UPDATE ON ventas
FOR EACH ROW
WHEN NEW.estado = 'ANULADA' AND OLD.estado != 'ANULADA'
BEGIN
    UPDATE variantes
    SET stock = stock + (
        SELECT dv.cantidad 
        FROM detalle_venta dv 
        WHERE dv.venta_id = NEW.id AND dv.variante_id = variantes.id
    )
    WHERE id IN (SELECT variante_id FROM detalle_venta WHERE venta_id = NEW.id);
    
    INSERT INTO movimientos_stock (variante_id, tipo, cantidad, stock_anterior, stock_nuevo, referencia, observaciones)
    SELECT 
        dv.variante_id,
        'DEVOLUCION',
        dv.cantidad,
        v.stock - dv.cantidad,
        v.stock,
        'Anulación Venta #' || NEW.numero_comprobante,
        'Devolución automática por anulación'
    FROM detalle_venta dv
    JOIN variantes v ON v.id = dv.variante_id
    WHERE dv.venta_id = NEW.id;
END;

-- ============================================
-- VISTAS (CORREGIDAS)
-- ============================================

CREATE VIEW IF NOT EXISTS v_variantes_completas AS
SELECT 
    v.id,
    v.sku,
    v.codigo_barras,
    v.precio_minorista,
    v.precio_mayorista,
    v.stock,
    v.stock_minimo,
    v.activo,
    p.id as producto_id,
    p.codigo as producto_codigo,
    p.nombre as producto_nombre,
    p.marca,
    c.id as categoria_id,
    c.nombre as categoria_nombre,
    GROUP_CONCAT(a.nombre || ': ' || va.valor, ' | ') as atributos_texto
FROM variantes v
INNER JOIN productos p ON v.producto_id = p.id
INNER JOIN categorias c ON p.categoria_id = c.id
LEFT JOIN variante_atributos va ON v.id = va.variante_id
LEFT JOIN atributos a ON va.atributo_id = a.id
GROUP BY v.id;

CREATE VIEW IF NOT EXISTS v_stock_bajo AS
SELECT 
    v.id,
    v.sku,
    v.stock,
    v.stock_minimo,
    (v.stock_minimo - v.stock) as faltante,
    p.nombre as producto_nombre,
    c.nombre as categoria_nombre
FROM variantes v
INNER JOIN productos p ON v.producto_id = p.id
INNER JOIN categorias c ON p.categoria_id = c.id
WHERE v.stock < v.stock_minimo AND v.activo = 1;

CREATE VIEW IF NOT EXISTS v_ventas_completas AS
SELECT 
    v.id,
    v.numero_comprobante,
    v.fecha,
    v.hora,
    v.subtotal,
    v.descuento,
    v.total,
    v.tipo_comprobante,
    v.tipo_venta,
    v.estado,
    c.nombre as cliente_nombre,
    c.apellido as cliente_apellido,
    c.telefono as cliente_telefono,
    COUNT(DISTINCT dv.id) as cantidad_items,
    SUM(dv.cantidad) as cantidad_total_productos,
    GROUP_CONCAT(DISTINCT mp.nombre, ', ') as metodos_pago
FROM ventas v
INNER JOIN clientes c ON v.cliente_id = c.id
LEFT JOIN detalle_venta dv ON v.id = dv.venta_id
LEFT JOIN pagos_venta pv ON v.id = pv.venta_id
LEFT JOIN metodos_pago mp ON pv.metodo_pago_id = mp.id
GROUP BY v.id;

-- ============================================
-- DATOS INICIALES
-- ============================================

INSERT OR IGNORE INTO configuracion (clave, valor, descripcion, tipo) VALUES
('empresa_nombre', 'StyleStock Showroom', 'Nombre de la empresa', 'TEXTO'),
('empresa_direccion', '', 'Dirección de la empresa', 'TEXTO'),
('empresa_telefono', '', 'Teléfono de la empresa', 'TEXTO'),
('empresa_cuit', '', 'CUIT de la empresa', 'TEXTO'),
('empresa_email', '', 'Email de la empresa', 'TEXTO'),
('stock_alerta_global', '5', 'Nivel de stock mínimo por defecto', 'NUMERO'),
('backup_auto', '1', 'Activar backup automático', 'BOOLEAN'),
('backup_dias', '7', 'Días entre backups automáticos', 'NUMERO'),
('ultima_version', '2.0.0', 'Versión del esquema de BD', 'TEXTO');

INSERT OR IGNORE INTO tipos_cliente (id, nombre, usa_precio_mayorista, descripcion) VALUES
(1, 'Minorista', 0, 'Cliente que compra a precio de lista'),
(2, 'Mayorista', 1, 'Cliente con precio mayorista');

INSERT OR IGNORE INTO categorias (id, nombre, descripcion, requiere_variantes) VALUES
(1, 'Ropa', 'Indumentaria y prendas de vestir', 1),
(2, 'Vaporizadores', 'Vapers y dispositivos de vapeo', 1),
(3, 'Esencias', 'Líquidos y esencias para vapers', 1),
(4, 'Perfumes', 'Perfumes y fragancias', 1),
(5, 'Accesorios', 'Accesorios varios', 0);

INSERT OR IGNORE INTO atributos (id, nombre, tipo, descripcion, orden) VALUES
(1, 'Talle', 'LISTA', 'Talle de ropa', 1),
(2, 'Color', 'COLOR', 'Color del producto', 2),
(3, 'Sabor', 'LISTA', 'Sabor para vapers y esencias', 3);

INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(1, 'XS', 1), (1, 'S', 2), (1, 'M', 3), (1, 'L', 4), (1, 'XL', 5),
(2, 'Negro', 1), (2, 'Blanco', 2), (2, 'Rojo', 3),
(3, 'Menta', 1), (3, 'Frutilla', 2), (3, 'Uva', 3);

INSERT OR IGNORE INTO categoria_atributos (categoria_id, atributo_id, requerido, orden) VALUES
(1, 1, 1, 1), (1, 2, 1, 2);

INSERT OR IGNORE INTO metodos_pago (nombre, requiere_cuotas, comision_porcentaje) VALUES
('Efectivo', 0, 0),
('Débito', 0, 0),
('Crédito', 1, 5.0),
('Transferencia', 0, 0);

INSERT OR IGNORE INTO clientes (id, nombre, apellido, tipo_cliente_id) VALUES
(1, 'Consumidor', 'Final', 1);