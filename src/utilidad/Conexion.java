package utilidad;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.*;
import java.util.Properties;

public class Conexion {
    private static final String CONFIG_FILE = "db.properties";
    private static String url = "jdbc:postgresql://localhost:5432/";   // valor por defecto
    private static String usuario = "postgres";
    private static String clave = "";

    static {
        cargarConfiguracion();
    }

    private static void cargarConfiguracion() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(CONFIG_FILE)) {
            props.load(input);
            url = props.getProperty("db.url", url);
            usuario = props.getProperty("db.usuario", usuario);
            clave = props.getProperty("db.clave", clave);
        } catch (IOException e) {
            // No existe el archivo, se queda con los valores por defecto
        }
    }

    private static void guardarConfiguracion() throws IOException {
        Properties props = new Properties();
        props.setProperty("db.url", url);
        props.setProperty("db.usuario", usuario);
        props.setProperty("db.clave", clave);
        try (OutputStream output = new FileOutputStream(CONFIG_FILE)) {
            props.store(output, "Configuración de base de datos");
        }
    }

    // Prueba de conexión con parámetros dados (sin cambiar la configuración actual)
    public static boolean probarConexion(String testUrl, String testUser, String testPass) {
        try (Connection conn = DriverManager.getConnection(testUrl, testUser, testPass)) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    // Actualiza la configuración persistente y la guarda
    public static void actualizarConfiguracion(String nuevaUrl, String nuevoUsuario, String nuevaClave) throws IOException {
        url = nuevaUrl;
        usuario = nuevoUsuario;
        clave = nuevaClave;
        guardarConfiguracion();
    }

    // Devuelve la conexión usando la configuración actual
    public static Connection get() throws SQLException {
        return DriverManager.getConnection(url, usuario, clave);
    }

    // Métodos para obtener los valores actuales (útiles para la ventana)
    public static String getUrl() { return url; }
    public static String getUsuario() { return usuario; }
    public static String getClave() { return clave; }
}