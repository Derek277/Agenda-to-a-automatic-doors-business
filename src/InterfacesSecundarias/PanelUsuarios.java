package InterfacesSecundarias;

import Clases.Usuario;
import org.mindrot.jbcrypt.BCrypt;
import utilidad.Conexion;
import utilidad.Estilos;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.sql.*;

/**
 * Diálogo de gestión de usuarios.
 * Solo accesible para administradores (PanelGeneral lo controla).
 *
 * Funciones:
 * - Listar usuarios con su cargo
 * - Crear usuario con contraseña hasheada con BCrypt
 * - Editar nombre y cargo (la contraseña se cambia solo si se llena el campo)
 * - Eliminar usuario (no se puede eliminar el propio usuario ni al último admin)
 */
public class PanelUsuarios extends JDialog {

    private final Usuario usuarioActual;

    // Tabla
    private DefaultTableModel modeloTabla;
    private JTable            tablaUsuarios;

    // Formulario
    private JTextField     txtNombre;
    private JPasswordField txtContrasena;
    private JPasswordField txtConfirmar;
    private JComboBox<String> cmbCargo;

    // Botones
    private JButton btnNuevo, btnEditar, btnGuardar, btnCancelar, btnEliminar;

    private boolean editando         = false;
    private int     idEditando       = -1;

    public PanelUsuarios(Usuario usuarioActual, JFrame owner) {
        super(owner, "Gestión de Usuarios", true);
        this.usuarioActual = usuarioActual;
        initComponents();
        cargarTabla();
        habilitarFormulario(false);
        actualizarBotones();
        setSize(820, 560);
        setLocationRelativeTo(owner);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));
        getRootPane().setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // ── Tabla ─────────────────────────────────────────────────────────────
        modeloTabla = new DefaultTableModel(
                new String[]{"ID", "Nombre de usuario", "Cargo"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaUsuarios = new JTable(modeloTabla);
        Estilos.estilizarTabla(tablaUsuarios);
        tablaUsuarios.getColumnModel().getColumn(0).setMaxWidth(50);
        tablaUsuarios.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !editando && tablaUsuarios.getSelectedRow() != -1) {
                cargarFormulario();
            }
        });
        
        // Doble clic para editar directamente
tablaUsuarios.addMouseListener(new java.awt.event.MouseAdapter() {
    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        if (e.getClickCount() == 2 && tablaUsuarios.getSelectedRow() != -1) {
            iniciarEdicion();
        }
    }
});
        
        JPanel panelTabla = new JPanel(new BorderLayout());
        panelTabla.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Estilos.CAMPO_BORDE), "Usuarios registrados"));
        panelTabla.add(Estilos.scrollParaTabla(tablaUsuarios), BorderLayout.CENTER);

        // ── Formulario ────────────────────────────────────────────────────────
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(Estilos.CAMPO_BORDE), "Datos del usuario"));
        formPanel.setPreferredSize(new Dimension(320, 0));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets  = new Insets(8, 10, 8, 10);
        gbc.fill    = GridBagConstraints.HORIZONTAL;
        gbc.anchor  = GridBagConstraints.WEST;

        int y = 0;

        // Nombre
        addLabel(formPanel, gbc, "Nombre de usuario:", y++);
        txtNombre = new JTextField(20);
        Estilos.estilizarCampo(txtNombre);
        addField(formPanel, gbc, txtNombre, y++);

        // Contraseña
        addLabel(formPanel, gbc,
                "<html>Contraseña:<br><small style='color:gray'>(dejar vacío para no cambiar)</small></html>", y++);
        txtContrasena = new JPasswordField(20);
        Estilos.estilizarCampo(txtContrasena);
        addField(formPanel, gbc, txtContrasena, y++);

        // Confirmar
        addLabel(formPanel, gbc, "Confirmar contraseña:", y++);
        txtConfirmar = new JPasswordField(20);
        Estilos.estilizarCampo(txtConfirmar);
        addField(formPanel, gbc, txtConfirmar, y++);

        // Cargo
        addLabel(formPanel, gbc, "Cargo:", y++);
        cmbCargo = new JComboBox<>(new String[]{"Administrador", "Encargado"});
        gbc.gridx = 0; gbc.gridy = y++; gbc.gridwidth = 2;
        formPanel.add(cmbCargo, gbc);

        // Botones CRUD
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
        gbc.insets = new Insets(16, 10, 4, 10);
        JPanel botones = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));

        btnNuevo   = new JButton("Nuevo");
        btnEditar  = new JButton("Editar");
        btnGuardar = new JButton("Guardar");
        btnCancelar= new JButton("Cancelar");
        btnEliminar= new JButton("Eliminar");

        Estilos.estilizarBoton(btnNuevo, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditar,   Estilos.BOTON_SECUNDARIO, Estilos.TEXTO_PRINCIPAL);
        Estilos.estilizarBoton(btnGuardar,  Estilos.AMARILLO,         Color.BLACK);
        Estilos.estilizarBoton(btnCancelar, Estilos.BOTON_SECUNDARIO, Estilos.TEXTO_PRINCIPAL);
        Estilos.estilizarBoton(btnEliminar, Estilos.ROJO_PELIGRO,     Color.WHITE);

        btnNuevo.addActionListener(e -> iniciarNuevo());
        btnEditar.addActionListener(e -> iniciarEdicion());
        btnGuardar.addActionListener(e -> guardar());
        btnCancelar.addActionListener(e -> cancelar());
        btnEliminar.addActionListener(e -> eliminar());

        botones.add(btnNuevo);
        botones.add(btnEditar);
        botones.add(btnGuardar);
        botones.add(btnCancelar);
        botones.add(btnEliminar);

        formPanel.add(botones, gbc);

        // Layout principal
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelTabla, formPanel);
        split.setResizeWeight(0.6);
        split.setDividerSize(6);
        add(split, BorderLayout.CENTER);
    }

    // ── Helpers de layout ────────────────────────────────────────────────────

    private void addLabel(JPanel p, GridBagConstraints gbc, String texto, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
        JLabel lbl = new JLabel("<html>" + texto + "</html>");
        lbl.setFont(new Font("SansSerif", Font.PLAIN, 13));
        p.add(lbl, gbc);
    }

    private void addField(JPanel p, GridBagConstraints gbc, JComponent field, int y) {
        gbc.gridx = 0; gbc.gridy = y; gbc.gridwidth = 2;
        p.add(field, gbc);
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    private void cargarTabla() {
    modeloTabla.setRowCount(0);
    String sql = "SELECT u.id_usuario, u.nombre, c.nombre AS cargo " +
                 "FROM usuario u JOIN cargo c ON u.id_cargo = c.id_cargo " +
                 "ORDER BY u.id_usuario";
    try (Connection conn = Conexion.get();
         Statement st = conn.createStatement();
         ResultSet rs = st.executeQuery(sql)) {
        while (rs.next()) {
            modeloTabla.addRow(new Object[]{
                rs.getInt("id_usuario"),
                rs.getString("nombre"),
                rs.getString("cargo")
            });
        }
    } catch (SQLException e) { e.printStackTrace(); }

    // Seleccionar automáticamente la primera fila si hay datos
    if (modeloTabla.getRowCount() > 0) {
        tablaUsuarios.setRowSelectionInterval(0, 0);
        cargarFormulario(); // rellena el formulario con ese usuario
    }
    actualizarBotones(); // para habilitar/deshabilitar correctamente
}

    private void cargarFormulario() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) return;
        idEditando = (int) modeloTabla.getValueAt(row, 0);
        txtNombre.setText((String) modeloTabla.getValueAt(row, 1));
        txtContrasena.setText("");
        txtConfirmar.setText("");
        String cargo = (String) modeloTabla.getValueAt(row, 2);
        cmbCargo.setSelectedItem(cargo.equals("Administrador") ? "Administrador" : "Encargado");
    }

    // ── Acciones CRUD ─────────────────────────────────────────────────────────

    private void iniciarNuevo() {
        editando   = true;
        idEditando = -1;
        txtNombre.setText("");
        txtContrasena.setText("");
        txtConfirmar.setText("");
        cmbCargo.setSelectedIndex(1); // Encargado por defecto
        habilitarFormulario(true);
        actualizarBotones();
        txtNombre.requestFocus();
    }

    private void iniciarEdicion() {
        if (tablaUsuarios.getSelectedRow() == -1) return;
        editando = true;
        habilitarFormulario(true);
        actualizarBotones();
    }

    private void guardar() {
        String nombre = txtNombre.getText().trim();
        String pass   = new String(txtContrasena.getPassword());
        String conf   = new String(txtConfirmar.getPassword());

        if (nombre.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre de usuario es obligatorio.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Si es nuevo SIEMPRE requiere contraseña
        if (idEditando == -1 && pass.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Debe establecer una contraseña para el nuevo usuario.",
                    "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Si se ingresó contraseña, verificar que coincidan
        if (!pass.isEmpty()) {
            if (!pass.equals(conf)) {
                JOptionPane.showMessageDialog(this, "Las contraseñas no coinciden.",
                        "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (pass.length() < 6) {
                JOptionPane.showMessageDialog(this, "La contraseña debe tener al menos 6 caracteres.",
                        "Validación", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int idCargo = cmbCargo.getSelectedIndex() == 0 ? 1 : 2; // 1=Admin, 2=Encargado

        try (Connection conn = Conexion.get()) {
            if (idEditando == -1) {
                // INSERT
                String hash = BCrypt.hashpw(pass, BCrypt.gensalt(12));
                String sql = "INSERT INTO usuario (nombre, contrasenia, id_cargo) VALUES (?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, nombre);
                    ps.setString(2, hash);
                    ps.setInt(3, idCargo);
                    ps.executeUpdate();
                }
                JOptionPane.showMessageDialog(this, "Usuario creado correctamente.");
            } else {
                // UPDATE
                if (!pass.isEmpty()) {
                    // Cambiar contraseña también
                    String hash = BCrypt.hashpw(pass, BCrypt.gensalt(12));
                    String sql = "UPDATE usuario SET nombre=?, contrasenia=?, id_cargo=? WHERE id_usuario=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nombre);
                        ps.setString(2, hash);
                        ps.setInt(3, idCargo);
                        ps.setInt(4, idEditando);
                        ps.executeUpdate();
                    }
                } else {
                    // Solo nombre y cargo
                    String sql = "UPDATE usuario SET nombre=?, id_cargo=? WHERE id_usuario=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nombre);
                        ps.setInt(2, idCargo);
                        ps.setInt(3, idEditando);
                        ps.executeUpdate();
                    }
                }
                JOptionPane.showMessageDialog(this, "Usuario actualizado correctamente.");
            }
            cancelar();
            cargarTabla();

        } catch (SQLException e) {
            if (e.getMessage().contains("unique") || e.getMessage().contains("duplicado")) {
                JOptionPane.showMessageDialog(this, "Ya existe un usuario con ese nombre.",
                        "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void eliminar() {
        int row = tablaUsuarios.getSelectedRow();
        if (row == -1) return;

        int idSeleccionado = (int) modeloTabla.getValueAt(row, 0);

        // No puede eliminar su propio usuario
        if (idSeleccionado == usuarioActual.getIdUsuario()) {
            JOptionPane.showMessageDialog(this, "No puede eliminar su propio usuario.",
                    "Operación no permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Verificar que no sea el último admin
        String cargo = (String) modeloTabla.getValueAt(row, 2);
        if ("Administrador".equals(cargo)) {
            long admins = 0;
            for (int i = 0; i < modeloTabla.getRowCount(); i++) {
                if ("Administrador".equals(modeloTabla.getValueAt(i, 2))) admins++;
            }
            if (admins <= 1) {
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar el único administrador del sistema.",
                        "Operación no permitida", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar el usuario \"" + modeloTabla.getValueAt(row, 1) + "\"?",
                "Confirmar eliminación", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM usuario WHERE id_usuario = ?")) {
                ps.setInt(1, idSeleccionado);
                ps.executeUpdate();
                cargarTabla();
                limpiarFormulario();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this,
                        "Error al eliminar: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void cancelar() {
        editando = false;
        habilitarFormulario(false);
        actualizarBotones();
        if (tablaUsuarios.getSelectedRow() != -1) cargarFormulario();
        else limpiarFormulario();
    }

    private void limpiarFormulario() {
        txtNombre.setText("");
        txtContrasena.setText("");
        txtConfirmar.setText("");
        cmbCargo.setSelectedIndex(1);
        idEditando = -1;
    }

    private void habilitarFormulario(boolean hab) {
        txtNombre.setEditable(hab);
        txtContrasena.setEditable(hab);
        txtConfirmar.setEditable(hab);
        cmbCargo.setEnabled(hab);
    }

    private void actualizarBotones() {
        boolean haySeleccion = tablaUsuarios.getSelectedRow() != -1;
        btnNuevo.setEnabled(!editando);
        btnEditar.setEnabled(!editando && haySeleccion);
        btnGuardar.setVisible(editando);
        btnCancelar.setVisible(editando);
        btnEliminar.setEnabled(!editando && haySeleccion);
    }
}
