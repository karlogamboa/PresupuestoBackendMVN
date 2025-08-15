package com.cdc.fin.presupuesto.repository.impl;

import com.cdc.fin.presupuesto.model.SolicitudPresupuesto;
import com.cdc.fin.presupuesto.repository.SolicitudPresupuestoRepositoryCustom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class SolicitudPresupuestoRepositoryImpl implements SolicitudPresupuestoRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    public Page<SolicitudPresupuesto> findByDynamicFilters(Map<String, String> filters, Pageable pageable) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<SolicitudPresupuesto> cq = cb.createQuery(SolicitudPresupuesto.class);
        Root<SolicitudPresupuesto> root = cq.from(SolicitudPresupuesto.class);

        List<Predicate> predicates = new ArrayList<>();
        filters.forEach((key, value) -> {
            if (value != null && !value.isEmpty()) {
                predicates.add(cb.equal(root.get(key), value));
            }
        });

        cq.where(predicates.toArray(new Predicate[0]));
        cq.orderBy(cb.desc(root.get("fechaCreacion")));

        TypedQuery<SolicitudPresupuesto> query = em.createQuery(cq);
        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());
        List<SolicitudPresupuesto> resultList = query.getResultList();

        // Count query
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<SolicitudPresupuesto> countRoot = countQuery.from(SolicitudPresupuesto.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long total = em.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(resultList, pageable, total);
    }
    // ...other methods...
}
