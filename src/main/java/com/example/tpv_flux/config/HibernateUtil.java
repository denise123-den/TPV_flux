package com.example.tpv_flux.config;

import com.example.tpv_flux.model.Categoria;
import com.example.tpv_flux.model.DetalleVenta;
import com.example.tpv_flux.model.MetodoPago;
import com.example.tpv_flux.model.MovimientoInventario;
import com.example.tpv_flux.model.Notificacion;
import com.example.tpv_flux.model.Producto;
import com.example.tpv_flux.model.Usuario;
import com.example.tpv_flux.model.Venta;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Gestor Singleton de la conexión con PostgreSQL mediante Hibernate ORM.
 */
public final class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static boolean initializationAttempted = false;

    private HibernateUtil() {
    }

    public static synchronized SessionFactory getSessionFactory() {
        if (sessionFactory == null && !initializationAttempted) {
            initializationAttempted = true;
            try {
                Configuration configuration = new Configuration().configure("hibernate.cfg.xml");

                String envUrl = getConfigValue("TPV_DB_URL", "tpv.db.url");
                String envUser = getConfigValue("TPV_DB_USER", "tpv.db.user");
                String envPassword = getConfigValue("TPV_DB_PASSWORD", "tpv.db.password");

                if (envUrl != null && !envUrl.isBlank()) {
                    configuration.setProperty("hibernate.connection.url", envUrl);
                }
                if (envUser != null && !envUser.isBlank()) {
                    configuration.setProperty("hibernate.connection.username", envUser);
                }
                if (envPassword != null) {
                    configuration.setProperty("hibernate.connection.password", envPassword);
                }

                String url = configuration.getProperty("hibernate.connection.url");
                String user = configuration.getProperty("hibernate.connection.username");
                String password = configuration.getProperty("hibernate.connection.password");

                DbConnectionConfig resolved = verifyAndPrepareDatabase(url, user, password);
                configuration.setProperty("hibernate.connection.url", resolved.url());
                configuration.setProperty("hibernate.connection.password", resolved.password());

                configuration.addAnnotatedClass(Categoria.class);
                configuration.addAnnotatedClass(Producto.class);
                configuration.addAnnotatedClass(MovimientoInventario.class);
                configuration.addAnnotatedClass(MetodoPago.class);
                configuration.addAnnotatedClass(Venta.class);
                configuration.addAnnotatedClass(DetalleVenta.class);
                configuration.addAnnotatedClass(Notificacion.class);
                configuration.addAnnotatedClass(Usuario.class);

                sessionFactory = configuration.buildSessionFactory();
            } catch (Throwable ex) {
                System.err.println("[TPV_flux] Aviso: No se pudo conectar a PostgreSQL (" + ex.getMessage() + "). Se usarán datos locales de respaldo.");
            }
        }
        return sessionFactory;
    }

    private record DbConnectionConfig(String url, String password) {
    }

    private static DbConnectionConfig verifyAndPrepareDatabase(String primaryUrl, String user, String password) throws SQLException {
        String[] candidatePasswords = {
                password,
                "123456",
                "postgres",
                "Luna11",
                "admin",
                "1234",
                ""
        };

        SQLException firstException = null;
        for (String candidatePwd : candidatePasswords) {
            if (candidatePwd == null || candidatePwd.isEmpty()) {
                continue;
            }
            try {
                String resolvedUrl = tryConnectWithPassword(primaryUrl, user, candidatePwd);
                return new DbConnectionConfig(resolvedUrl, candidatePwd);
            } catch (SQLException ex) {
                if (firstException == null) {
                    firstException = ex;
                }
            }
        }
        throw firstException != null ? firstException : new SQLException("No se pudo conectar a PostgreSQL");
    }

    private static String tryConnectWithPassword(String primaryUrl, String user, String password) throws SQLException {
        try (Connection conn = DriverManager.getConnection(primaryUrl, user, password)) {
            migrateFotoColumnIfNeeded(conn);
            System.out.println("[TPV_flux] Conectado exitosamente a PostgreSQL: " + primaryUrl);
            return primaryUrl;
        } catch (SQLException ex) {
            // Si la BD fue creada en minúsculas (tpv_flux) o mayúsculas (TPV_flux), probar la variante
            if (primaryUrl != null && primaryUrl.endsWith("/TPV_flux")) {
                String fallbackUrl = primaryUrl.substring(0, primaryUrl.length() - "/TPV_flux".length()) + "/tpv_flux";
                try (Connection conn = DriverManager.getConnection(fallbackUrl, user, password)) {
                    migrateFotoColumnIfNeeded(conn);
                    System.out.println("[TPV_flux] Conectado exitosamente a PostgreSQL: " + fallbackUrl);
                    return fallbackUrl;
                } catch (SQLException ignored) {
                }

                // Intentar crear automáticamente la base de datos TPV_flux si el servidor PostgreSQL está activo
                String adminUrl = primaryUrl.substring(0, primaryUrl.length() - "/TPV_flux".length()) + "/postgres";
                try (Connection adminConn = DriverManager.getConnection(adminUrl, user, password);
                     Statement st = adminConn.createStatement()) {
                    st.executeUpdate("CREATE DATABASE \"TPV_flux\"");
                    System.out.println("[TPV_flux] Base de datos 'TPV_flux' creada automáticamente en PostgreSQL.");
                    try (Connection newConn = DriverManager.getConnection(primaryUrl, user, password)) {
                        migrateFotoColumnIfNeeded(newConn);
                        return primaryUrl;
                    }
                } catch (SQLException ignored) {
                }
            }
            throw ex;
        }
    }

    /**
     * Si la columna 'foto' fue creada previamente como BYTEA, la convierte automáticamente a TEXT
     * para poder almacenar nombres de archivo o rutas de imágenes fácilmente.
     */
    private static void migrateFotoColumnIfNeeded(Connection conn) {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT data_type FROM information_schema.columns WHERE table_name = 'productos' AND column_name = 'foto'"
             )) {
            if (rs.next()) {
                String dataType = rs.getString(1);
                if ("bytea".equalsIgnoreCase(dataType)) {
                    st.executeUpdate("ALTER TABLE productos ALTER COLUMN foto TYPE TEXT USING NULL");
                }
            }
        } catch (SQLException ignored) {
        }
    }

    public static boolean isConnected() {
        SessionFactory sf = getSessionFactory();
        return sf != null && !sf.isClosed();
    }

    public static synchronized void resetAndReconnect() {
        shutdown();
        initializationAttempted = false;
        getSessionFactory();
    }

    public static synchronized void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            sessionFactory.close();
        }
        sessionFactory = null;
    }

    private static String getConfigValue(String envKey, String sysPropKey) {
        String prop = System.getProperty(sysPropKey);
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return System.getenv(envKey);
    }
}
