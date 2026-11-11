package com.example.tpv_flux.dao;

import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.model.Notificacion;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;

import java.util.Collections;
import java.util.List;

public class NotificacionDao {

    public List<Notificacion> findAll() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "FROM Notificacion n ORDER BY n.fechaCreacion DESC",
                    Notificacion.class
            ).getResultList();
        }
    }

    public List<Notificacion> findUnread() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return Collections.emptyList();
        }
        try (Session session = sf.openSession()) {
            return session.createQuery(
                    "FROM Notificacion n WHERE n.leida = false ORDER BY n.fechaCreacion DESC",
                    Notificacion.class
            ).getResultList();
        }
    }

    public long countUnread() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return 0L;
        }
        try (Session session = sf.openSession()) {
            Long count = session.createQuery(
                    "SELECT COUNT(n) FROM Notificacion n WHERE n.leida = false",
                    Long.class
            ).uniqueResult();
            return count != null ? count : 0L;
        }
    }

    public void markAllAsRead() {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null) {
            return;
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.createMutationQuery("UPDATE Notificacion n SET n.leida = true WHERE n.leida = false")
                        .executeUpdate();
                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception ignored) {
                    }
                }
                throw e;
            }
        }
    }

    public void markAsRead(Integer id) {
        SessionFactory sf = HibernateUtil.getSessionFactory();
        if (sf == null || id == null) {
            return;
        }
        try (Session session = sf.openSession()) {
            Transaction tx = null;
            try {
                tx = session.beginTransaction();
                session.createMutationQuery("UPDATE Notificacion n SET n.leida = true WHERE n.id = :id")
                        .setParameter("id", id)
                        .executeUpdate();
                tx.commit();
            } catch (Exception e) {
                if (tx != null && tx.isActive()) {
                    try {
                        tx.rollback();
                    } catch (Exception ignored) {
                    }
                }
                throw e;
            }
        }
    }
}
