// ============================================
// ConfiguracionService.java
// ============================================
package com.style.stock.service;

import com.style.stock.dao.ConfiguracionDAO;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.Configuracion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class ConfiguracionService {
    private static final Logger logger = LoggerFactory.getLogger(ConfiguracionService.class);
    private final ConfiguracionDAO configuracionDAO;

    public ConfiguracionService() {
        this.configuracionDAO = new ConfiguracionDAO();
    }

    public void guardar(String clave, String valor) throws DataAccessException {
        Configuracion config = new Configuracion(clave, valor);
        configuracionDAO.save(config);
        logger.info("Configuración guardada: {} = {}", clave, valor);
    }

    public void guardar(String clave, Integer valor) throws DataAccessException {
        guardar(clave, String.valueOf(valor));
    }

    public void guardar(String clave, Boolean valor) throws DataAccessException {
        guardar(clave, String.valueOf(valor));
    }

    public void guardar(String clave, Double valor) throws DataAccessException {
        guardar(clave, String.valueOf(valor));
    }

    public String obtener(String clave, String valorPorDefecto) throws DataAccessException {
        return configuracionDAO.findByClave(clave)
            .map(Configuracion::getValor)
            .orElse(valorPorDefecto);
    }

    public String obtener(String clave) throws DataAccessException {
        return obtener(clave, "");
    }

    public Integer obtenerInt(String clave, Integer valorPorDefecto) throws DataAccessException {
        try {
            String valor = obtener(clave);
            return valor.isEmpty() ? valorPorDefecto : Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            logger.warn("Valor no es número para clave: {}", clave);
            return valorPorDefecto;
        }
    }

    public Boolean obtenerBoolean(String clave, Boolean valorPorDefecto) throws DataAccessException {
        String valor = obtener(clave);
        if (valor.isEmpty()) {
            return valorPorDefecto;
        }
        return "true".equalsIgnoreCase(valor) || "1".equals(valor);
    }

    public Double obtenerDouble(String clave, Double valorPorDefecto) throws DataAccessException {
        try {
            String valor = obtener(clave);
            return valor.isEmpty() ? valorPorDefecto : Double.parseDouble(valor);
        } catch (NumberFormatException e) {
            logger.warn("Valor no es número para clave: {}", clave);
            return valorPorDefecto;
        }
    }

    public List<Configuracion> listarTodas() throws DataAccessException {
        return configuracionDAO.findAll();
    }

    public void eliminar(String clave) throws DataAccessException {
        configuracionDAO.delete(clave);
        logger.info("Configuración eliminada: {}", clave);
    }
}
