package com.cdc.fin.presupuesto.repository;

import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface SolicitudPresupuestoRepositoryCustom {
    Page<SolicitudPresupuesto> findByDynamicFilters(Map<String, String> filters, Pageable pageable);
}
