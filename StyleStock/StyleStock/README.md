# StyleStock - Sistema de Gestión de Stock y Facturación

## 📋 Descripción

StyleStock es un sistema completo de gestión de stock y facturación desarrollado en Java con JavaFX para interfaces gráficas y SQLite como base de datos. Diseñado para pequeñas y medianas empresas que necesitan controlar su inventario y generar facturas de manera eficiente.

## ✨ Características

### Gestión de Productos
- CRUD completo (Crear, Leer, Actualizar, Eliminar)
- Control de stock con alertas de stock bajo
- Categorización de productos
- Stock mínimo configurable
- Búsqueda y filtrado en tiempo real
- Auditoría de movimientos de stock

### Gestión de Clientes
- Administración completa de clientes
- Validación de CUIT
- Historial de compras por cliente
- Búsqueda rápida

### Facturación
- Creación de facturas tipos A, B y C
- Cálculo automático de totales
- Descuentos por factura
- Generación de PDF profesional
- Control automático de stock
- Anulación de facturas con reversión de stock
- Historial de facturas

### Características Técnicas
- **Arquitectura en capas** (Controller, Service, DAO, Model)
- **Connection Pool** con HikariCP para mejor rendimiento
- **Logging profesional** con SLF4J y Logback
- **Manejo robusto de excepciones** personalizadas
- **Validaciones exhaustivas** en todos los niveles
- **Transacciones ACID** para operaciones críticas
- **Backup automático** de base de datos
- **Exportación a Excel** de reportes
- **Temas** claro y oscuro
- **Auditoría** completa de operaciones

## 🛠️ Requisitos

- **JDK 17** o superior
- **Maven 3.8+**
- **IntelliJ IDEA** (recomendado) o cualquier IDE Java

## 📦 Instalación

### 1. Clonar el repositorio
```bash
git clone https://github.com/tu-usuario/stylestock.git
cd stylestock
```

### 2. Compilar el proyecto
```bash
mvn clean compile
```

### 3. Ejecutar desde Maven
```bash
mvn javafx:run
```

### 4. O ejecutar desde IntelliJ
1. Abrir el proyecto en IntelliJ IDEA
2. Esperar a que Maven descargue las dependencias
3. Ejecutar la clase `com.style.stock.Main`

## 📦 Generar JAR ejecutable

```bash
mvn clean package
```

El JAR se generará en `target/style-stock-1.0.0.jar`

Para ejecutarlo:
```bash
java -jar target/style-stock-1.0.0.jar
```

## 📁 Estructura del Proyecto

```
stylestock/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/style/stock/
│   │   │       ├── Main.java                 # Clase principal
│   │   │       ├── controller/               # Controladores JavaFX
│   │   │       │   ├── MainController.java
│   │   │       │   ├── ProductoController.java
│   │   │       │   ├── ClienteController.java
│   │   │       │   └── FacturaController.java
│   │   │       ├── service/                  # Lógica de negocio
│   │   │       │   ├── ProductoService.java
│   │   │       │   ├── ClienteService.java
│   │   │       │   ├── FacturaService.java
│   │   │       │   └── PDFService.java
│   │   │       ├── dao/                      # Acceso a datos
│   │   │       │   ├── ProductoDAO.java
│   │   │       │   ├── ClienteDAO.java
│   │   │       │   └── FacturaDAO.java
│   │   │       ├── model/                    # Modelos de datos
│   │   │       │   ├── Producto.java
│   │   │       │   ├── Cliente.java
│   │   │       │   ├── Factura.java
│   │   │       │   ├── DetalleFactura.java
│   │   │       │   └── MovimientoStock.java
│   │   │       ├── database/                 # Gestión de BD
│   │   │       │   └── DatabaseManager.java
│   │   │       ├── exception/                # Excepciones personalizadas
│   │   │       │   ├── StockException.java
│   │   │       │   ├── ValidationException.java
│   │   │       │   ├── BusinessException.java
│   │   │       │   └── DataAccessException.java
│   │   │       └── util/                     # Utilidades
│   │   │           ├── AlertUtils.java
│   │   │           ├── AppConfig.java
│   │   │           ├── BackupUtils.java
│   │   │           └── ExcelExportUtils.java
│   │   └── resources/
│   │       ├── fxml/                         # Archivos FXML
│   │       │   ├── main-view.fxml
│   │       │   ├── producto-view.fxml
│   │       │   ├── cliente-view.fxml
│   │       │   └── factura-view.fxml
│   │       ├── css/                          # Estilos
│   │       │   ├── light.css
│   │       │   └── dark.css
│   │       ├── icons/                        # Íconos
│   │       ├── sql/                          # Scripts SQL
│   │       │   └── create_tables.sql
│   │       └── logback.xml                   # Configuración logging
│   └── test/                                 # Tests unitarios
├── pom.xml                                   # Configuración Maven
└── README.md
```

## 🗄️ Ubicación de Archivos

La aplicación crea automáticamente los siguientes directorios en la carpeta del usuario:

```
~/style-stock/
├── style_stock.db          # Base de datos SQLite
├── config.properties        # Configuración de la aplicación
├── logs/                    # Logs de la aplicación
│   ├── stylestock.log
│   └── errors.log
├── facturas/                # PDFs de facturas generadas
├── backups/                 # Backups automáticos de la BD
└── exports/                 # Exportaciones a Excel
```

## 🔧 Configuración

### Archivo config.properties

El archivo se crea automáticamente con valores por defecto, pero puede editarse:

```properties
# Tema de la interfaz (light o dark)
theme=light

# Backup automático
backup_auto=true
backup_dias=7
ultimo_backup=2025-10-26

# Alertas de stock
stock_alerta_activa=true
stock_alerta=5
```

### Base de Datos

La base de datos se crea automáticamente al iniciar la aplicación. Incluye:
- Tablas con constraints y validaciones
- Índices para optimizar consultas
- Triggers para auditoría automática
- Vistas para reportes

## 📊 Características de la Base de Datos

- **Integridad referencial** con claves foráneas
- **Soft delete** (eliminación lógica)
- **Timestamps automáticos** (created_at, updated_at)
- **Auditoría de stock** con tabla de movimientos
- **Triggers** para actualización automática de fechas
- **Vistas** para consultas complejas
- **Índices** en campos frecuentemente consultados

## 🎨 Temas

La aplicación incluye dos temas:
- **Claro**: Diseño limpio y profesional
- **Oscuro**: Ideal para trabajar de noche

Cambiar tema: Usar el selector en la barra superior de la aplicación.

## 📝 Logging

Los logs se guardan en `~/style-stock/logs/`:
- `stylestock.log`: Log general de la aplicación
- `errors.log`: Solo errores críticos

Configuración en `src/main/resources/logback.xml`

## 🔒 Seguridad

- Validaciones en múltiples capas (UI, Service, DAO)
- Uso de PreparedStatements para prevenir SQL Injection
- Validación de tipos de datos
- Manejo seguro de excepciones sin exponer información sensible

## 🚀 Mejoras Implementadas vs Versión Original

### Arquitectura
✅ Separación en capas (Controller, Service, DAO)
✅ Patrones de diseño (Singleton, DAO, Service Layer)
✅ Inyección de dependencias manual

### Base de Datos
✅ Connection Pool con HikariCP
✅ Índices para optimización
✅ Triggers para auditoría
✅ Vistas para reportes
✅ Constraints y validaciones
✅ Tabla de auditoría de movimientos

### Funcionalidades
✅ Sistema de logging profesional
✅ Backup automático
✅ Exportación a Excel
✅ Validaciones robustas
✅ Manejo transaccional completo
✅ Búsqueda en tiempo real
✅ Confirmaciones de eliminación
✅ Indicadores de progreso
✅ Feedback visual mejorado

### Código
✅ Manejo de excepciones personalizado
✅ Código documentado
✅ Validaciones exhaustivas
✅ Separación de responsabilidades
✅ Código testeable
✅ Logs estructurados

## 📚 Dependencias Principales

- **JavaFX 21**: Framework de UI
- **SQLite JDBC 3.44**: Driver de base de datos
- **HikariCP 5.1**: Connection pooling
- **Apache PDFBox 3.0**: Generación de PDFs
- **Apache POI 5.2**: Exportación a Excel
- **SLF4J + Logback**: Logging
- **Apache Commons Lang3**: Utilidades

## 🤝 Contribuir

Las contribuciones son bienvenidas. Por favor:
1. Fork el proyecto
2. Crea una rama para tu feature (`git checkout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature'`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Request

## 📄 Licencia

Este proyecto está bajo la Licencia MIT.

## 👥 Soporte

Para soporte o consultas:
- Abrir un issue en GitHub
- Email: soporte@stylestock.com

## 🔄 Roadmap Futuro

- [ ] Reportes avanzados con gráficos
- [ ] Sistema de usuarios y permisos
- [ ] Sincronización en la nube (opcional)
- [ ] App móvil companion
- [ ] Integración con AFIP
- [ ] Sistema de caja
- [ ] Gestión de proveedores
- [ ] Órdenes de compra
- [ ] Dashboard con KPIs

## 📸 Capturas de Pantalla

(Agregar capturas de pantalla de la aplicación)

---

**Versión**: 1.0.0  
**Última actualización**: Octubre 2025  
**Autor**: StyleStock Team