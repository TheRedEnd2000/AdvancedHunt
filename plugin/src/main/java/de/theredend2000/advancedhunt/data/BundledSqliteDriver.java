package de.theredend2000.advancedhunt.data;

import org.bukkit.plugin.java.JavaPlugin;

import javax.sql.DataSource;
import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

final class BundledSqliteDriver implements AutoCloseable {

    static final String DRIVER_CLASS_NAME = "org.sqlite.JDBC";
    private static final String DRIVER_JAR_RESOURCE = "META-INF/advancedhunt/sqlite-jdbc.jar";
    private static final Logger LOGGER = Logger.getLogger("AdvancedHunt.SQLite");

    private final Logger pluginLogger;
    private final File extractedDriverJar;
    private final URLClassLoader classLoader;
    private final DataSource dataSource;

    private BundledSqliteDriver(
            Logger pluginLogger,
            File extractedDriverJar,
            URLClassLoader classLoader,
            DataSource dataSource
    ) {
        this.pluginLogger = pluginLogger;
        this.extractedDriverJar = extractedDriverJar;
        this.classLoader = classLoader;
        this.dataSource = dataSource;
    }

    static BundledSqliteDriver load(JavaPlugin plugin, String jdbcUrl) {
        File driverJar = null;
        URLClassLoader classLoader = null;
        try (InputStream resource = plugin.getResource(DRIVER_JAR_RESOURCE)) {
            if (resource == null) {
                return null;
            }

            driverJar = File.createTempFile("advancedhunt-sqlite-", ".jar");
            driverJar.deleteOnExit();
            try (OutputStream output = Files.newOutputStream(driverJar.toPath())) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = resource.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            classLoader = new ChildFirstSqliteClassLoader(
                    driverJar.toURI().toURL(),
                    SqlRepository.class.getClassLoader()
            );
            Class<?> driverClass = Class.forName(DRIVER_CLASS_NAME, true, classLoader);
            Driver driver = (Driver) driverClass.getDeclaredConstructor().newInstance();
            DataSource dataSource = new SqliteDriverDataSource(driver, jdbcUrl);

            plugin.getLogger().info("Using the bundled SQLite JDBC driver in an isolated classloader.");
            return new BundledSqliteDriver(plugin.getLogger(), driverJar, classLoader, dataSource);
        } catch (IOException | ReflectiveOperationException | ClassCastException e) {
            closeAfterFailedLoad(classLoader, driverJar, e);
            throw new IllegalStateException("Failed to load the bundled SQLite JDBC driver", e);
        }
    }

    DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public void close() {
        try {
            classLoader.close();
        } catch (IOException e) {
            pluginLogger.log(Level.WARNING, "Failed to close the bundled SQLite driver classloader", e);
        }

        if (extractedDriverJar.exists() && !extractedDriverJar.delete()) {
            extractedDriverJar.deleteOnExit();
            pluginLogger.warning("Failed to delete the extracted bundled SQLite JDBC jar: "
                    + extractedDriverJar.getAbsolutePath());
        }
    }

    private static void closeAfterFailedLoad(
            URLClassLoader classLoader,
            File driverJar,
            Exception originalException
    ) {
        if (classLoader != null) {
            try {
                classLoader.close();
            } catch (IOException closeException) {
                originalException.addSuppressed(closeException);
            }
        }
        if (driverJar != null && driverJar.exists() && !driverJar.delete()) {
            driverJar.deleteOnExit();
        }
    }

    private static final class ChildFirstSqliteClassLoader extends URLClassLoader {
        private ChildFirstSqliteClassLoader(URL driverJar, ClassLoader parent) {
            super(new URL[]{driverJar}, parent);
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("org.sqlite.")) {
                return super.loadClass(name, resolve);
            }

            synchronized (getClassLoadingLock(name)) {
                Class<?> loadedClass = findLoadedClass(name);
                if (loadedClass == null) {
                    try {
                        loadedClass = findClass(name);
                    } catch (ClassNotFoundException e) {
                        loadedClass = super.loadClass(name, false);
                    }
                }
                if (resolve) {
                    resolveClass(loadedClass);
                }
                return loadedClass;
            }
        }

        @Override
        public URL getResource(String name) {
            String resourceName = name.startsWith("/") ? name.substring(1) : name;
            if (isSqliteResource(resourceName)) {
                URL resource = findResource(resourceName);
                if (resource != null) {
                    return resource;
                }
            }
            return super.getResource(resourceName);
        }

        private boolean isSqliteResource(String name) {
            return name.startsWith("org/sqlite/")
                    || name.startsWith("META-INF/maven/org.xerial/sqlite-jdbc/")
                    || "META-INF/services/java.sql.Driver".equals(name);
        }
    }

    private static final class SqliteDriverDataSource implements DataSource {
        private final Driver driver;
        private final String jdbcUrl;
        private volatile PrintWriter logWriter;
        private volatile int loginTimeout;

        private SqliteDriverDataSource(Driver driver, String jdbcUrl) {
            this.driver = driver;
            this.jdbcUrl = jdbcUrl;
        }

        @Override
        public Connection getConnection() throws SQLException {
            return connect(new Properties());
        }

        @Override
        public Connection getConnection(String username, String password) throws SQLException {
            Properties properties = new Properties();
            if (username != null) {
                properties.setProperty("user", username);
            }
            if (password != null) {
                properties.setProperty("password", password);
            }
            return connect(properties);
        }

        private Connection connect(Properties properties) throws SQLException {
            Connection connection = driver.connect(jdbcUrl, properties);
            if (connection == null) {
                throw new SQLException("The bundled SQLite JDBC driver rejected the configured URL");
            }
            return connection;
        }

        @Override
        public PrintWriter getLogWriter() {
            return logWriter;
        }

        @Override
        public void setLogWriter(PrintWriter out) {
            logWriter = out;
        }

        @Override
        public void setLoginTimeout(int seconds) {
            loginTimeout = seconds;
        }

        @Override
        public int getLoginTimeout() {
            return loginTimeout;
        }

        @Override
        public Logger getParentLogger() {
            return LOGGER;
        }

        @Override
        public <T> T unwrap(Class<T> iface) throws SQLException {
            if (iface.isInstance(this)) {
                return iface.cast(this);
            }
            if (iface.isInstance(driver)) {
                return iface.cast(driver);
            }
            throw new SQLException("Not a wrapper for " + iface.getName());
        }

        @Override
        public boolean isWrapperFor(Class<?> iface) {
            return iface.isInstance(this) || iface.isInstance(driver);
        }
    }
}
