package InterfacesPrincipales;

import utilidad.Conexion;
import utilidad.Estilos;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel de catálogos: Tipos de Motor y Tipos de Servicio.
 * 
 * - Sin filtros, tablas ordenadas alfabéticamente por nombre.
 * - Botones para añadir, editar y eliminar en cada tabla.
 * - No se muestran IDs; los valores booleanos de garantía aparecen como "Sí"/"No".
 */
public class PanelCatalogos extends JPanel {

    // Tipos de Puerta
    private JTable tablaTiposPuerta;
    private DefaultTableModel modelTiposPuerta;
    private JButton btnNuevoTipoPuerta, btnEditarTipoPuerta, btnEliminarTipoPuerta;
    private List<Integer> idsTiposPuerta = new ArrayList<>();

    // Motores
    private JTable tablaMotores;
    private DefaultTableModel modelMotores;
    private JButton btnNuevoMotor, btnEditarMotor, btnEliminarMotor;
    private List<Integer> idsMotores = new ArrayList<>();

    // Servicios
    private JTable tablaServicios;
    private DefaultTableModel modelServicios;
    private JButton btnNuevoServicio, btnEditarServicio, btnEliminarServicio;
    private List<Integer> idsServicios = new ArrayList<>();

    // SQLState estándar de PostgreSQL para violación de llave foránea
    private static final String SQLSTATE_FOREIGN_KEY_VIOLATION = "23503";

    /**
     * Detecta si un SQLException corresponde a una violación de llave foránea
     * (incluye el caso de restricciones ON DELETE RESTRICT). Se revisa primero
     * el SQLState estándar y, como respaldo, el texto del mensaje, por si el
     * SQLState no llegara a propagarse correctamente.
     */
    private static boolean esViolacionLlaveForanea(SQLException e) {
        if (SQLSTATE_FOREIGN_KEY_VIOLATION.equals(e.getSQLState())) return true;
        String msg = e.getMessage();
        if (msg == null) return false;
        String m = msg.toLowerCase();
        return m.contains("foreign key") && (m.contains("violat") || m.contains("restrict"));
    }

    public PanelCatalogos() {
        setLayout(new GridLayout(1, 3, 15, 15));
        setBorder(new EmptyBorder(15, 15, 15, 15));

        add(buildPanelTiposPuerta());
        add(buildPanelMotores());
        add(buildPanelServicios());

        cargarTiposPuerta();
        cargarMotores();
        cargarServicios();
    }

    // ────────────────────────────────────────────────────────────────────────
    // Panel de Tipos de Puerta (mismo patrón que Tipos de Motor)
    // ────────────────────────────────────────────────────────────────────────
    private JPanel buildPanelTiposPuerta() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), "Tipos de Puerta"));
        panel.setBackground(Color.WHITE);

        modelTiposPuerta = new DefaultTableModel(new String[]{"Nombre"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaTiposPuerta = new JTable(modelTiposPuerta);
        tablaTiposPuerta.setRowHeight(30);
        tablaTiposPuerta.setDefaultRenderer(Object.class, new RendererCatalogo());
        Estilos.estilizarTabla(tablaTiposPuerta);
        tablaTiposPuerta.getSelectionModel().addListSelectionListener(e -> actualizarBotonesTipoPuerta());

        JScrollPane scroll = Estilos.scrollParaTabla(tablaTiposPuerta);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);

        btnNuevoTipoPuerta = new JButton("+ Nuevo");
        btnEditarTipoPuerta = new JButton("Editar");
        btnEliminarTipoPuerta = new JButton("Eliminar");

        Estilos.estilizarBoton(btnNuevoTipoPuerta, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditarTipoPuerta, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        Estilos.estilizarBoton(btnEliminarTipoPuerta, Estilos.ROJO_PELIGRO, Color.WHITE);

        btnNuevoTipoPuerta.addActionListener(e -> abrirDialogoTipoPuerta(null));
        btnEditarTipoPuerta.addActionListener(e -> abrirDialogoTipoPuerta(obtenerIdTipoPuertaSeleccionado()));
        btnEliminarTipoPuerta.addActionListener(e -> eliminarTipoPuerta());

        toolbar.add(btnNuevoTipoPuerta);
        toolbar.add(btnEditarTipoPuerta);
        toolbar.add(btnEliminarTipoPuerta);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.SOUTH);

        actualizarBotonesTipoPuerta();
        return panel;
    }

    private void cargarTiposPuerta() {
        modelTiposPuerta.setRowCount(0);
        idsTiposPuerta.clear();
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id_tipo_puerta, nombre FROM tipo_puerta ORDER BY nombre")) {
            while (rs.next()) {
                modelTiposPuerta.addRow(new Object[]{rs.getString("nombre")});
                idsTiposPuerta.add(rs.getInt("id_tipo_puerta"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Integer obtenerIdTipoPuertaSeleccionado() {
        int row = tablaTiposPuerta.getSelectedRow();
        return (row != -1 && row < idsTiposPuerta.size()) ? idsTiposPuerta.get(row) : null;
    }

    private void actualizarBotonesTipoPuerta() {
        boolean sel = tablaTiposPuerta.getSelectedRow() != -1;
        btnEditarTipoPuerta.setEnabled(sel);
        btnEliminarTipoPuerta.setEnabled(sel);
    }

    private void abrirDialogoTipoPuerta(Integer idTipoPuerta) {
        String nombreActual = "";
        if (idTipoPuerta != null) {
            int row = idsTiposPuerta.indexOf(idTipoPuerta);
            if (row >= 0) nombreActual = (String) modelTiposPuerta.getValueAt(row, 0);
        }
        DialogoCatalogo dlg = new DialogoCatalogo(SwingUtilities.getWindowAncestor(this),
                idTipoPuerta == null ? "Nuevo tipo de puerta" : "Editar tipo de puerta",
                "Nombre del tipo de puerta:", nombreActual, false);
        dlg.setVisible(true);
        if (dlg.guardadoExitoso) {
            guardarTipoPuerta(idTipoPuerta, dlg.texto.trim());
            cargarTiposPuerta();
        }
    }

    private void guardarTipoPuerta(Integer idTipoPuerta, String nombre) {
        try (Connection conn = Conexion.get()) {
            if (idTipoPuerta == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tipo_puerta (nombre) VALUES (?)")) {
                    ps.setString(1, nombre);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tipo_puerta SET nombre=? WHERE id_tipo_puerta=?")) {
                    ps.setString(1, nombre);
                    ps.setInt(2, idTipoPuerta);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
        }
    }

    private void eliminarTipoPuerta() {
        Integer id = obtenerIdTipoPuertaSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un tipo de puerta.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este tipo de puerta?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM tipo_puerta WHERE id_tipo_puerta = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                cargarTiposPuerta();
            } catch (SQLException e) {
                if (esViolacionLlaveForanea(e)) {
                    JOptionPane.showMessageDialog(this,
                            "No se puede eliminar: hay puertas que usan este tipo.\n"
                            + "Reasigna o elimina esas puertas primero.",
                            "No se puede eliminar", JOptionPane.WARNING_MESSAGE);
                } else {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Panel de Tipos de Motor
    // ────────────────────────────────────────────────────────────────────────
    private JPanel buildPanelMotores() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), "Tipos de Motor"));
        panel.setBackground(Color.WHITE);

        modelMotores = new DefaultTableModel(new String[]{"Nombre"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaMotores = new JTable(modelMotores);
        tablaMotores.setRowHeight(30);
        tablaMotores.setDefaultRenderer(Object.class, new RendererCatalogo());
        Estilos.estilizarTabla(tablaMotores);
        tablaMotores.getSelectionModel().addListSelectionListener(e -> actualizarBotonesMotor());

        JScrollPane scroll = Estilos.scrollParaTabla(tablaMotores);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);

        btnNuevoMotor = new JButton("+ Nuevo");
        btnEditarMotor = new JButton("Editar");
        btnEliminarMotor = new JButton("Eliminar");

        Estilos.estilizarBoton(btnNuevoMotor, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditarMotor, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        Estilos.estilizarBoton(btnEliminarMotor, Estilos.ROJO_PELIGRO, Color.WHITE);

        btnNuevoMotor.addActionListener(e -> abrirDialogoMotor(null));
        btnEditarMotor.addActionListener(e -> abrirDialogoMotor(obtenerIdMotorSeleccionado()));
        btnEliminarMotor.addActionListener(e -> eliminarMotor());

        toolbar.add(btnNuevoMotor);
        toolbar.add(btnEditarMotor);
        toolbar.add(btnEliminarMotor);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.SOUTH);

        actualizarBotonesMotor();
        return panel;
    }

    private void cargarMotores() {
        modelMotores.setRowCount(0);
        idsMotores.clear();
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id_tipo_motor, nombre FROM tipo_motor ORDER BY nombre")) {
            while (rs.next()) {
                modelMotores.addRow(new Object[]{rs.getString("nombre")});
                idsMotores.add(rs.getInt("id_tipo_motor"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Integer obtenerIdMotorSeleccionado() {
        int row = tablaMotores.getSelectedRow();
        return (row != -1 && row < idsMotores.size()) ? idsMotores.get(row) : null;
    }

    private void actualizarBotonesMotor() {
        boolean sel = tablaMotores.getSelectedRow() != -1;
        btnEditarMotor.setEnabled(sel);
        btnEliminarMotor.setEnabled(sel);
    }

    private void abrirDialogoMotor(Integer idMotor) {
        String nombreActual = "";
        if (idMotor != null) {
            int row = idsMotores.indexOf(idMotor);
            if (row >= 0) nombreActual = (String) modelMotores.getValueAt(row, 0);
        }
        DialogoCatalogo dlg = new DialogoCatalogo(SwingUtilities.getWindowAncestor(this),
                idMotor == null ? "Nuevo tipo de motor" : "Editar tipo de motor",
                "Nombre del motor:", nombreActual, false);
        dlg.setVisible(true);
        if (dlg.guardadoExitoso) {
            guardarMotor(idMotor, dlg.texto.trim());
            cargarMotores();
        }
    }

    private void guardarMotor(Integer idMotor, String nombre) {
        try (Connection conn = Conexion.get()) {
            if (idMotor == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tipo_motor (nombre) VALUES (?)")) {
                    ps.setString(1, nombre);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tipo_motor SET nombre=? WHERE id_tipo_motor=?")) {
                    ps.setString(1, nombre);
                    ps.setInt(2, idMotor);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
        }
    }

    private void eliminarMotor() {
        Integer id = obtenerIdMotorSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un tipo de motor.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este tipo de motor?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM tipo_motor WHERE id_tipo_motor = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                cargarMotores();
            } catch (SQLException e) {
                if (esViolacionLlaveForanea(e)) {
                    JOptionPane.showMessageDialog(this,
                            "No se puede eliminar: hay puertas que usan este tipo de motor.\n"
                            + "Reasigna o elimina esas puertas primero.",
                            "No se puede eliminar", JOptionPane.WARNING_MESSAGE);
                } else {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Panel de Tipos de Servicio
    // ────────────────────────────────────────────────────────────────────────
    private JPanel buildPanelServicios() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)), "Tipos de Servicio"));
        panel.setBackground(Color.WHITE);

        modelServicios = new DefaultTableModel(new String[]{"Nombre", "Garantía anual"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaServicios = new JTable(modelServicios);
        tablaServicios.setRowHeight(30);
        tablaServicios.setDefaultRenderer(Object.class, new RendererCatalogo());
        Estilos.estilizarTabla(tablaServicios);
        tablaServicios.getSelectionModel().addListSelectionListener(e -> actualizarBotonesServicio());

        JScrollPane scroll = Estilos.scrollParaTabla(tablaServicios);

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        toolbar.setBackground(Color.WHITE);

        btnNuevoServicio = new JButton("+ Nuevo");
        btnEditarServicio = new JButton("Editar");
        btnEliminarServicio = new JButton("Eliminar");

        Estilos.estilizarBoton(btnNuevoServicio, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditarServicio, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        Estilos.estilizarBoton(btnEliminarServicio, Estilos.ROJO_PELIGRO, Color.WHITE);

        btnNuevoServicio.addActionListener(e -> abrirDialogoServicio(null));
        btnEditarServicio.addActionListener(e -> abrirDialogoServicio(obtenerIdServicioSeleccionado()));
        btnEliminarServicio.addActionListener(e -> eliminarServicio());

        toolbar.add(btnNuevoServicio);
        toolbar.add(btnEditarServicio);
        toolbar.add(btnEliminarServicio);

        panel.add(scroll, BorderLayout.CENTER);
        panel.add(toolbar, BorderLayout.SOUTH);

        actualizarBotonesServicio();
        return panel;
    }

    private void cargarServicios() {
        modelServicios.setRowCount(0);
        idsServicios.clear();
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT id_tipo_servicio, nombre, garantia_anual FROM tipo_servicio ORDER BY nombre")) {
            while (rs.next()) {
                modelServicios.addRow(new Object[]{
                        rs.getString("nombre"),
                        rs.getBoolean("garantia_anual") ? "Sí" : "No"
                });
                idsServicios.add(rs.getInt("id_tipo_servicio"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private Integer obtenerIdServicioSeleccionado() {
        int row = tablaServicios.getSelectedRow();
        return (row != -1 && row < idsServicios.size()) ? idsServicios.get(row) : null;
    }

    private void actualizarBotonesServicio() {
        boolean sel = tablaServicios.getSelectedRow() != -1;
        btnEditarServicio.setEnabled(sel);
        btnEliminarServicio.setEnabled(sel);
    }

    private void abrirDialogoServicio(Integer idServicio) {
        String nombreActual = "";
        boolean garantiaActual = false;
        if (idServicio != null) {
            int row = idsServicios.indexOf(idServicio);
            if (row >= 0) {
                nombreActual = (String) modelServicios.getValueAt(row, 0);
                garantiaActual = "Sí".equals(modelServicios.getValueAt(row, 1));
            }
        }
        DialogoCatalogoConGarantia dlg = new DialogoCatalogoConGarantia(
                SwingUtilities.getWindowAncestor(this),
                idServicio == null ? "Nuevo tipo de servicio" : "Editar tipo de servicio",
                "Nombre del servicio:", nombreActual, garantiaActual);
        dlg.setVisible(true);
        if (dlg.guardadoExitoso) {
            guardarServicio(idServicio, dlg.texto.trim(), dlg.garantia);
            cargarServicios();
        }
    }

    private void guardarServicio(Integer idServicio, String nombre, boolean garantia) {
        try (Connection conn = Conexion.get()) {
            if (idServicio == null) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "INSERT INTO tipo_servicio (nombre, garantia_anual) VALUES (?, ?)")) {
                    ps.setString(1, nombre);
                    ps.setBoolean(2, garantia);
                    ps.executeUpdate();
                }
            } else {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE tipo_servicio SET nombre=?, garantia_anual=? WHERE id_tipo_servicio=?")) {
                    ps.setString(1, nombre);
                    ps.setBoolean(2, garantia);
                    ps.setInt(3, idServicio);
                    ps.executeUpdate();
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
        }
    }

    private void eliminarServicio() {
        Integer id = obtenerIdServicioSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un tipo de servicio.");
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este tipo de servicio?",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(
                         "DELETE FROM tipo_servicio WHERE id_tipo_servicio = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                cargarServicios();
            } catch (SQLException e) {
                if (esViolacionLlaveForanea(e)) {
                    JOptionPane.showMessageDialog(this,
                            "No se puede eliminar: hay citas que usan este tipo de servicio.\n"
                            + "Elimina esas citas primero.",
                            "No se puede eliminar", JOptionPane.WARNING_MESSAGE);
                } else {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
                }
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Renderer genérico para las tablas (solo alterna colores)
    // ────────────────────────────────────────────────────────────────────────
    private class RendererCatalogo extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                label.setForeground(Color.BLACK);
            } else {
                label.setBackground(Estilos.AMARILLO);
                label.setForeground(Color.BLACK);
            }
            label.setBorder(new EmptyBorder(0, 8, 0, 8));
            return label;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Diálogos reutilizables para ingresar/editar un nombre (y garantía)
    // ────────────────────────────────────────────────────────────────────────

    // Diálogo simple para motor (solo nombre)
    private static class DialogoCatalogo extends JDialog {
        boolean guardadoExitoso = false;
        String texto;
        private JTextField txtNombre;
        private JButton btnGuardar, btnCancelar;

        DialogoCatalogo(Window owner, String titulo, String labelTexto, String valorInicial, boolean mostrarGarantia) {
            super(owner, titulo, ModalityType.APPLICATION_MODAL);
            setSize(400, 180);
            setLocationRelativeTo(owner);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel(labelTexto), gbc);
            txtNombre = new JTextField(valorInicial, 20);
            Estilos.estilizarCampo(txtNombre);
            gbc.gridx = 1;
            panel.add(txtNombre, gbc);

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            botones.setBackground(Color.WHITE);
            btnGuardar = new JButton("Guardar");
            btnCancelar = new JButton("Cancelar");
            Estilos.estilizarBoton(btnGuardar, Estilos.AMARILLO, Color.BLACK);
            Estilos.estilizarBoton(btnCancelar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            btnGuardar.addActionListener(e -> {
                texto = txtNombre.getText().trim();
                if (texto.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío.");
                    return;
                }
                guardadoExitoso = true;
                dispose();
            });
            btnCancelar.addActionListener(e -> dispose());
            botones.add(btnGuardar);
            botones.add(btnCancelar);
            gbc.gridx = 0; gbc.gridy = 1; gbc.gridwidth = 2;
            panel.add(botones, gbc);

            getContentPane().add(panel);
            getRootPane().setDefaultButton(btnGuardar);
        }
    }

    // Diálogo para servicio (nombre + check garantía)
    private static class DialogoCatalogoConGarantia extends JDialog {
        boolean guardadoExitoso = false;
        String texto;
        boolean garantia;
        private JTextField txtNombre;
        private JCheckBox chkGarantia;
        private JButton btnGuardar, btnCancelar;

        DialogoCatalogoConGarantia(Window owner, String titulo, String labelTexto,
                                   String valorInicial, boolean garantiaInicial) {
            super(owner, titulo, ModalityType.APPLICATION_MODAL);
            setSize(400, 220);
            setLocationRelativeTo(owner);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);

            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            gbc.gridx = 0; gbc.gridy = 0;
            panel.add(new JLabel(labelTexto), gbc);
            txtNombre = new JTextField(valorInicial, 20);
            Estilos.estilizarCampo(txtNombre);
            gbc.gridx = 1;
            panel.add(txtNombre, gbc);

            gbc.gridx = 0; gbc.gridy = 1;
            panel.add(new JLabel("Garantía anual:"), gbc);
            chkGarantia = new JCheckBox("", garantiaInicial);
            chkGarantia.setBackground(Color.WHITE);
            gbc.gridx = 1;
            panel.add(chkGarantia, gbc);

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            botones.setBackground(Color.WHITE);
            btnGuardar = new JButton("Guardar");
            btnCancelar = new JButton("Cancelar");
            Estilos.estilizarBoton(btnGuardar, Estilos.AMARILLO, Color.BLACK);
            Estilos.estilizarBoton(btnCancelar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            btnGuardar.addActionListener(e -> {
                texto = txtNombre.getText().trim();
                if (texto.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "El nombre no puede estar vacío.");
                    return;
                }
                garantia = chkGarantia.isSelected();
                guardadoExitoso = true;
                dispose();
            });
            btnCancelar.addActionListener(e -> dispose());
            botones.add(btnGuardar);
            botones.add(btnCancelar);
            gbc.gridx = 0; gbc.gridy = 2; gbc.gridwidth = 2;
            panel.add(botones, gbc);

            getContentPane().add(panel);
            getRootPane().setDefaultButton(btnGuardar);
        }
    }
}