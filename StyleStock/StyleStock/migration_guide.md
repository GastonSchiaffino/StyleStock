# Guía de Migración - StyleStock v0.2.0 → v1.0.0

## 📌 Resumen de Cambios

Esta guía te ayudará a migrar desde la versión anterior de StyleStock a la versión 1.0.0 mejorada.

## ⚠️ IMPORTANTE: Backup Antes de Migrar

**ANTES DE CONTINUAR, HAZ UN BACKUP DE TU BASE DE DATOS ACTUAL:**

```bash
# Linux/Mac
cp ~/style-stock/style_stock.db ~/style-stock/style_stock_backup_$(date +%Y%m%d).db

# Windows (PowerShell)
Copy-Item "$env:USERPROFILE\style-stock\style_stock.db" "$env:USERPROFILE\style-stock\style_stock_backup_$(Get-Date -Format yyyyMMdd).db"
```

## 🔄 Proceso de Migración

### Paso 1: Backup de Datos

1. Cierra la aplicación actual si está corriendo
2. Haz backup de:
   - Base de datos: `~/style-stock/style_stock.db`
   - Configuración: `~/style-stock/config.properties`
   - Facturas PDF: `~/style-stock/facturas/`

### Paso 2: Actualizar el Código

```bash
# Descargar la nueva versión
git pull origin main

# O descargar el ZIP y extraer
```

### Paso 3: Limpiar y Compilar

```bash
mvn clean install
```

### Paso 4: Ejecutar Script de Migración de BD

La nueva versión tiene un schema mejorado. Ejecuta este script SQL:

```sql
-- Agregar nuevas columnas a productos
ALTER TABLE productos ADD COLUMN stock_minimo INTEGER DEFAULT 5;
ALTER TABLE productos ADD COLUMN categoria TEXT;
ALTER TABLE productos ADD COLUMN activo INTEGER DEFAULT 1;
ALTER TABLE productos ADD COLUMN created_at TEXT DEFAULT (datetime('now', 'localtime'));
ALTER TABLE productos ADD COLUMN updated_at TEXT DEFAULT (datetime('now', 'localtime'));

-- Agregar nuevas columnas a clientes
ALTER TABLE clientes ADD COLUMN email TEXT;
ALTER TABLE clientes ADD COLUMN activo INTEGER DEFAULT 1;
ALTER TABLE clientes ADD COLUMN created_at TEXT DEFAULT (datetime('now', 'localtime'));
ALTER TABLE clientes ADD COLUMN updated_at TEXT DEFAULT (datetime('now', 'localtime'));

-- Agregar nuevas columnas a facturas
ALTER TABLE facturas ADD COLUMN numero_factura TEXT;
ALTER TABLE facturas ADD COLUMN subtotal REAL DEFAULT 0;
ALTER TABLE facturas ADD COLUMN descuento REAL DEFAULT 0;
ALTER TABLE facturas ADD COLUMN estado TEXT DEFAULT 'EMITIDA';
ALTER TABLE facturas ADD COLUMN notas TEXT;
ALTER TABLE facturas ADD COLUMN created_at TEXT DEFAULT (datetime('now', 'localtime'));
ALTER TABLE facturas ADD COLUMN updated_at TEXT DEFAULT (datetime('now', 'localtime'));

-- Agregar nuevas columnas a detalle_factura
ALTER TABLE detalle_factura ADD COLUMN descuento REAL DEFAULT 0;
ALTER TABLE detalle_factura ADD COLUMN created_at TEXT DEFAULT