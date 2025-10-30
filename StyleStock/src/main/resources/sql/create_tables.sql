-- ============================================
-- StyleStock v2.0 - Schema con Modelo Flexible
-- Sistema de atributos dinámicos para productos
-- ============================================

-- ============================================
-- TABLAS DE CONFIGURACIÓN
-- ============================================

-- Categorías de productos
CREATE TABLE IF NOT EXISTS categorias (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    descripcion TEXT,
    requiere_variantes INTEGER DEFAULT 1, -- Si necesita variantes (combinaciones) o es producto simple
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Definición de atributos posibles (Talle, Color, Sabor, etc.)
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

-- Valores predefinidos para atributos tipo LISTA
CREATE TABLE IF NOT EXISTS valores_atributo (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    atributo_id INTEGER NOT NULL,
    valor TEXT NOT NULL,
    codigo_hex TEXT, -- Para colores (ej: #FF0000)
    orden INTEGER DEFAULT 0,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(atributo_id) REFERENCES atributos(id) ON DELETE CASCADE,
    UNIQUE(atributo_id, valor)
);

-- Relación: Qué atributos aplican a cada categoría
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

-- Productos base
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

-- Variantes: Combinaciones específicas de atributos
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

-- Valores de atributos para cada variante
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

CREATE TABLE IF NOT EXISTS clientes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL,
    apellido TEXT,
    direccion TEXT,
    telefono TEXT,
    email TEXT,
    cuit TEXT,
    notas TEXT,
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- TABLAS DE VENTAS/FACTURACIÓN
-- ============================================

-- Métodos de pago configurables
CREATE TABLE IF NOT EXISTS metodos_pago (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    nombre TEXT NOT NULL UNIQUE,
    requiere_cuotas INTEGER DEFAULT 0,
    comision_porcentaje REAL DEFAULT 0 CHECK(comision_porcentaje >= 0),
    activo INTEGER DEFAULT 1,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- Facturas/Ventas
CREATE TABLE IF NOT EXISTS facturas (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    numero_factura TEXT UNIQUE,
    cliente_id INTEGER NOT NULL,
    fecha TEXT NOT NULL DEFAULT (date('now', 'localtime')),
    subtotal REAL NOT NULL DEFAULT 0 CHECK(subtotal >= 0),
    descuento REAL DEFAULT 0 CHECK(descuento >= 0),
    total REAL NOT NULL DEFAULT 0 CHECK(total >= 0),
    tipo TEXT DEFAULT 'A' CHECK(tipo IN ('A', 'B', 'C', 'X')), -- X para ticket
    estado TEXT DEFAULT 'EMITIDA' CHECK(estado IN ('EMITIDA', 'ANULADA', 'PAGADA')),
    notas TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(cliente_id) REFERENCES clientes(id) ON DELETE RESTRICT
);

-- Detalle de factura (productos vendidos)
CREATE TABLE IF NOT EXISTS detalle_factura (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    factura_id INTEGER NOT NULL,
    variante_id INTEGER NOT NULL,
    cantidad INTEGER NOT NULL CHECK(cantidad > 0),
    precio_unitario REAL NOT NULL CHECK(precio_unitario >= 0),
    descuento REAL DEFAULT 0 CHECK(descuento >= 0),
    subtotal REAL NOT NULL CHECK(subtotal >= 0),
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(factura_id) REFERENCES facturas(id) ON DELETE CASCADE,
    FOREIGN KEY(variante_id) REFERENCES variantes(id) ON DELETE RESTRICT
);

-- Pagos de facturas (soporta pagos múltiples/combinados)
CREATE TABLE IF NOT EXISTS pagos_factura (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    factura_id INTEGER NOT NULL,
    metodo_pago_id INTEGER NOT NULL,
    monto REAL NOT NULL CHECK(monto > 0),
    cuotas INTEGER DEFAULT 1 CHECK(cuotas > 0),
    comision REAL DEFAULT 0 CHECK(comision >= 0),
    observaciones TEXT,
    created_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime')),
    FOREIGN KEY(factura_id) REFERENCES facturas(id) ON DELETE CASCADE,
    FOREIGN KEY(metodo_pago_id) REFERENCES metodos_pago(id) ON DELETE RESTRICT
);

-- ============================================
-- TABLA DE AUDITORÍA
-- ============================================

-- Movimientos de stock (auditoría)
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
-- TABLA DE CONFIGURACIÓN GENERAL
-- ============================================

CREATE TABLE IF NOT EXISTS configuracion (
    clave TEXT PRIMARY KEY,
    valor TEXT NOT NULL,
    descripcion TEXT,
    tipo TEXT DEFAULT 'TEXTO' CHECK(tipo IN ('TEXTO', 'NUMERO', 'BOOLEAN')),
    updated_at TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))
);

-- ============================================
-- ÍNDICES PARA OPTIMIZACIÓN
-- ============================================

-- Índices de modelos
CREATE INDEX IF NOT EXISTS idx_modelos_codigo ON modelos(codigo);
CREATE INDEX IF NOT EXISTS idx_modelos_categoria ON modelos(categoria_id);
CREATE INDEX IF NOT EXISTS idx_modelos_activo ON modelos(activo);
CREATE INDEX IF NOT EXISTS idx_modelos_marca ON modelos(marca);

-- Índices de variantes
CREATE INDEX IF NOT EXISTS idx_variantes_sku ON variantes(sku);
CREATE INDEX IF NOT EXISTS idx_variantes_modelo ON variantes(modelo_id);
CREATE INDEX IF NOT EXISTS idx_variantes_activo ON variantes(activo);
CREATE INDEX IF NOT EXISTS idx_variantes_stock ON variantes(stock);
CREATE INDEX IF NOT EXISTS idx_variantes_codigo_barras ON variantes(codigo_barras);

-- Índices de atributos
CREATE INDEX IF NOT EXISTS idx_variante_atributos_variante ON variante_atributos(variante_id);
CREATE INDEX IF NOT EXISTS idx_variante_atributos_atributo ON variante_atributos(atributo_id);
CREATE INDEX IF NOT EXISTS idx_valores_atributo_atributo ON valores_atributo(atributo_id);

-- Índices de clientes
CREATE INDEX IF NOT EXISTS idx_clientes_nombre ON clientes(nombre);
CREATE INDEX IF NOT EXISTS idx_clientes_telefono ON clientes(telefono);
CREATE INDEX IF NOT EXISTS idx_clientes_activo ON clientes(activo);

-- Índices de facturas
CREATE INDEX IF NOT EXISTS idx_facturas_numero ON facturas(numero_factura);
CREATE INDEX IF NOT EXISTS idx_facturas_cliente ON facturas(cliente_id);
CREATE INDEX IF NOT EXISTS idx_facturas_fecha ON facturas(fecha);
CREATE INDEX IF NOT EXISTS idx_facturas_estado ON facturas(estado);

-- Índices de detalles
CREATE INDEX IF NOT EXISTS idx_detalle_factura ON detalle_factura(factura_id);
CREATE INDEX IF NOT EXISTS idx_detalle_variante ON detalle_factura(variante_id);

-- Índices de pagos
CREATE INDEX IF NOT EXISTS idx_pagos_factura ON pagos_factura(factura_id);
CREATE INDEX IF NOT EXISTS idx_pagos_metodo ON pagos_factura(metodo_pago_id);

-- Índices de movimientos
CREATE INDEX IF NOT EXISTS idx_movimientos_variante ON movimientos_stock(variante_id);
CREATE INDEX IF NOT EXISTS idx_movimientos_tipo ON movimientos_stock(tipo);
CREATE INDEX IF NOT EXISTS idx_movimientos_fecha ON movimientos_stock(created_at);

-- ============================================
-- TRIGGERS PARA AUDITORÍA
-- ============================================

-- Trigger para actualizar updated_at en modelos
CREATE TRIGGER IF NOT EXISTS trg_modelos_updated_at
AFTER UPDATE ON modelos
FOR EACH ROW
BEGIN
    UPDATE modelos SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- Trigger para actualizar updated_at en variantes
CREATE TRIGGER IF NOT EXISTS trg_variantes_updated_at
AFTER UPDATE ON variantes
FOR EACH ROW
BEGIN
    UPDATE variantes SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- Trigger para actualizar updated_at en categorías
CREATE TRIGGER IF NOT EXISTS trg_categorias_updated_at
AFTER UPDATE ON categorias
FOR EACH ROW
BEGIN
    UPDATE categorias SET updated_at = datetime('now', 'localtime')
    WHERE id = NEW.id;
END;

-- Trigger para actualizar updated_at en atributos
CREATE TRIGGER IF NOT EXISTS trg_atributos_updated_at
AFTER UPDATE ON atributos
FOR EACH ROW
BEGIN
    UPDATE atributos SET updated_at = datetime('now', 'localtime')
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

-- Trigger para registrar movimiento de stock en ventas
CREATE TRIGGER IF NOT EXISTS trg_movimiento_venta
AFTER INSERT ON detalle_factura
FOR EACH ROW
BEGIN
    INSERT INTO movimientos_stock (variante_id, tipo, cantidad, stock_anterior, stock_nuevo, referencia)
    SELECT 
        v.id,
        'VENTA',
        NEW.cantidad,
        v.stock,
        v.stock - NEW.cantidad,
        'Factura #' || (SELECT numero_factura FROM facturas WHERE id = NEW.factura_id)
    FROM variantes v
    WHERE v.id = NEW.variante_id;
    
    UPDATE variantes 
    SET stock = stock - NEW.cantidad
    WHERE id = NEW.variante_id;
END;

-- Trigger para revertir stock en anulación de factura
CREATE TRIGGER IF NOT EXISTS trg_revertir_stock_anulacion
AFTER UPDATE ON facturas
FOR EACH ROW
WHEN NEW.estado = 'ANULADA' AND OLD.estado != 'ANULADA'
BEGIN
    -- Revertir stock de todos los items
    UPDATE variantes
    SET stock = stock + (
        SELECT df.cantidad 
        FROM detalle_factura df 
        WHERE df.factura_id = NEW.id AND df.variante_id = variantes.id
    )
    WHERE id IN (SELECT variante_id FROM detalle_factura WHERE factura_id = NEW.id);
    
    -- Registrar movimientos de devolución
    INSERT INTO movimientos_stock (variante_id, tipo, cantidad, stock_anterior, stock_nuevo, referencia, observaciones)
    SELECT 
        df.variante_id,
        'DEVOLUCION',
        df.cantidad,
        v.stock - df.cantidad,
        v.stock,
        'Anulación Factura #' || NEW.numero_factura,
        'Devolución automática por anulación'
    FROM detalle_factura df
    JOIN variantes v ON v.id = df.variante_id
    WHERE df.factura_id = NEW.id;
END;

-- ============================================
-- VISTAS ÚTILES PARA REPORTES
-- ============================================

-- Vista de variantes con toda su información
CREATE VIEW IF NOT EXISTS v_variantes_completas AS
SELECT 
    v.id,
    v.sku,
    v.codigo_barras,
    v.precio,
    v.stock,
    v.stock_minimo,
    v.activo,
    m.id as modelo_id,
    m.codigo as modelo_codigo,
    m.nombre as modelo_nombre,
    m.marca,
    c.id as categoria_id,
    c.nombre as categoria_nombre,
    GROUP_CONCAT(a.nombre || ': ' || va.valor, ' | ') as atributos_texto
FROM variantes v
INNER JOIN modelos m ON v.modelo_id = m.id
INNER JOIN categorias c ON m.categoria_id = c.id
LEFT JOIN variante_atributos va ON v.id = va.variante_id
LEFT JOIN atributos a ON va.atributo_id = a.id
GROUP BY v.id;

-- Vista de productos con stock bajo
CREATE VIEW IF NOT EXISTS v_stock_bajo AS
SELECT 
    v.id,
    v.sku,
    v.stock,
    v.stock_minimo,
    (v.stock_minimo - v.stock) as faltante,
    m.nombre as modelo_nombre,
    c.nombre as categoria_nombre
FROM variantes v
INNER JOIN modelos m ON v.modelo_id = m.id
INNER JOIN categorias c ON m.categoria_id = c.id
WHERE v.stock < v.stock_minimo AND v.activo = 1;

-- Vista de facturas completas
CREATE VIEW IF NOT EXISTS v_facturas_completas AS
SELECT 
    f.id,
    f.numero_factura,
    f.fecha,
    f.subtotal,
    f.descuento,
    f.total,
    f.tipo,
    f.estado,
    c.nombre as cliente_nombre,
    c.apellido as cliente_apellido,
    c.telefono as cliente_telefono,
    COUNT(DISTINCT df.id) as cantidad_items,
    SUM(df.cantidad) as cantidad_total_productos,
    GROUP_CONCAT(DISTINCT mp.nombre, ', ') as metodos_pago
FROM facturas f
INNER JOIN clientes c ON f.cliente_id = c.id
LEFT JOIN detalle_factura df ON f.id = df.factura_id
LEFT JOIN pagos_factura pf ON f.id = pf.factura_id
LEFT JOIN metodos_pago mp ON pf.metodo_pago_id = mp.id
GROUP BY f.id;

-- ============================================
-- DATOS INICIALES
-- ============================================

-- Configuración general
INSERT OR IGNORE INTO configuracion (clave, valor, descripcion, tipo) VALUES
('empresa_nombre', 'StyleStock Showroom', 'Nombre de la empresa', 'TEXTO'),
('empresa_direccion', '', 'Dirección de la empresa', 'TEXTO'),
('empresa_telefono', '', 'Teléfono de la empresa', 'TEXTO'),
('empresa_cuit', '', 'CUIT de la empresa', 'TEXTO'),
('empresa_email', '', 'Email de la empresa', 'TEXTO'),
('factura_pie', 'Gracias por su compra', 'Texto al pie de la factura', 'TEXTO'),
('stock_alerta_global', '5', 'Nivel de stock mínimo por defecto', 'NUMERO'),
('backup_auto', '1', 'Activar backup automático', 'BOOLEAN'),
('backup_dias', '7', 'Días entre backups automáticos', 'NUMERO'),
('generar_sku_auto', '1', 'Generar SKU automáticamente', 'BOOLEAN'),
('ultima_version', '2.0.0', 'Versión del esquema de BD', 'TEXTO');

-- Categorías iniciales
INSERT OR IGNORE INTO categorias (id, nombre, descripcion, requiere_variantes) VALUES
(1, 'Ropa', 'Indumentaria y prendas de vestir', 1),
(2, 'Vaporizadores', 'Vapers y dispositivos de vapeo', 1),
(3, 'Esencias', 'Líquidos y esencias para vapers', 1),
(4, 'Perfumes', 'Perfumes y fragancias', 1),
(5, 'Accesorios', 'Accesorios varios', 0);

-- Atributos base
INSERT OR IGNORE INTO atributos (id, nombre, tipo, descripcion, orden) VALUES
(1, 'Talle', 'LISTA', 'Talle de ropa', 1),
(2, 'Color', 'COLOR', 'Color del producto', 2),
(3, 'Sabor', 'LISTA', 'Sabor para vapers y esencias', 3),
(4, 'Aroma', 'LISTA', 'Aroma de perfumes', 4),
(5, 'Concentración', 'LISTA', 'Concentración de nicotina o perfume', 5),
(6, 'Volumen', 'LISTA', 'Volumen o capacidad', 6),
(7, 'Material', 'LISTA', 'Material del producto', 7),
(8, 'Tipo', 'LISTA', 'Tipo de producto', 8),
(9, 'Género', 'LISTA', 'Género (masculino/femenino/unisex)', 9),
(10, 'Capacidad', 'LISTA', 'Capacidad en puffs o ml', 10);

-- Valores para Talles
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(1, 'XS', 1), (1, 'S', 2), (1, 'M', 3), (1, 'L', 4), (1, 'XL', 5), (1, 'XXL', 6),
(1, '36', 7), (1, '38', 8), (1, '40', 9), (1, '42', 10), (1, '44', 11), 
(1, '46', 12), (1, '48', 13), (1, '50', 14);

-- Valores para Colores
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, codigo_hex, orden) VALUES
(2, 'Negro', '#000000', 1),
(2, 'Blanco', '#FFFFFF', 2),
(2, 'Gris', '#808080', 3),
(2, 'Azul', '#0000FF', 4),
(2, 'Rojo', '#FF0000', 5),
(2, 'Verde', '#00FF00', 6),
(2, 'Amarillo', '#FFFF00', 7),
(2, 'Rosa', '#FFC0CB', 8),
(2, 'Naranja', '#FFA500', 9),
(2, 'Violeta', '#8B00FF', 10);

-- Valores para Sabores
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(3, 'Menta', 1),
(3, 'Frutilla', 2),
(3, 'Uva', 3),
(3, 'Sandía', 4),
(3, 'Mango', 5),
(3, 'Durazno', 6),
(3, 'Banana', 7),
(3, 'Cereza', 8),
(3, 'Limón', 9),
(3, 'Naranja', 10),
(3, 'Tabaco', 11),
(3, 'Café', 12);

-- Valores para Aromas
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(4, 'Amaderado', 1),
(4, 'Cítrico', 2),
(4, 'Floral', 3),
(4, 'Oriental', 4),
(4, 'Frutal', 5),
(4, 'Especiado', 6),
(4, 'Fresco', 7),
(4, 'Dulce', 8);

-- Valores para Concentración
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(5, '0mg', 1),
(5, '3mg', 2),
(5, '6mg', 3),
(5, '12mg', 4),
(5, '18mg', 5),
(5, 'Eau de Toilette', 6),
(5, 'Eau de Parfum', 7),
(5, 'Parfum', 8);

-- Valores para Volumen
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(6, '10ml', 1),
(6, '30ml', 2),
(6, '50ml', 3),
(6, '60ml', 4),
(6, '100ml', 5);

-- Valores para Género
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(9, 'Masculino', 1),
(9, 'Femenino', 2),
(9, 'Unisex', 3);

-- Valores para Capacidad
INSERT OR IGNORE INTO valores_atributo (atributo_id, valor, orden) VALUES
(10, '800 puffs', 1),
(10, '1500 puffs', 2),
(10, '2000 puffs', 3),
(10, '3000 puffs', 4),
(10, '5000 puffs', 5);

-- Relaciones Categoría-Atributos
-- Ropa: Talle, Color, Material
INSERT OR IGNORE INTO categoria_atributos (categoria_id, atributo_id, requerido, orden) VALUES
(1, 1, 1, 1), -- Talle (requerido)
(1, 2, 1, 2), -- Color (requerido)
(1, 7, 0, 3); -- Material (opcional)

-- Vaporizadores: Sabor, Concentración, Capacidad
INSERT OR IGNORE INTO categoria_atributos (categoria_id, atributo_id, requerido, orden) VALUES
(2, 3, 1, 1), -- Sabor (requerido)
(2, 5, 1, 2), -- Concentración (requerido)
(2, 10, 0, 3); -- Capacidad (opcional)

-- Esencias: Sabor, Concentración, Volumen
INSERT OR IGNORE INTO categoria_atributos (categoria_id, atributo_id, requerido, orden) VALUES
(3, 3, 1, 1), -- Sabor (requerido)
(3, 5, 1, 2), -- Concentración (requerido)
(3, 6, 1, 3); -- Volumen (requerido)

-- Perfumes: Aroma, Concentración, Volumen, Género
INSERT OR IGNORE INTO categoria_atributos (categoria_id, atributo_id, requerido, orden) VALUES
(4, 4, 1, 1), -- Aroma (requerido)
(4, 5, 1, 2), -- Concentración (requerido)
(4, 6, 1, 3), -- Volumen (requerido)
(4, 9, 0, 4); -- Género (opcional)

-- Métodos de pago iniciales
INSERT OR IGNORE INTO metodos_pago (nombre, requiere_cuotas, comision_porcentaje) VALUES
('Efectivo', 0, 0),
('Débito', 0, 0),
('Crédito', 1, 5.0),
('Transferencia', 0, 0),
('MercadoPago', 1, 4.5),
('QR', 0, 0);

-- Cliente genérico para ventas sin datos
INSERT OR IGNORE INTO clientes (id, nombre, apellido) VALUES
(1, 'Consumidor', 'Final');