package Clases;

public class Usuario {
    private int idUsuario;
    private String nombre;
    private String contrasena;
    private int rol; // 0=Administrador, 1=Encargado

    // Constructor con todo
    public Usuario(int idUsuario, String nombre, String contrasena, int rol) {
        this.idUsuario = idUsuario;
        this.nombre = nombre;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    // Constructor sin id (para nuevo usuario)
    public Usuario(String nombre, String contrasena, int rol) {
        this.nombre = nombre;
        this.contrasena = contrasena;
        this.rol = rol;
    }

    // Getters y setters
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    public int getRol() { return rol; }
    public void setRol(int rol) { this.rol = rol; }

    public boolean esAdministrador() {
        return rol == 0;
    }
}