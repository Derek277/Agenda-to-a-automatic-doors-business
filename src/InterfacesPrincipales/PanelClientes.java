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
 * Panel de gestión de clientes.
 *
 * Diseño deliberadamente simple, distinto al de Citas:
 * - No se muestra el ID (el cliente no necesita verlo).
 * - No se muestra una sola "dirección" en la tabla, porque un cliente puede
 *   tener varias direcciones con varias puertas cada una; mostrar solo una
 *   sería engañoso. El detalle de direcciones se gestiona dentro del diálogo
 *   de alta/edición.
 * - Filtro único: buscar por nombre o alias, más un botón que invierte el
 *   orden alfabético (A-Z / Z-A) en vez de un combo de "ordenar por".
 * - Mismo patrón de paginación con flechas y de botones Nuevo/Editar/Eliminar
 *   que ya se usa en el panel de Citas.
 */
public class PanelClientes extends JPanel {

    private JTable tablaClientes;
    private DefaultTableModel modelo;
    private JButton btnNuevo, btnEditar, btnEliminar;
    private JButton btnFiltrar, btnLimpiar, btnOrden;
    private JButton btnPaginaAnterior, btnPaginaSiguiente;
    private JTextField txtBuscar;
    private JLabel lblPaginaInfo;

    private List<Integer> idsClientes = new ArrayList<>();
    private boolean ordenAscendente = true; // true = A-Z, false = Z-A

    private int paginaActual = 0;
    private final int TAMANO_PAGINA = 50;

    public PanelClientes() {
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initComponents();
        cargarTabla();
    }

    private void initComponents() {
        // ── Panel de filtros (una sola fila, simple) ──
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

        int col = 0;

        // Etiqueta y campo de búsqueda pegados
        JPanel panelBuscar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelBuscar.setOpaque(false);
        panelBuscar.add(new JLabel("Buscar por nombre o alias: "));
        txtBuscar = new JTextField(20);
        Estilos.estilizarCampo(txtBuscar);
        panelBuscar.add(txtBuscar);
        gbc.gridx = col++; gbc.gridy = 0;
        panelFiltros.add(panelBuscar, gbc);

        // Botón de orden alfabético: alterna A-Z / Z-A y lo indica en su propio texto
        btnOrden = new JButton("Orden: A → Z");
        Estilos.estilizarBoton(btnOrden, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        btnOrden.setToolTipText("Cambia el orden alfabético por nombre.");
        btnOrden.addActionListener(e -> {
            ordenAscendente = !ordenAscendente;
            btnOrden.setText(ordenAscendente ? "Orden: A → Z" : "Orden: Z → A");
            paginaActual = 0;
            cargarTabla();
        });
        gbc.gridx = col++;
        panelFiltros.add(btnOrden, gbc);

        // Limpiar primero, Filtrar después (mismo orden y tamaño que en Citas)
        btnLimpiar = new JButton("Limpiar");
        Estilos.estilizarBoton(btnLimpiar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        btnLimpiar.setPreferredSize(new Dimension(100, 30));
        btnLimpiar.addActionListener(e -> limpiarFiltros());

        btnFiltrar = new JButton("Filtrar");
        Estilos.estilizarBoton(btnFiltrar, Estilos.AMARILLO, Color.BLACK);
        btnFiltrar.setPreferredSize(new Dimension(100, 30));
        btnFiltrar.addActionListener(e -> { paginaActual = 0; cargarTabla(); });

        gbc.gridx = col++;
        panelFiltros.add(btnLimpiar, gbc);
        gbc.gridx = col++;
        panelFiltros.add(btnFiltrar, gbc);

        // Enter en el campo de búsqueda → Filtrar
        addEnterAction(panelFiltros, () -> { paginaActual = 0; cargarTabla(); });

        // ── Barra de herramientas (CRUD) ──
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        toolBar.setBackground(Color.WHITE);
        toolBar.setBorder(new EmptyBorder(5, 0, 5, 0));

        btnNuevo = new JButton("+ Nuevo cliente");
        btnEditar = new JButton("Editar");
        btnEliminar = new JButton("Eliminar");

        Estilos.estilizarBoton(btnNuevo, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        Estilos.estilizarBoton(btnEliminar, Estilos.ROJO_PELIGRO, Color.WHITE);

        btnNuevo.addActionListener(e -> abrirDialogoCliente(null));
        btnEditar.addActionListener(e -> abrirDialogoCliente(obtenerIdSeleccionado()));
        btnEliminar.addActionListener(e -> eliminarCliente());

        toolBar.add(btnNuevo);
        toolBar.add(btnEditar);
        toolBar.add(btnEliminar);

        // ── Tabla (sin ID, sin dirección) ──
        modelo = new DefaultTableModel(new String[]{"Nombre", "Apellidos", "Alias", "Teléfono"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaClientes = new JTable(modelo);
        tablaClientes.setRowHeight(30);
        tablaClientes.setShowGrid(false);
        tablaClientes.setIntercellSpacing(new Dimension(0, 0));
        tablaClientes.setDefaultRenderer(Object.class, new RendererTablaClientes());
        Estilos.estilizarTabla(tablaClientes);
        tablaClientes.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarBotones();
        });
        // Doble clic = editar directamente
        tablaClientes.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) abrirDialogoCliente(obtenerIdSeleccionado());
            }
        });

        JScrollPane scrollTabla = Estilos.scrollParaTabla(tablaClientes);

        // ── Paginación ──
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

        // ── Layout final ──
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
            @Override public void actionPerformed(ActionEvent e) { action.run(); }
        });
    }

    // ── Carga de datos ────────────────────────────────────────────────────────

    private void cargarTabla() {
        modelo.setRowCount(0);
        idsClientes.clear();

        StringBuilder sql = new StringBuilder(
            "SELECT id_cliente, nombre, apellidos, alias_cliente, telefono_movil " +
            "FROM cliente WHERE 1=1 "
        );
        List<Object> parametros = new ArrayList<>();

        String texto = txtBuscar.getText().trim();
        if (!texto.isEmpty()) {
            String patron = "%" + texto + "%";
            sql.append("AND (nombre ILIKE ? OR alias_cliente ILIKE ?) ");
            parametros.add(patron);
            parametros.add(patron);
        }

        sql.append("ORDER BY nombre ").append(ordenAscendente ? "ASC" : "DESC")
           .append(", apellidos ").append(ordenAscendente ? "ASC" : "DESC")
           .append(" LIMIT ").append(TAMANO_PAGINA)
           .append(" OFFSET ").append(paginaActual * TAMANO_PAGINA);

        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < parametros.size(); i++) {
                ps.setObject(i + 1, parametros.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                int count = 0;
                while (rs.next()) {
                    modelo.addRow(new Object[]{
                        rs.getString("nombre"),
                        rs.getString("apellidos"),
                        rs.getString("alias_cliente") != null ? rs.getString("alias_cliente") : "—",
                        rs.getString("telefono_movil") != null ? rs.getString("telefono_movil") : "—"
                    });
                    idsClientes.add(rs.getInt("id_cliente"));
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
            JOptionPane.showMessageDialog(this, "Error al cargar clientes: " + e.getMessage());
        }
    }

    private void limpiarFiltros() {
        txtBuscar.setText("");
        ordenAscendente = true;
        btnOrden.setText("Orden: A → Z");
        paginaActual = 0;
        cargarTabla();
    }

    private void abrirDialogoCliente(Integer idCliente) {
        DialogoCliente dialog = new DialogoCliente(this, idCliente);
        dialog.setVisible(true);
        if (dialog.isGuardadoExitoso()) {
            cargarTabla();
        }
    }

    private Integer obtenerIdSeleccionado() {
        int row = tablaClientes.getSelectedRow();
        if (row == -1 || row >= idsClientes.size()) return null;
        return idsClientes.get(row);
    }

    private void eliminarCliente() {
        Integer id = obtenerIdSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccione un cliente para eliminar.");
            return;
        }

        // Verificar si alguna de las citas del cliente pertenece a una póliza activa
        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT 1 FROM cita c " +
                 "JOIN puerta p ON c.id_puerta = p.id_puerta " +
                 "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                 "WHERE d.id_cliente = ? AND (" +
                 "  EXISTS (SELECT 1 FROM poliza_6m p6 WHERE c.id_cita IN (p6.id_cita_1, p6.id_cita_2)) " +
                 "  OR EXISTS (SELECT 1 FROM poliza_3m p3 WHERE c.id_cita IN (p3.id_cita_1, p3.id_cita_2, p3.id_cita_3, p3.id_cita_4)) " +
                 ") LIMIT 1")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this,
                            "No se puede eliminar este cliente porque tiene citas asociadas a una póliza.\n" +
                            "Elimine primero las pólizas correspondientes desde la pestaña Pólizas.",
                            "Cliente con pólizas asociadas", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al verificar pólizas: " + e.getMessage());
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar este cliente?\nTambién se eliminarán sus direcciones, puertas y citas asociadas.",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Conexion.get()) {
                conn.setAutoCommit(false);
                try {
                    // Borrado en cascada manual, respetando las FK ON DELETE RESTRICT del esquema
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM servicio_cita WHERE id_cita IN (" +
                            "  SELECT c.id_cita FROM cita c " +
                            "  JOIN puerta p ON c.id_puerta = p.id_puerta " +
                            "  JOIN direccion d ON p.id_direccion = d.id_direccion " +
                            "  WHERE d.id_cliente = ?)")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM cita WHERE id_puerta IN (" +
                            "  SELECT p.id_puerta FROM puerta p " +
                            "  JOIN direccion d ON p.id_direccion = d.id_direccion " +
                            "  WHERE d.id_cliente = ?)")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM puerta WHERE id_direccion IN (" +
                            "  SELECT id_direccion FROM direccion WHERE id_cliente = ?)")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM direccion WHERE id_cliente = ?")) {
                        ps.setInt(1, id);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM cliente WHERE id_cliente = ?")) {
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
        boolean seleccionado = tablaClientes.getSelectedRow() != -1;
        btnEditar.setEnabled(seleccionado);
        btnEliminar.setEnabled(seleccionado);
    }

    private class RendererTablaClientes extends DefaultTableCellRenderer {
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

    // ─── DIÁLOGO DE CLIENTE (con gestión de sus direcciones) ───────────────

    private static class DialogoCliente extends JDialog {
        private final Integer idClienteEditar;
        private boolean guardadoExitoso = false;

        private JTextField txtNombre, txtApellidos, txtTelefono, txtAlias;
        private JTable tablaDirecciones;
        private DefaultTableModel modeloDirecciones;
        private List<Integer> idsDirecciones = new ArrayList<>();
        private JButton btnGuardar, btnCancelar;
        private JButton btnAgregarDireccion, btnEditarDireccion, btnEliminarDireccion;

        public DialogoCliente(PanelClientes padre, Integer idCliente) {
            super(SwingUtilities.getWindowAncestor(padre),
                    (idCliente == null) ? "Nuevo cliente" : "Editar cliente", ModalityType.APPLICATION_MODAL);
            this.idClienteEditar = idCliente;
            initUI();
            if (idClienteEditar != null) {
                cargarDatosCliente();
                cargarDirecciones();
            }
            setSize(640, 560);
            setLocationRelativeTo(padre);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);
            getRootPane().setDefaultButton(btnGuardar); // Enter → Guardar
        }

        private void initUI() {
            JPanel panel = new JPanel(new BorderLayout(15, 15));
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            panel.setBackground(Color.WHITE);

            // ── Datos personales ──
            JPanel panelDatos = new JPanel(new GridBagLayout());
            panelDatos.setBackground(Color.WHITE);
            panelDatos.setBorder(BorderFactory.createTitledBorder("Datos del cliente"));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int y = 0;
            gbc.gridx = 0; gbc.gridy = y;
            panelDatos.add(new JLabel("Nombre: *"), gbc);
            txtNombre = new JTextField(15);
            Estilos.estilizarCampo(txtNombre);
            gbc.gridx = 1;
            panelDatos.add(txtNombre, gbc);

            gbc.gridx = 2;
            panelDatos.add(new JLabel("Apellidos: *"), gbc);
            txtApellidos = new JTextField(15);
            Estilos.estilizarCampo(txtApellidos);
            gbc.gridx = 3;
            panelDatos.add(txtApellidos, gbc);

            gbc.gridx = 0; gbc.gridy = ++y;
            panelDatos.add(new JLabel("Teléfono:"), gbc);
            txtTelefono = new JTextField(15);
            Estilos.estilizarCampo(txtTelefono);
            gbc.gridx = 1;
            panelDatos.add(txtTelefono, gbc);

            gbc.gridx = 2;
            panelDatos.add(new JLabel("Alias:"), gbc);
            txtAlias = new JTextField(15);
            Estilos.estilizarCampo(txtAlias);
            gbc.gridx = 3;
            panelDatos.add(txtAlias, gbc);

            panel.add(panelDatos, BorderLayout.NORTH);

            // ── Direcciones del cliente ──
            JPanel panelDirecciones = new JPanel(new BorderLayout(0, 8));
            panelDirecciones.setBackground(Color.WHITE);
            panelDirecciones.setBorder(BorderFactory.createTitledBorder("Direcciones registradas"));

            modeloDirecciones = new DefaultTableModel(new String[]{"Calle", "Número", "Colonia"}, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };
            tablaDirecciones = new JTable(modeloDirecciones);
            tablaDirecciones.setRowHeight(28);
            Estilos.estilizarTabla(tablaDirecciones);
            tablaDirecciones.getSelectionModel().addListSelectionListener(e -> actualizarBotonesDireccion());

            JScrollPane scrollDirecciones = Estilos.scrollParaTabla(tablaDirecciones);
            scrollDirecciones.setPreferredSize(new Dimension(0, 150));

            JPanel toolbarDirecciones = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            toolbarDirecciones.setBackground(Color.WHITE);
            btnAgregarDireccion = new JButton("+ Agregar dirección");
            btnEditarDireccion = new JButton("Editar dirección");
            btnEliminarDireccion = new JButton("Eliminar dirección");
            Estilos.estilizarBoton(btnAgregarDireccion, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            Estilos.estilizarBoton(btnEditarDireccion, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            Estilos.estilizarBoton(btnEliminarDireccion, Estilos.ROJO_PELIGRO, Color.WHITE);

            btnAgregarDireccion.addActionListener(e -> agregarDireccion());
            btnEditarDireccion.addActionListener(e -> editarDireccion());
            btnEliminarDireccion.addActionListener(e -> eliminarDireccion());

            if (idClienteEditar == null) {
                // Mientras el cliente no exista en BD, no se pueden agregar direcciones todavía
                btnAgregarDireccion.setEnabled(false);
                btnAgregarDireccion.setToolTipText("Primero guarde el cliente; luego podrá agregar direcciones.");
            }

            toolbarDirecciones.add(btnAgregarDireccion);
            toolbarDirecciones.add(btnEditarDireccion);
            toolbarDirecciones.add(btnEliminarDireccion);

            panelDirecciones.add(scrollDirecciones, BorderLayout.CENTER);
            panelDirecciones.add(toolbarDirecciones, BorderLayout.SOUTH);

            panel.add(panelDirecciones, BorderLayout.CENTER);

            // ── Botones finales ──
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
            panel.add(panelBotones, BorderLayout.SOUTH);

            getContentPane().add(panel);
            actualizarBotonesDireccion();
        }

        private void actualizarBotonesDireccion() {
            boolean haySeleccion = tablaDirecciones.getSelectedRow() != -1;
            btnEditarDireccion.setEnabled(haySeleccion);
            btnEliminarDireccion.setEnabled(haySeleccion);
        }

        private void cargarDatosCliente() {
            String sql = "SELECT nombre, apellidos, telefono_movil, alias_cliente FROM cliente WHERE id_cliente = ?";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idClienteEditar);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    txtNombre.setText(rs.getString("nombre"));
                    txtApellidos.setText(rs.getString("apellidos"));
                    txtTelefono.setText(rs.getString("telefono_movil"));
                    txtAlias.setText(rs.getString("alias_cliente"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        private void cargarDirecciones() {
            modeloDirecciones.setRowCount(0);
            idsDirecciones.clear();
            String sql = "SELECT id_direccion, calle, numero, colonia FROM direccion WHERE id_cliente = ? ORDER BY id_direccion";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idClienteEditar);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    modeloDirecciones.addRow(new Object[]{
                            rs.getString("calle"), rs.getString("numero"), rs.getString("colonia")
                    });
                    idsDirecciones.add(rs.getInt("id_direccion"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        private void agregarDireccion() {
            DialogoDireccion dlg = new DialogoDireccion(this, idClienteEditar, null);
            dlg.setVisible(true);
            if (dlg.isGuardadoExitoso()) cargarDirecciones();
        }

        private void editarDireccion() {
            int row = tablaDirecciones.getSelectedRow();
            if (row == -1) return;
            int idDireccion = idsDirecciones.get(row);
            DialogoDireccion dlg = new DialogoDireccion(this, idClienteEditar, idDireccion);
            dlg.setVisible(true);
            if (dlg.isGuardadoExitoso()) cargarDirecciones();
        }

        private void eliminarDireccion() {
            int row = tablaDirecciones.getSelectedRow();
            if (row == -1) return;
            int idDireccion = idsDirecciones.get(row);

            // Verificar si alguna cita de esta dirección pertenece a una póliza
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM cita c " +
                     "JOIN puerta p ON c.id_puerta = p.id_puerta " +
                     "WHERE p.id_direccion = ? AND (" +
                     "  EXISTS (SELECT 1 FROM poliza_6m p6 WHERE c.id_cita IN (p6.id_cita_1, p6.id_cita_2)) " +
                     "  OR EXISTS (SELECT 1 FROM poliza_3m p3 WHERE c.id_cita IN (p3.id_cita_1, p3.id_cita_2, p3.id_cita_3, p3.id_cita_4)) " +
                     ") LIMIT 1")) {
                ps.setInt(1, idDireccion);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        JOptionPane.showMessageDialog(this,
                                "No se puede eliminar esta dirección porque tiene citas asociadas a una póliza.\n" +
                                "Elimine primero las pólizas correspondientes desde la pestaña Pólizas.",
                                "Dirección con pólizas asociadas", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al verificar pólizas: " + e.getMessage());
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar esta dirección?\nTambién se eliminarán las puertas y citas asociadas a ella.",
                    "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.YES_OPTION) return;

            try (Connection conn = Conexion.get()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM servicio_cita WHERE id_cita IN (" +
                            "  SELECT c.id_cita FROM cita c " +
                            "  JOIN puerta p ON c.id_puerta = p.id_puerta " +
                            "  WHERE p.id_direccion = ?)")) {
                        ps.setInt(1, idDireccion);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM cita WHERE id_puerta IN (SELECT id_puerta FROM puerta WHERE id_direccion = ?)")) {
                        ps.setInt(1, idDireccion);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM puerta WHERE id_direccion = ?")) {
                        ps.setInt(1, idDireccion);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = conn.prepareStatement(
                            "DELETE FROM direccion WHERE id_direccion = ?")) {
                        ps.setInt(1, idDireccion);
                        ps.executeUpdate();
                    }
                    conn.commit();
                    cargarDirecciones();
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al eliminar dirección: " + e.getMessage());
            }
        }

        private void guardar() {
            String nombre = txtNombre.getText().trim();
            String apellidos = txtApellidos.getText().trim();
            if (nombre.isEmpty() || apellidos.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nombre y apellidos son obligatorios.");
                return;
            }

            try (Connection conn = Conexion.get()) {
                if (idClienteEditar == null) {
                    String sql = "INSERT INTO cliente (nombre, apellidos, telefono_movil, alias_cliente) VALUES (?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nombre);
                        ps.setString(2, apellidos);
                        ps.setString(3, txtTelefono.getText().trim());
                        ps.setString(4, txtAlias.getText().trim());
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE cliente SET nombre=?, apellidos=?, telefono_movil=?, alias_cliente=? WHERE id_cliente=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, nombre);
                        ps.setString(2, apellidos);
                        ps.setString(3, txtTelefono.getText().trim());
                        ps.setString(4, txtAlias.getText().trim());
                        ps.setInt(5, idClienteEditar);
                        ps.executeUpdate();
                    }
                }
                guardadoExitoso = true;
                JOptionPane.showMessageDialog(this, "Cliente guardado correctamente.");
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

    // ─── DIÁLOGO DE DIRECCIÓN (para agregar/editar una dirección del cliente) ─

    private static class DialogoDireccion extends JDialog {
        private final int idCliente;
        private final Integer idDireccionEditar;
        private boolean guardadoExitoso = false;

        private JTextField txtCalle, txtNumero, txtColonia;
        private JTextArea txtReferencia;

        public DialogoDireccion(JDialog padre, int idCliente, Integer idDireccion) {
            super(padre, (idDireccion == null) ? "Nueva dirección" : "Editar dirección", ModalityType.APPLICATION_MODAL);
            this.idCliente = idCliente;
            this.idDireccionEditar = idDireccion;
            initUI();
            if (idDireccionEditar != null) cargarDatos();
            setSize(420, 380);
            setLocationRelativeTo(padre);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);
        }

        private void initUI() {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 6, 6, 6);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int y = 0;
            gbc.gridx = 0; gbc.gridy = y;
            panel.add(new JLabel("Calle: *"), gbc);
            txtCalle = new JTextField(18);
            Estilos.estilizarCampo(txtCalle);
            gbc.gridx = 1;
            panel.add(txtCalle, gbc);

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Número: *"), gbc);
            txtNumero = new JTextField(18);
            Estilos.estilizarCampo(txtNumero);
            gbc.gridx = 1;
            panel.add(txtNumero, gbc);

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Colonia: *"), gbc);
            txtColonia = new JTextField(18);
            Estilos.estilizarCampo(txtColonia);
            gbc.gridx = 1;
            panel.add(txtColonia, gbc);

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Referencia:"), gbc);
            txtReferencia = new JTextArea(4, 18);
            txtReferencia.setLineWrap(true);
            txtReferencia.setWrapStyleWord(true);
            txtReferencia.setBorder(new LineBorder(new Color(180, 180, 180), 1));
            JScrollPane scrollRef = new JScrollPane(txtReferencia);
            gbc.gridx = 1;
            panel.add(scrollRef, gbc);

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            botones.setBackground(Color.WHITE);
            JButton btnGuardar = new JButton("Guardar");
            JButton btnCancelar = new JButton("Cancelar");
            Estilos.estilizarBoton(btnGuardar, Estilos.AMARILLO, Color.BLACK);
            Estilos.estilizarBoton(btnCancelar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            btnGuardar.addActionListener(e -> guardar());
            btnCancelar.addActionListener(e -> dispose());
            botones.add(btnGuardar);
            botones.add(btnCancelar);

            gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 2;
            panel.add(botones, gbc);

            getContentPane().add(panel);
            getRootPane().setDefaultButton(btnGuardar);
        }

        private void cargarDatos() {
            String sql = "SELECT calle, numero, colonia, referencia FROM direccion WHERE id_direccion = ?";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idDireccionEditar);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    txtCalle.setText(rs.getString("calle"));
                    txtNumero.setText(rs.getString("numero"));
                    txtColonia.setText(rs.getString("colonia"));
                    txtReferencia.setText(rs.getString("referencia"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        private void guardar() {
            String calle = txtCalle.getText().trim();
            String numero = txtNumero.getText().trim();
            String colonia = txtColonia.getText().trim();
            if (calle.isEmpty() || numero.isEmpty() || colonia.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Calle, número y colonia son obligatorios.");
                return;
            }

            try (Connection conn = Conexion.get()) {
                if (idDireccionEditar == null) {
                    String sql = "INSERT INTO direccion (calle, numero, colonia, referencia, id_cliente) VALUES (?, ?, ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, calle);
                        ps.setString(2, numero);
                        ps.setString(3, colonia);
                        ps.setString(4, txtReferencia.getText().trim());
                        ps.setInt(5, idCliente);
                        ps.executeUpdate();
                    }
                } else {
                    String sql = "UPDATE direccion SET calle=?, numero=?, colonia=?, referencia=? WHERE id_direccion=?";
                    try (PreparedStatement ps = conn.prepareStatement(sql)) {
                        ps.setString(1, calle);
                        ps.setString(2, numero);
                        ps.setString(3, colonia);
                        ps.setString(4, txtReferencia.getText().trim());
                        ps.setInt(5, idDireccionEditar);
                        ps.executeUpdate();
                    }
                }
                guardadoExitoso = true;
                dispose();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al guardar dirección: " + e.getMessage());
            }
        }

        public boolean isGuardadoExitoso() {
            return guardadoExitoso;
        }
    }
}
