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
 * Panel de gestión de puertas.
 *
 * Sigue el patrón visual y funcional de PanelCitas y PanelClientes:
 * - Filtros: combo de cliente, combo de tipo de motor, orden, búsqueda textual.
 * - Tabla con columnas Motor, Color, Dirección, Cliente (sin ID visible).
 * - Paginación con flechas.
 * - Botones CRUD con diálogo modal para alta/edición.
 * - Enter en filtros → Filtrar, Enter en diálogo → Guardar.
 */
public class PanelPuertas extends JPanel {

    private JTable tablaPuertas;
    private DefaultTableModel modelo;
    private JButton btnNuevo, btnEditar, btnEliminar;
    private JButton btnFiltrar, btnLimpiar;
    private JButton btnPaginaAnterior, btnPaginaSiguiente;
    private JComboBox<String> cmbFiltroCliente, cmbFiltroMotor;
    private JComboBox<String> cmbOrden;
    private JTextField txtBuscar;
    private JLabel lblPaginaInfo;

    private List<Integer> idsPuertas = new ArrayList<>();

    private int paginaActual = 0;
    private final int TAMANO_PAGINA = 50;

    public PanelPuertas() {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initComponents();
        cargarCombosFiltro();
        cargarTabla();
    }

    private void initComponents() {
        JPanel panelFiltros = new JPanel(new GridBagLayout());
        panelFiltros.setBackground(Color.WHITE);
        panelFiltros.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(200, 200, 200), 1),
                new EmptyBorder(10, 14, 10, 14)
        ));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(4, 6, 4, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        int col = 0, y = 0;

        // Cliente
        gbc.gridx = col++; gbc.gridy = y;
        panelFiltros.add(new JLabel("Cliente:"), gbc);
        cmbFiltroCliente = new JComboBox<>();
        cmbFiltroCliente.addItem("-- Todos los clientes --");
        estilizarCombo(cmbFiltroCliente);
        gbc.gridx = col++; panelFiltros.add(cmbFiltroCliente, gbc);

        // Tipo Motor
        gbc.gridx = col++;
        panelFiltros.add(new JLabel("Motor:"), gbc);
        cmbFiltroMotor = new JComboBox<>();
        cmbFiltroMotor.addItem("-- Todos los motores --");
        estilizarCombo(cmbFiltroMotor);
        gbc.gridx = col++; panelFiltros.add(cmbFiltroMotor, gbc);

        // Búsqueda
        gbc.gridx = col++;
        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelBuscar.setOpaque(false);
        panelBuscar.add(new JLabel("Buscar:"));
        txtBuscar = new JTextField(14);
        Estilos.estilizarCampo(txtBuscar);
        txtBuscar.setToolTipText("Busca por color, calle, colonia, nombre del cliente o motor.");
        panelBuscar.add(txtBuscar);
        panelFiltros.add(panelBuscar, gbc);

        // Orden
        gbc.gridx = col++;
        JPanel panelOrden = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelOrden.setOpaque(false);
        panelOrden.add(new JLabel("Ordenar:"));
        cmbOrden = new JComboBox<>(new String[]{
                "Cliente A-Z",
                "Cliente Z-A",
                "Color A-Z",
                "Color Z-A"
        });
        estilizarCombo(cmbOrden);
        panelOrden.add(cmbOrden);
        gbc.gridx = col++; gbc.gridwidth = 1;
        panelFiltros.add(panelOrden, gbc);

        // Botones Limpiar / Filtrar (mismo tamaño, Limpiar primero)
        btnLimpiar = new JButton("Limpiar");
        Estilos.estilizarBoton(btnLimpiar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        btnLimpiar.setPreferredSize(new Dimension(100, 30));
        btnLimpiar.addActionListener(e -> limpiarFiltros());

        btnFiltrar = new JButton("Filtrar");
        Estilos.estilizarBoton(btnFiltrar, Estilos.AMARILLO, Color.BLACK);
        btnFiltrar.setPreferredSize(new Dimension(100, 30));
        btnFiltrar.addActionListener(e -> { paginaActual = 0; cargarTabla(); });

        gbc.gridx = col++; gbc.gridwidth = 1;
        panelFiltros.add(btnLimpiar, gbc);
        gbc.gridx = col++;
        panelFiltros.add(btnFiltrar, gbc);

        // Enter en cualquier campo del panel de filtros → Filtrar
        addEnterAction(panelFiltros, () -> { paginaActual = 0; cargarTabla(); });

        // Barra de herramientas
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        toolBar.setBackground(Color.WHITE);
        toolBar.setBorder(new EmptyBorder(5, 0, 5, 0));

        btnNuevo = new JButton("+ Nueva puerta");
        btnEditar = new JButton("Editar");
        btnEliminar = new JButton("Eliminar");

        Estilos.estilizarBoton(btnNuevo, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        Estilos.estilizarBoton(btnEliminar, Estilos.ROJO_PELIGRO, Color.WHITE);

        btnNuevo.addActionListener(e -> abrirDialogoPuerta(null));
        btnEditar.addActionListener(e -> abrirDialogoPuerta(obtenerIdSeleccionado()));
        btnEliminar.addActionListener(e -> eliminarPuerta());

        toolBar.add(btnNuevo);
        toolBar.add(btnEditar);
        toolBar.add(btnEliminar);

        // Tabla
        modelo = new DefaultTableModel(
                new String[]{"Motor", "Color", "Dirección", "Cliente"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaPuertas = new JTable(modelo);
        tablaPuertas.setRowHeight(30);
        tablaPuertas.setShowGrid(false);
        tablaPuertas.setIntercellSpacing(new Dimension(0, 0));
        tablaPuertas.setDefaultRenderer(Object.class, new RendererTablaPuertas());
        Estilos.estilizarTabla(tablaPuertas);
        tablaPuertas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarBotones();
        });

        JScrollPane scrollTabla = Estilos.scrollParaTabla(tablaPuertas);

        // Paginación
        JPanel panelPaginacion = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 8));
        panelPaginacion.setBackground(Color.WHITE);
        btnPaginaAnterior = Estilos.botonFlecha(false);
        btnPaginaSiguiente = Estilos.botonFlecha(true);
        lblPaginaInfo = new JLabel("Página 1");
        lblPaginaInfo.setFont(new Font("SansSerif", Font.BOLD, 13));
        btnPaginaAnterior.addActionListener(e -> { if (paginaActual > 0) { paginaActual--; cargarTabla(); } });
        btnPaginaSiguiente.addActionListener(e -> { paginaActual++; cargarTabla(); });
        panelPaginacion.add(btnPaginaAnterior);
        panelPaginacion.add(lblPaginaInfo);
        panelPaginacion.add(btnPaginaSiguiente);

        // Layout final
        JPanel panelSuperior = new JPanel(new BorderLayout());
        panelSuperior.setBackground(Color.WHITE);
        panelSuperior.add(panelFiltros, BorderLayout.NORTH);
        panelSuperior.add(toolBar, BorderLayout.SOUTH);

        JPanel panelCentral = new JPanel(new BorderLayout());
        panelCentral.setBackground(Color.WHITE);
        panelCentral.add(scrollTabla, BorderLayout.CENTER);
        panelCentral.add(panelPaginacion, BorderLayout.SOUTH);

        add(panelSuperior, BorderLayout.NORTH);
        add(panelCentral, BorderLayout.CENTER);
    }

    private void addEnterAction(JComponent container, Runnable action) {
        InputMap im = container.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = container.getActionMap();
        im.put(KeyStroke.getKeyStroke("ENTER"), "enterAction");
        am.put("enterAction", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                action.run();
            }
        });
    }

    private void estilizarCombo(JComboBox<?> combo) {
        combo.setBackground(Color.WHITE);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setBorder(new LineBorder(new Color(180, 180, 180), 1));
    }

    private void cargarCombosFiltro() {
        // Clientes
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_cliente, nombre, apellidos FROM cliente ORDER BY nombre")) {
            while (rs.next()) {
                cmbFiltroCliente.addItem(rs.getInt("id_cliente") + " - " + rs.getString("nombre") + " " + rs.getString("apellidos"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Tipos de motor
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_tipo_motor, nombre FROM tipo_motor ORDER BY nombre")) {
            while (rs.next()) {
                cmbFiltroMotor.addItem(rs.getInt("id_tipo_motor") + " - " + rs.getString("nombre"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        idsPuertas.clear();

        StringBuilder sql = new StringBuilder(
            "SELECT p.id_puerta, tm.nombre AS motor, p.color, " +
            "d.calle, d.numero, d.colonia, " +
            "cl.nombre AS cliente, cl.apellidos " +
            "FROM puerta p " +
            "JOIN direccion d ON p.id_direccion = d.id_direccion " +
            "JOIN cliente cl ON d.id_cliente = cl.id_cliente " +
            "LEFT JOIN tipo_motor tm ON p.id_tipo_motor = tm.id_tipo_motor " +
            "WHERE 1=1 "
        );
        List<Object> parametros = new ArrayList<>();

        if (cmbFiltroCliente.getSelectedIndex() > 0) {
            int idCliente = extraerId(cmbFiltroCliente.getSelectedItem().toString());
            sql.append("AND cl.id_cliente = ? ");
            parametros.add(idCliente);
        }
        if (cmbFiltroMotor.getSelectedIndex() > 0) {
            int idMotor = extraerId(cmbFiltroMotor.getSelectedItem().toString());
            sql.append("AND p.id_tipo_motor = ? ");
            parametros.add(idMotor);
        }
        String texto = txtBuscar.getText().trim();
        if (!texto.isEmpty()) {
            String patron = "%" + texto + "%";
            sql.append("AND (p.color ILIKE ? OR d.calle ILIKE ? OR d.colonia ILIKE ? OR cl.nombre ILIKE ? OR cl.apellidos ILIKE ? OR tm.nombre ILIKE ?) ");
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
            parametros.add(patron);
        }

        int orden = cmbOrden.getSelectedIndex();
        switch (orden) {
            case 0 -> sql.append("ORDER BY cl.nombre ASC, cl.apellidos ASC, p.color ASC ");
            case 1 -> sql.append("ORDER BY cl.nombre DESC, cl.apellidos DESC, p.color ASC ");
            case 2 -> sql.append("ORDER BY p.color ASC, cl.nombre ASC ");
            case 3 -> sql.append("ORDER BY p.color DESC, cl.nombre ASC ");
        }

        sql.append("LIMIT ").append(TAMANO_PAGINA).append(" OFFSET ").append(paginaActual * TAMANO_PAGINA);

        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    String motor = rs.getString("motor") != null ? rs.getString("motor") : "—";
                    String color = rs.getString("color");
                    String direccion = rs.getString("calle") + " " + rs.getString("numero") + ", " + rs.getString("colonia");
                    String cliente = rs.getString("cliente") + " " + rs.getString("apellidos");

                    modelo.addRow(new Object[]{motor, color, direccion, cliente});
                    idsPuertas.add(rs.getInt("id_puerta"));
                    count++;
                }
                btnPaginaSiguiente.setEnabled(count == TAMANO_PAGINA);
                btnPaginaAnterior.setEnabled(paginaActual > 0);
                int desde = paginaActual * TAMANO_PAGINA + 1;
                int hasta = paginaActual * TAMANO_PAGINA + count;
                lblPaginaInfo.setText(count == 0 ? "Sin resultados" :
                        "Mostrando " + desde + "–" + hasta + "  ·  página " + (paginaActual + 1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar puertas: " + e.getMessage());
        }
    }

    private int extraerId(String itemCombo) {
        return Integer.parseInt(itemCombo.split(" - ")[0].trim());
    }

    private void limpiarFiltros() {
        cmbFiltroCliente.setSelectedIndex(0);
        cmbFiltroMotor.setSelectedIndex(0);
        txtBuscar.setText("");
        cmbOrden.setSelectedIndex(0);
        paginaActual = 0;
        cargarTabla();
    }

    private void abrirDialogoPuerta(Integer idPuerta) {
        DialogoPuerta dialog = new DialogoPuerta(this, idPuerta);
        dialog.setVisible(true);
        if (dialog.isGuardadoExitoso()) {
            cargarTabla();
            // Refrescar también los combos de filtro (motor pudo haber cambiado)
            cmbFiltroMotor.removeAllItems();
            cmbFiltroMotor.addItem("-- Todos los motores --");
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_tipo_motor, nombre FROM tipo_motor ORDER BY nombre")) {
                while (rs.next()) {
                    cmbFiltroMotor.addItem(rs.getInt("id_tipo_motor") + " - " + rs.getString("nombre"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private Integer obtenerIdSeleccionado() {
        int row = tablaPuertas.getSelectedRow();
        if (row == -1 || row >= idsPuertas.size()) return null;
        return idsPuertas.get(row);
    }

    private void eliminarPuerta() {
        Integer id = obtenerIdSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una puerta para eliminar.");
            return;
        }

        // Verificar si alguna cita de esta puerta pertenece a una póliza
        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM cita c WHERE c.id_puerta = ? AND (" +
                 "  EXISTS (SELECT 1 FROM poliza_6m p6 WHERE c.id_cita IN (p6.id_cita_1, p6.id_cita_2)) " +
                 "  OR EXISTS (SELECT 1 FROM poliza_3m p3 WHERE c.id_cita IN (p3.id_cita_1, p3.id_cita_2, p3.id_cita_3, p3.id_cita_4)) " +
                 ") LIMIT 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this,
                            "No se puede eliminar esta puerta porque tiene citas asociadas a una póliza.\n" +
                            "Elimine primero las pólizas correspondientes desde la pestaña Pólizas.",
                            "Puerta con pólizas asociadas", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al verificar pólizas: " + e.getMessage());
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar esta puerta?\nTambién se eliminarán las citas asociadas.",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Conexion.get()) {
                conn.setAutoCommit(false);
                try {
                    // Eliminar servicio_cita de las citas de la puerta
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM servicio_cita WHERE id_cita IN (SELECT id_cita FROM cita WHERE id_puerta = ?)")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    // Eliminar citas de la puerta
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM cita WHERE id_puerta = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    // Eliminar puerta
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM puerta WHERE id_puerta = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    cargarTabla();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
            }
        }
    }

    private void actualizarBotones() {
        boolean seleccionado = tablaPuertas.getSelectedRow() != -1;
        btnEditar.setEnabled(seleccionado);
        btnEliminar.setEnabled(seleccionado);
    }

    private class RendererTablaPuertas extends DefaultTableCellRenderer {
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

    // ─── DIÁLOGO DE PUERTA ──────────────────────────────────────────────────

    private static class DialogoPuerta extends JDialog {
        private final Integer idPuertaEditar;
        private boolean guardadoExitoso = false;

        private JComboBox<ClienteItem> cmbCliente;
        private JComboBox<DireccionItem> cmbDireccion;
        private JComboBox<MotorItem> cmbMotor;
        private JComboBox<String> cmbColor;
        private JButton btnGuardar, btnCancelar;

        private static class ClienteItem {
            int id; String texto;
            ClienteItem(int id, String texto) { this.id = id; this.texto = texto; }
            @Override public String toString() { return texto; }
        }
        private static class DireccionItem {
            int idDireccion; String texto;
            DireccionItem(int idDireccion, String texto) { this.idDireccion = idDireccion; this.texto = texto; }
            @Override public String toString() { return texto; }
        }
        private static class MotorItem {
            int id; String texto;
            MotorItem(int id, String texto) { this.id = id; this.texto = texto; }
            @Override public String toString() { return texto; }
        }

        public DialogoPuerta(PanelPuertas padre, Integer idPuerta) {
            super(SwingUtilities.getWindowAncestor(padre),
                    (idPuerta == null) ? "Nueva puerta" : "Editar puerta", ModalityType.APPLICATION_MODAL);
            this.idPuertaEditar = idPuerta;
            initUI();
            cargarClientes();
            cargarMotores();
            if (idPuertaEditar != null) cargarDatos();
            else if (cmbCliente.getItemCount() > 0) cmbCliente.setSelectedIndex(0); // activa carga de direcciones
            setSize(550, 400);
            setLocationRelativeTo(padre);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);
            getRootPane().setDefaultButton(btnGuardar);
        }

        private void initUI() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int y = 0;

            gbc.gridx = 0; gbc.gridy = y;
            panel.add(new JLabel("Cliente: *"), gbc);
            cmbCliente = new JComboBox<>();
            estilizarCombo(cmbCliente);
            cmbCliente.addActionListener(e -> cargarDireccionesDelCliente());
            gbc.gridx = 1; gbc.gridwidth = 2;
            panel.add(cmbCliente, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Dirección: *"), gbc);
            cmbDireccion = new JComboBox<>();
            estilizarCombo(cmbDireccion);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panel.add(cmbDireccion, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Tipo de motor:"), gbc);
            cmbMotor = new JComboBox<>();
            estilizarCombo(cmbMotor);
            JButton btnAgregarMotor = new JButton("+");
            btnAgregarMotor.setToolTipText("Agregar nuevo tipo de motor");
            btnAgregarMotor.addActionListener(e -> agregarNuevoMotor());
            JPanel panelMotor = new JPanel(new BorderLayout());
            panelMotor.setOpaque(false);
            panelMotor.add(cmbMotor, BorderLayout.CENTER);
            panelMotor.add(btnAgregarMotor, BorderLayout.EAST);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panel.add(panelMotor, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Color: *"), gbc);
            cmbColor = new JComboBox<>(new String[]{"Blanco", "Negro", "Gris", "Madera", "Aluminio"});
            cmbColor.setEditable(true);
            estilizarCombo(cmbColor);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panel.add(cmbColor, gbc);
            gbc.gridwidth = 1;

            JLabel lblAyuda = new JLabel("<html><i style='color:gray'>* Campos obligatorios</i></html>");
            gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 3;
            panel.add(lblAyuda, gbc);

            JPanel panelBotones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            panelBotones.setBackground(Color.WHITE);
            btnGuardar = new JButton("Guardar");
            btnCancelar = new JButton("Cancelar");
            Estilos.estilizarBoton(btnGuardar, Estilos.AMARILLO, Color.BLACK);
            Estilos.estilizarBoton(btnCancelar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            btnGuardar.addActionListener(e -> guardar());
            btnCancelar.addActionListener(e -> dispose());
            panelBotones.add(btnGuardar);
            panelBotones.add(btnCancelar);
            gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 3;
            panel.add(panelBotones, gbc);

            getContentPane().add(panel);
        }

        private void estilizarCombo(JComboBox<?> combo) {
            combo.setBackground(Color.WHITE);
            combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            combo.setBorder(new LineBorder(new Color(180, 180, 180), 1));
        }

        private void cargarClientes() {
            cmbCliente.removeAllItems();
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_cliente, nombre, apellidos FROM cliente ORDER BY nombre")) {
                while (rs.next()) {
                    cmbCliente.addItem(new ClienteItem(rs.getInt("id_cliente"),
                            rs.getString("nombre") + " " + rs.getString("apellidos")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            if (cmbCliente.getItemCount() > 0) cmbCliente.setSelectedIndex(0);
            else cmbCliente.setSelectedItem(null);
        }

        private void cargarMotores() {
            cmbMotor.removeAllItems();
            cmbMotor.addItem(new MotorItem(-1, "-- Sin motor --")); // opción nulo
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_tipo_motor, nombre FROM tipo_motor ORDER BY nombre")) {
                while (rs.next()) {
                    cmbMotor.addItem(new MotorItem(rs.getInt("id_tipo_motor"), rs.getString("nombre")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            cmbMotor.setSelectedIndex(0); // sin motor por defecto
        }

        private void cargarDireccionesDelCliente() {
            cmbDireccion.removeAllItems();
            ClienteItem cliente = (ClienteItem) cmbCliente.getSelectedItem();
            if (cliente == null) return;
            String sql = "SELECT id_direccion, calle, numero, colonia FROM direccion WHERE id_cliente = ? ORDER BY id_direccion";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, cliente.id);
                ResultSet rs = ps.executeQuery();
                boolean hayDirecciones = false;
                while (rs.next()) {
                    hayDirecciones = true;
                    String texto = rs.getString("calle") + " " + rs.getString("numero") + ", " + rs.getString("colonia");
                    cmbDireccion.addItem(new DireccionItem(rs.getInt("id_direccion"), texto));
                }
                if (!hayDirecciones) {
                    JOptionPane.showMessageDialog(this,
                            "Este cliente no tiene direcciones registradas.\nAgregue una dirección desde la pestaña Clientes.",
                            "Sin direcciones", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            if (cmbDireccion.getItemCount() > 0) cmbDireccion.setSelectedIndex(0);
        }

        private void agregarNuevoMotor() {
            String nombre = JOptionPane.showInputDialog(this, "Nombre del nuevo tipo de motor:");
            if (nombre != null && !nombre.trim().isEmpty()) {
                try (Connection conn = Conexion.get();
                     PreparedStatement ps = conn.prepareStatement("INSERT INTO tipo_motor (nombre) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, nombre.trim());
                    ps.executeUpdate();
                    ResultSet keys = ps.getGeneratedKeys();
                    if (keys.next()) {
                        int id = keys.getInt(1);
                        cmbMotor.addItem(new MotorItem(id, nombre.trim()));
                        cmbMotor.setSelectedIndex(cmbMotor.getItemCount() - 1);
                    }
                } catch (SQLException e) { e.printStackTrace(); }
            }
        }

        private void cargarDatos() {
            String sql = "SELECT p.color, p.id_direccion, p.id_tipo_motor, d.id_cliente " +
                         "FROM puerta p " +
                         "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                         "WHERE p.id_puerta = ?";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idPuertaEditar);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    // Seleccionar cliente
                    int idCliente = rs.getInt("id_cliente");
                    for (int i = 0; i < cmbCliente.getItemCount(); i++) {
                        if (cmbCliente.getItemAt(i).id == idCliente) {
                            cmbCliente.setSelectedIndex(i);
                            break;
                        }
                    }
                    // Seleccionar dirección (ya cargada por el listener)
                    int idDireccion = rs.getInt("id_direccion");
                    for (int i = 0; i < cmbDireccion.getItemCount(); i++) {
                        if (cmbDireccion.getItemAt(i).idDireccion == idDireccion) {
                            cmbDireccion.setSelectedIndex(i);
                            break;
                        }
                    }
                    // Seleccionar motor
                    int idMotor = rs.getInt("id_tipo_motor");
                    if (rs.wasNull()) {
                        cmbMotor.setSelectedIndex(0); // sin motor
                    } else {
                        for (int i = 1; i < cmbMotor.getItemCount(); i++) {
                            if (cmbMotor.getItemAt(i).id == idMotor) {
                                cmbMotor.setSelectedIndex(i);
                                break;
                            }
                        }
                    }
                    cmbColor.setSelectedItem(rs.getString("color"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        private void guardar() {
            ClienteItem cliente = (ClienteItem) cmbCliente.getSelectedItem();
            if (cliente == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un cliente.");
                return;
            }
            DireccionItem direccion = (DireccionItem) cmbDireccion.getSelectedItem();
            if (direccion == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una dirección.");
                return;
            }
            String color = cmbColor.getSelectedItem() != null ? cmbColor.getSelectedItem().toString().trim() : "";
            if (color.isEmpty()) {
                JOptionPane.showMessageDialog(this, "El color es obligatorio.");
                return;
            }

            MotorItem motor = (MotorItem) cmbMotor.getSelectedItem();
            Integer idMotor = (motor != null && motor.id != -1) ? motor.id : null;

            try (Connection conn = Conexion.get()) {
                if (idPuertaEditar == null) {
                    String sql = "INSERT INTO puerta (color, id_direccion, id_tipo_motor) VALUES (?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, color);
                        ps.setInt(2, direccion.idDireccion);
                        if (idMotor != null) ps.setInt(3, idMotor);
                        else ps.setNull(3, Types.INTEGER);
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE puerta SET color=?, id_direccion=?, id_tipo_motor=? WHERE id_puerta=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, color);
                        ps.setInt(2, direccion.idDireccion);
                        if (idMotor != null) ps.setInt(3, idMotor);
                        else ps.setNull(3, Types.INTEGER);
                        ps.setInt(4, idPuertaEditar);
                        ps.executeUpdate();
                    }
                }
                guardadoExitoso = true;
                JOptionPane.showMessageDialog(this, "Puerta guardada correctamente.");
                dispose();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
            }
        }

        public boolean isGuardadoExitoso() {
            return guardadoExitoso;
        }
    }
}