package com.style.stock.service;

import com.style.stock.dao.MovimientoStockDAO;
import com.style.stock.exception.DataAccessException;
import com.style.stock.model.MovimientoStock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;

public class MovimientoStockService {
    private static final Logger logger = LoggerFactory.getLogger(MovimientoStockService.class);
    private final MovimientoStockDAO movimientoDAO;

    public MovimientoStockService() {
        this.movimientoDAO = new MovimientoStockDAO();
    }

    public List<MovimientoStock> buscarMovimientos(LocalDate desde, LocalDate hasta,
                                                   Integer varianteId, String tipo)
            throws DataAccessException {
        logger.debug("Buscando movimientos desde {} hasta {}", desde, hasta);
        return movimientoDAO.findByFechas(desde, hasta, varianteId, tipo);
    }

    public List<MovimientoStock> listarPorVariante(Integer varianteId, int limite)
            throws DataAccessException {
        return movimientoDAO.findByVariante(varianteId, limite);
    }

    public MovimientoStock registrarMovimiento(MovimientoStock movimiento) throws DataAccessException {
        logger.debug("Registrando movimiento: {} para variante {}",
                movimiento.getTipo(), movimiento.getProductoId());
        return movimientoDAO.save(movimiento);
    }
}