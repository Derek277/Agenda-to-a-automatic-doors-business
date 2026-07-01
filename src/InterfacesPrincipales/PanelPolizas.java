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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PanelPolizas extends JPanel {

    private JTable tablaPolizas;
    private DefaultTableModel modelo;
    private JButton btnNueva, btnEditar, btnEliminar;
    private JButton btnFiltrar, btnLimpiar;
    private JButton btnPaginaAnterior, btnPaginaSiguiente;
    private JComboBox<String> cmbFiltroCliente, cmbFiltroTipo, cmbFiltroEstado;
    private JLabel lblPaginaInfo;

    private List<Integer> idsPolizas = new ArrayList<>();
    private List<String> origenPolizas = new ArrayList<>(); // "6m" o "3m"

    private int paginaActual = 0;
    private final int TAMANO_PAGINA = 50;
    private final int idUsuarioActual;

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public PanelPolizas(int idUsuario) {
        this.idUsuarioActual = idUsuario;
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

        int col = 0;

        gbc.gridx = col++; gbc.gridy = 0;
        panelFiltros.add(new JLabel("Cliente:"), gbc);
        cmbFiltroCliente = new JComboBox<>();
        cmbFiltroCliente.addItem("-- Todos --");
        estilizarCombo(cmbFiltroCliente);
        gbc.gridx = col++; panelFiltros.add(cmbFiltroCliente, gbc);

        gbc.gridx = col++;
        panelFiltros.add(new JLabel("Tipo:"), gbc);
        cmbFiltroTipo = new JComboBox<>(new String[]{"Todos", "6 meses", "3 meses"});
        estilizarCombo(cmbFiltroTipo);
        gbc.gridx = col++; panelFiltros.add(cmbFiltroTipo, gbc);

        gbc.gridx = col++;
        panelFiltros.add(new JLabel("Estado:"), gbc);
        cmbFiltroEstado = new JComboBox<>(new String[]{"Todos", "Activa", "Terminada"});
        estilizarCombo(cmbFiltroEstado);
        gbc.gridx = col++; panelFiltros.add(cmbFiltroEstado, gbc);

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

        addEnterAction(panelFiltros, () -> { paginaActual = 0; cargarTabla(); });

        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 5));
        toolBar.setBackground(Color.WHITE);
        toolBar.setBorder(new EmptyBorder(5, 0, 5, 0));

        btnNueva = new JButton("+ Nueva póliza");
        btnEditar = new JButton("Editar");
        btnEliminar = new JButton("Eliminar");

        Estilos.estilizarBoton(btnNueva, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        Estilos.estilizarBoton(btnEliminar, Estilos.ROJO_PELIGRO, Color.WHITE);

        btnNueva.addActionListener(e -> abrirDialogoNuevaPoliza());
        btnEditar.addActionListener(e -> verDetallePoliza());
        btnEliminar.addActionListener(e -> eliminarPoliza());

        toolBar.add(btnNueva);
        toolBar.add(btnEditar);
        toolBar.add(btnEliminar);

        modelo = new DefaultTableModel(
                new String[]{"Cliente", "Puerta", "Tipo", "Estado", "Última cita"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaPolizas = new JTable(modelo);
        tablaPolizas.setRowHeight(30);
        tablaPolizas.setShowGrid(false);
        tablaPolizas.setIntercellSpacing(new Dimension(0, 0));
        tablaPolizas.setDefaultRenderer(Object.class, new RendererPoliza());
        Estilos.estilizarTabla(tablaPolizas);
        tablaPolizas.getSelectionModel().addListSelectionListener(e -> actualizarBotones());
        tablaPolizas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) verDetallePoliza();
            }
        });

        JScrollPane scrollTabla = Estilos.scrollParaTabla(tablaPolizas);

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

    private void estilizarCombo(JComboBox<?> combo) {
        combo.setBackground(Color.WHITE);
        combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
        combo.setBorder(new LineBorder(new Color(180, 180, 180), 1));
    }

    private void cargarCombosFiltro() {
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_cliente, nombre, apellidos FROM cliente ORDER BY nombre")) {
            while (rs.next()) {
                cmbFiltroCliente.addItem(rs.getInt("id_cliente") + " - " + rs.getString("nombre") + " " + rs.getString("apellidos"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        idsPolizas.clear();
        origenPolizas.clear();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM (");
        sql.append("SELECT p.id_poliza_6m AS id, '6m' AS origen, ");
        sql.append("cl.nombre || ' ' || cl.apellidos AS cliente, ");
        sql.append("tm.nombre AS motor, pu.color AS puerta_color, ");
        sql.append("'6 meses' AS tipo, ");
        sql.append("CASE WHEN MAX(c.fecha_hora) >= NOW() THEN 'Activa' ELSE 'Terminada' END AS estado, ");
        sql.append("MAX(c.fecha_hora) AS ultima_cita ");
        sql.append("FROM poliza_6m p ");
        sql.append("JOIN cita c ON c.id_cita IN (p.id_cita_1, p.id_cita_2) ");
        sql.append("JOIN puerta pu ON c.id_puerta = pu.id_puerta ");
        sql.append("JOIN direccion d ON pu.id_direccion = d.id_direccion ");
        sql.append("JOIN cliente cl ON d.id_cliente = cl.id_cliente ");
        sql.append("LEFT JOIN tipo_motor tm ON pu.id_tipo_motor = tm.id_tipo_motor ");
        sql.append("GROUP BY p.id_poliza_6m, cl.nombre, cl.apellidos, tm.nombre, pu.color ");
        sql.append("UNION ALL ");
        sql.append("SELECT p.id_poliza_3m AS id, '3m' AS origen, ");
        sql.append("cl.nombre || ' ' || cl.apellidos AS cliente, ");
        sql.append("tm.nombre AS motor, pu.color AS puerta_color, ");
        sql.append("'3 meses' AS tipo, ");
        sql.append("CASE WHEN MAX(c.fecha_hora) >= NOW() THEN 'Activa' ELSE 'Terminada' END AS estado, ");
        sql.append("MAX(c.fecha_hora) AS ultima_cita ");
        sql.append("FROM poliza_3m p ");
        sql.append("JOIN cita c ON c.id_cita IN (p.id_cita_1, p.id_cita_2, p.id_cita_3, p.id_cita_4) ");
        sql.append("JOIN puerta pu ON c.id_puerta = pu.id_puerta ");
        sql.append("JOIN direccion d ON pu.id_direccion = d.id_direccion ");
        sql.append("JOIN cliente cl ON d.id_cliente = cl.id_cliente ");
        sql.append("LEFT JOIN tipo_motor tm ON pu.id_tipo_motor = tm.id_tipo_motor ");
        sql.append("GROUP BY p.id_poliza_3m, cl.nombre, cl.apellidos, tm.nombre, pu.color ");
        sql.append(") sub WHERE 1=1 ");

        List<Object> parametros = new ArrayList<>();

        if (cmbFiltroCliente.getSelectedIndex() > 0) {
            int idCliente = extraerId(cmbFiltroCliente.getSelectedItem().toString());
            sql.append("AND sub.cliente IN (SELECT nombre || ' ' || apellidos FROM cliente WHERE id_cliente = ?) ");
            parametros.add(idCliente);
        }

        if (cmbFiltroTipo.getSelectedIndex() == 1) {
            sql.append("AND sub.tipo = '6 meses' ");
        } else if (cmbFiltroTipo.getSelectedIndex() == 2) {
            sql.append("AND sub.tipo = '3 meses' ");
        }

        if (cmbFiltroEstado.getSelectedIndex() == 1) {
            sql.append("AND sub.estado = 'Activa' ");
        } else if (cmbFiltroEstado.getSelectedIndex() == 2) {
            sql.append("AND sub.estado = 'Terminada' ");
        }

        sql.append("ORDER BY sub.ultima_cita DESC ");
        sql.append("LIMIT ? OFFSET ?");

        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (Object p : parametros) {
                ps.setObject(paramIndex++, p);
            }
            ps.setInt(paramIndex++, TAMANO_PAGINA);
            ps.setInt(paramIndex, paginaActual * TAMANO_PAGINA);

            ResultSet rs = ps.executeQuery();
            int count = 0;
            while (rs.next()) {
                String cliente = rs.getString("cliente");
                String motor = rs.getString("motor") != null ? rs.getString("motor") + ", " : "";
                String puerta = motor + rs.getString("puerta_color");
                String tipo = rs.getString("tipo");
                String estado = rs.getString("estado");
                Timestamp ultima = rs.getTimestamp("ultima_cita");
                String ultimaStr = ultima != null ? ultima.toLocalDateTime().format(FMT_FECHA) : "—";

                modelo.addRow(new Object[]{cliente, puerta, tipo, estado, ultimaStr});
                idsPolizas.add(rs.getInt("id"));
                origenPolizas.add(rs.getString("origen"));
                count++;
            }
            btnPaginaSiguiente.setEnabled(count == TAMANO_PAGINA);
            btnPaginaAnterior.setEnabled(paginaActual > 0);
            int desde = paginaActual * TAMANO_PAGINA + 1;
            int hasta = paginaActual * TAMANO_PAGINA + count;
            lblPaginaInfo.setText(count == 0 ? "Sin resultados" :
                    "Mostrando " + desde + "–" + hasta + "  ·  página " + (paginaActual + 1));
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar pólizas: " + e.getMessage());
        }
    }

    private int extraerId(String itemCombo) {
        return Integer.parseInt(itemCombo.split(" - ")[0].trim());
    }

    private void limpiarFiltros() {
        cmbFiltroCliente.setSelectedIndex(0);
        cmbFiltroTipo.setSelectedIndex(0);
        cmbFiltroEstado.setSelectedIndex(0);
        paginaActual = 0;
        cargarTabla();
    }

    private void actualizarBotones() {
        boolean seleccionado = tablaPolizas.getSelectedRow() != -1;
        btnEditar.setEnabled(seleccionado);
        btnEliminar.setEnabled(seleccionado);
    }

    private void verDetallePoliza() {
        int row = tablaPolizas.getSelectedRow();
        if (row == -1) return;
        int idPoliza = idsPolizas.get(row);
        String origen = origenPolizas.get(row);
        new DialogoDetallePoliza(this, idPoliza, origen).setVisible(true);
    }

    private void abrirDialogoNuevaPoliza() {
        new DialogoNuevaPoliza(this).setVisible(true);
    }

    private void eliminarPoliza() {
        int row = tablaPolizas.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Seleccione una póliza para eliminar.");
            return;
        }
        int idPoliza = idsPolizas.get(row);
        String origen = origenPolizas.get(row);
        int confirm = JOptionPane.showConfirmDialog(this,
                "¿Eliminar esta póliza?\n(Las citas no se eliminarán)",
                "Confirmar", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            String tabla = origen.equals("6m") ? "poliza_6m" : "poliza_3m";
            String columnaId = origen.equals("6m") ? "id_poliza_6m" : "id_poliza_3m";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM " + tabla + " WHERE " + columnaId + " = ?")) {
                ps.setInt(1, idPoliza);
                ps.executeUpdate();
                cargarTabla();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Renderer de la tabla principal
    // ────────────────────────────────────────────────────────────────────────
    private class RendererPoliza extends DefaultTableCellRenderer {
        // Columna 3 = "Estado" (Activa / Terminada)
        private static final int COL_ESTADO = 3;

        // Colores para toda la fila según el estado de la póliza
        private final Color FONDO_ACTIVA = new Color(200, 230, 201);       // verde claro
        private final Color TEXTO_ACTIVA = new Color(27, 94, 32);
        private final Color FONDO_TERMINADA = new Color(255, 245, 157);    // amarillo claro
        private final Color TEXTO_TERMINADA = new Color(130, 106, 0);

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            String estado = (String) table.getValueAt(row, COL_ESTADO);

            if (isSelected) {
                label.setBackground(Estilos.AMARILLO);
                label.setForeground(Color.BLACK);
            } else if ("Activa".equals(estado)) {
                label.setBackground(FONDO_ACTIVA);
                label.setForeground(TEXTO_ACTIVA);
            } else if ("Terminada".equals(estado)) {
                label.setBackground(FONDO_TERMINADA);
                label.setForeground(TEXTO_TERMINADA);
            } else {
                label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(248, 248, 248));
                label.setForeground(Color.BLACK);
            }

            if (column == COL_ESTADO && !isSelected) {
                label.setFont(label.getFont().deriveFont(Font.BOLD));
            }

            label.setBorder(new EmptyBorder(0, 8, 0, 8));
            return label;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Diálogo de detalle de póliza
    // ────────────────────────────────────────────────────────────────────────
    private class DialogoDetallePoliza extends JDialog {
        private int idPoliza;
        private String origen;
        private JTable tablaCitas;
        private DefaultTableModel modelCitas;
        private List<Integer> idsCitas = new ArrayList<>();

        public DialogoDetallePoliza(JPanel owner, int idPoliza, String origen) {
            super(SwingUtilities.getWindowAncestor(owner), "Detalle de póliza", ModalityType.APPLICATION_MODAL);
            this.idPoliza = idPoliza;
            this.origen = origen;
            setSize(700, 400);
            setLocationRelativeTo(owner);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);
            initUI();
            cargarCitas();
        }

        private void initUI() {
            JPanel panel = new JPanel(new BorderLayout(10, 10));
            panel.setBackground(Color.WHITE);
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));

            modelCitas = new DefaultTableModel(new String[]{"ID", "Fecha/Hora", "Puerta", "Servicio", "Notas"}, 0) {
                @Override public boolean isCellEditable(int r, int c) { return false; }
            };
            tablaCitas = new JTable(modelCitas);
            tablaCitas.setRowHeight(30);
            tablaCitas.removeColumn(tablaCitas.getColumnModel().getColumn(0));
            Estilos.estilizarTabla(tablaCitas);
            JScrollPane scroll = Estilos.scrollParaTabla(tablaCitas);

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            botones.setBackground(Color.WHITE);
            JButton btnEditarCita = new JButton("Editar cita seleccionada");
            Estilos.estilizarBoton(btnEditarCita, Estilos.AMARILLO, Color.BLACK);
            btnEditarCita.addActionListener(e -> editarCita());
            JButton btnCerrar = new JButton("Cerrar");
            Estilos.estilizarBoton(btnCerrar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            btnCerrar.addActionListener(e -> dispose());
            botones.add(btnEditarCita);
            botones.add(btnCerrar);

            panel.add(scroll, BorderLayout.CENTER);
            panel.add(botones, BorderLayout.SOUTH);
            getContentPane().add(panel);
        }

        private void cargarCitas() {
            modelCitas.setRowCount(0);
            idsCitas.clear();

            List<Integer> citasIds = new ArrayList<>();
            String sqlIds;
            if (origen.equals("6m")) {
                sqlIds = "SELECT id_cita_1 AS id FROM poliza_6m WHERE id_poliza_6m = ? " +
                         "UNION SELECT id_cita_2 FROM poliza_6m WHERE id_poliza_6m = ?";
            } else {
                sqlIds = "SELECT id_cita_1 AS id FROM poliza_3m WHERE id_poliza_3m = ? " +
                         "UNION SELECT id_cita_2 FROM poliza_3m WHERE id_poliza_3m = ? " +
                         "UNION SELECT id_cita_3 FROM poliza_3m WHERE id_poliza_3m = ? " +
                         "UNION SELECT id_cita_4 FROM poliza_3m WHERE id_poliza_3m = ?";
            }
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sqlIds)) {
                int numParams = origen.equals("6m") ? 2 : 4;
                for (int i = 1; i <= numParams; i++) {
                    ps.setInt(i, idPoliza);
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    citasIds.add(rs.getInt("id"));
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al obtener citas de la póliza.");
                return;
            }

            if (citasIds.isEmpty()) return;

            StringBuilder sqlDatos = new StringBuilder(
                "SELECT c.id_cita, c.fecha_hora, pu.color, tm.nombre AS motor, ts.nombre AS servicio, c.notas " +
                "FROM cita c " +
                "JOIN puerta pu ON c.id_puerta = pu.id_puerta " +
                "LEFT JOIN tipo_motor tm ON pu.id_tipo_motor = tm.id_tipo_motor " +
                "LEFT JOIN servicio_cita sc ON c.id_cita = sc.id_cita " +
                "LEFT JOIN tipo_servicio ts ON sc.id_tipo_servicio = ts.id_tipo_servicio " +
                "WHERE c.id_cita IN ("
            );
            for (int i = 0; i < citasIds.size(); i++) {
                if (i > 0) sqlDatos.append(", ");
                sqlDatos.append("?");
            }
            sqlDatos.append(") ORDER BY c.fecha_hora");

            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sqlDatos.toString())) {
                for (int i = 0; i < citasIds.size(); i++) {
                    ps.setInt(i + 1, citasIds.get(i));
                }
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    int idCita = rs.getInt("id_cita");
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    String fecha = ts != null ? ts.toLocalDateTime().format(FMT_FECHA) : "—";
                    String motor = rs.getString("motor") != null ? rs.getString("motor") + ", " : "";
                    String puerta = motor + rs.getString("color");
                    String servicio = rs.getString("servicio") != null ? rs.getString("servicio") : "—";
                    String notas = rs.getString("notas") != null ? rs.getString("notas") : "";
                    modelCitas.addRow(new Object[]{idCita, fecha, puerta, servicio, notas});
                    idsCitas.add(idCita);
                }
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al cargar los detalles de las citas.");
            }
        }

        private void editarCita() {
            int row = tablaCitas.getSelectedRow();
            if (row == -1) {
                JOptionPane.showMessageDialog(this, "Seleccione una cita de la lista.");
                return;
            }
            int idCita = idsCitas.get(row);
            new DialogoEditarCita(this, idCita).setVisible(true);
            cargarCitas();
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Diálogo para editar una cita individual
    // ────────────────────────────────────────────────────────────────────────
    private class DialogoEditarCita extends JDialog {
        private int idCita;
        private JTextField txtFecha, txtHora;
        private JComboBox<String> cmbServicio;
        private JTextArea txtNotas;
        private JButton btnGuardar, btnCancelar;
        private List<Integer> idsServicios = new ArrayList<>();
        private List<Boolean> garantiasServicios = new ArrayList<>();

        public DialogoEditarCita(JDialog owner, int idCita) {
            super(owner, "Editar cita", ModalityType.APPLICATION_MODAL);
            this.idCita = idCita;
            setSize(400, 350);
            setLocationRelativeTo(owner);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);
            initUI();
            cargarDatos();
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
            panel.add(new JLabel("Fecha (dd/MM/yyyy):"), gbc);
            txtFecha = new JTextField(10);
            Estilos.estilizarCampo(txtFecha);
            gbc.gridx = 1; panel.add(txtFecha, gbc);

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Hora (HH:mm):"), gbc);
            txtHora = new JTextField(5);
            Estilos.estilizarCampo(txtHora);
            gbc.gridx = 1; panel.add(txtHora, gbc);

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Servicio:"), gbc);
            cmbServicio = new JComboBox<>();
            cargarServicios();
            estilizarCombo(cmbServicio);
            gbc.gridx = 1; panel.add(cmbServicio, gbc);

            gbc.gridx = 0; gbc.gridy = ++y;
            panel.add(new JLabel("Notas:"), gbc);
            txtNotas = new JTextArea(3, 20);
            txtNotas.setLineWrap(true);
            txtNotas.setBorder(new LineBorder(new Color(180, 180, 180), 1));
            JScrollPane scrollNotas = new JScrollPane(txtNotas);
            gbc.gridx = 1; panel.add(scrollNotas, gbc);

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            botones.setBackground(Color.WHITE);
            btnGuardar = new JButton("Guardar");
            btnCancelar = new JButton("Cancelar");
            Estilos.estilizarBoton(btnGuardar, Estilos.AMARILLO, Color.BLACK);
            Estilos.estilizarBoton(btnCancelar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            btnGuardar.addActionListener(e -> guardarCambios());
            btnCancelar.addActionListener(e -> dispose());
            botones.add(btnGuardar);
            botones.add(btnCancelar);
            gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 2;
            panel.add(botones, gbc);

            getContentPane().add(panel);
        }

        private void cargarServicios() {
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_tipo_servicio, nombre, garantia_anual FROM tipo_servicio ORDER BY nombre")) {
                while (rs.next()) {
                    cmbServicio.addItem(rs.getString("nombre"));
                    idsServicios.add(rs.getInt("id_tipo_servicio"));
                    garantiasServicios.add(rs.getBoolean("garantia_anual"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        private void cargarDatos() {
            String sql = "SELECT c.fecha_hora, c.notas, sc.id_tipo_servicio " +
                         "FROM cita c " +
                         "LEFT JOIN servicio_cita sc ON c.id_cita = sc.id_cita " +
                         "WHERE c.id_cita = ?";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCita);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    if (ts != null) {
                        txtFecha.setText(ts.toLocalDateTime().toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                        txtHora.setText(ts.toLocalDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                    }
                    txtNotas.setText(rs.getString("notas") != null ? rs.getString("notas") : "");
                    int idServicio = rs.getInt("id_tipo_servicio");
                    if (!rs.wasNull()) {
                        int idx = idsServicios.indexOf(idServicio);
                        if (idx >= 0) cmbServicio.setSelectedIndex(idx);
                    }
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        private void guardarCambios() {
            try {
                LocalDateTime fechaHora = LocalDateTime.parse(
                        txtFecha.getText().trim() + " " + txtHora.getText().trim(),
                        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                Timestamp ts = Timestamp.valueOf(fechaHora);

                int idServicio = idsServicios.get(cmbServicio.getSelectedIndex());
                boolean garantiaAnual = garantiasServicios.get(cmbServicio.getSelectedIndex());
                String notas = txtNotas.getText().trim();

                Timestamp vencimientoGarantia = garantiaAnual
                        ? Timestamp.valueOf(fechaHora.plusYears(1))
                        : null;

                try (Connection conn = Conexion.get()) {
                    conn.setAutoCommit(false);
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE cita SET fecha_hora=?, notas=? WHERE id_cita=?")) {
                        ps.setTimestamp(1, ts);
                        ps.setString(2, notas);
                        ps.setInt(3, idCita);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement psCheck = conn.prepareStatement(
                            "SELECT id_servicio_cita FROM servicio_cita WHERE id_cita=?")) {
                        psCheck.setInt(1, idCita);
                        ResultSet rs = psCheck.executeQuery();
                        if (rs.next()) {
                            try (PreparedStatement psUpd = conn.prepareStatement(
                                    "UPDATE servicio_cita SET id_tipo_servicio=?, fecha_vencimiento_garantia=? WHERE id_cita=?")) {
                                psUpd.setInt(1, idServicio);
                                if (vencimientoGarantia != null) psUpd.setTimestamp(2, vencimientoGarantia);
                                else psUpd.setNull(2, Types.TIMESTAMP);
                                psUpd.setInt(3, idCita);
                                psUpd.executeUpdate();
                            }
                        } else {
                            try (PreparedStatement psIns = conn.prepareStatement(
                                    "INSERT INTO servicio_cita (id_tipo_servicio, fecha_vencimiento_garantia, id_cita) VALUES (?, ?, ?)")) {
                                psIns.setInt(1, idServicio);
                                if (vencimientoGarantia != null) psIns.setTimestamp(2, vencimientoGarantia);
                                else psIns.setNull(2, Types.TIMESTAMP);
                                psIns.setInt(3, idCita);
                                psIns.executeUpdate();
                            }
                        }
                    }
                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Cita actualizada correctamente.");
                    dispose();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage());
            }
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    // Diálogo para crear una nueva póliza (con mini‑calendario y generación automática)
    // ────────────────────────────────────────────────────────────────────────
    private class DialogoNuevaPoliza extends JDialog {
        private JTextField txtFecha, txtHora;
        private JComboBox<ClienteItem> cmbCliente;
        private JComboBox<PuertaItem> cmbPuerta;
        private JComboBox<ServicioItem> cmbServicio;
        private JComboBox<String> cmbTipoPoliza;
        private JLabel lblMotor, lblPreview;
        private JButton btnGuardar, btnCancelar;

        private YearMonth mesActual = YearMonth.now();
        private JPanel gridDias;
        private JLabel lblMesAnio;
        private LocalDate fechaSeleccionada = LocalDate.now();

        private static class ClienteItem {
            int id; String texto;
            ClienteItem(int id, String texto) { this.id = id; this.texto = texto; }
            @Override public String toString() { return texto; }
        }
        private static class PuertaItem {
            int idPuerta; int idDireccion; Integer idTipoMotor; String tipoMotorNombre; String texto;
            PuertaItem(int idPuerta, int idDireccion, Integer idTipoMotor, String tipoMotorNombre, String texto) {
                this.idPuerta = idPuerta; this.idDireccion = idDireccion;
                this.idTipoMotor = idTipoMotor; this.tipoMotorNombre = tipoMotorNombre; this.texto = texto;
            }
            @Override public String toString() { return texto; }
        }
        private static class ServicioItem {
            int id; String texto;
            ServicioItem(int id, String texto) { this.id = id; this.texto = texto; }
            @Override public String toString() { return texto; }
        }

        public DialogoNuevaPoliza(JPanel owner) {
            super(SwingUtilities.getWindowAncestor(owner), "Nueva póliza", ModalityType.APPLICATION_MODAL);
            initUI();
            cargarClientes();
            cargarServicios();
            cargarTodasLasPuertas();
            actualizarFecha();
            setSize(820, 650);
            setLocationRelativeTo(owner);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);
            getRootPane().setDefaultButton(btnGuardar);
        }

        private void initUI() {
            JPanel panel = new JPanel(new BorderLayout(15, 15));
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            panel.setBackground(Color.WHITE);

            JPanel panelCalendario = crearPanelCalendario();
            panelCalendario.setPreferredSize(new Dimension(300, 0));

            JPanel panelForm = new JPanel(new GridBagLayout());
            panelForm.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.WEST;

            int y = 0;

            gbc.gridx = 0; gbc.gridy = y;
            panelForm.add(new JLabel("Fecha:"), gbc);
            txtFecha = new JTextField(12);
            txtFecha.setEditable(false);
            txtFecha.setBackground(new Color(245, 245, 245));
            Estilos.estilizarCampo(txtFecha);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(txtFecha, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelForm.add(new JLabel("Hora:"), gbc);
            txtHora = new JTextField("09:00", 6);
            Estilos.estilizarCampo(txtHora);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(txtHora, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelForm.add(new JLabel("Cliente: *"), gbc);
            cmbCliente = new JComboBox<>();
            estilizarCombo(cmbCliente);
            cmbCliente.addActionListener(e -> {
                if (cmbCliente.getSelectedItem() != null) cargarPuertasDelCliente();
                else cargarTodasLasPuertas();
                actualizarPreview();
            });
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(cmbCliente, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelForm.add(new JLabel("Puerta: *"), gbc);
            cmbPuerta = new JComboBox<>();
            estilizarCombo(cmbPuerta);
            cmbPuerta.addActionListener(e -> { actualizarMotor(); actualizarPreview(); });
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(cmbPuerta, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelForm.add(new JLabel("Tipo motor:"), gbc);
            lblMotor = new JLabel("—");
            lblMotor.setFont(new Font("SansSerif", Font.ITALIC, 13));
            lblMotor.setForeground(new Color(90, 90, 90));
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(lblMotor, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelForm.add(new JLabel("Servicio: *"), gbc);
            cmbServicio = new JComboBox<>();
            estilizarCombo(cmbServicio);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(cmbServicio, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelForm.add(new JLabel("Tipo póliza:"), gbc);
            cmbTipoPoliza = new JComboBox<>(new String[]{"3 meses", "6 meses"});
            estilizarCombo(cmbTipoPoliza);
            cmbTipoPoliza.addActionListener(e -> actualizarPreview());
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(cmbTipoPoliza, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelForm.add(new JLabel("Citas generadas:"), gbc);
            lblPreview = new JLabel("<html><i>(Seleccione fecha, hora y puerta)</i></html>");
            lblPreview.setFont(new Font("SansSerif", Font.PLAIN, 12));
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelForm.add(lblPreview, gbc);
            gbc.gridwidth = 1;

            JPanel botones = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
            botones.setBackground(Color.WHITE);
            btnGuardar = new JButton("Guardar");
            btnCancelar = new JButton("Cancelar");
            Estilos.estilizarBoton(btnGuardar, Estilos.AMARILLO, Color.BLACK);
            Estilos.estilizarBoton(btnCancelar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
            btnGuardar.addActionListener(e -> guardarPoliza());
            btnCancelar.addActionListener(e -> dispose());
            botones.add(btnGuardar);
            botones.add(btnCancelar);
            gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 3;
            panelForm.add(botones, gbc);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelCalendario, panelForm);
            split.setResizeWeight(0.4);
            split.setDividerSize(6);
            split.setBorder(null);
            panel.add(split, BorderLayout.CENTER);

            getContentPane().add(panel);
        }

        private JPanel crearPanelCalendario() {
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.setBackground(Color.WHITE);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    new LineBorder(new Color(200, 200, 200), 1),
                    new EmptyBorder(6, 6, 6, 6)
            ));

            JPanel nav = new JPanel(new BorderLayout());
            nav.setBackground(Estilos.FONDO_OSCURO);
            nav.setBorder(new EmptyBorder(6, 8, 6, 8));

            JButton btnAnt = Estilos.botonFlecha(false);
            JButton btnSig = Estilos.botonFlecha(true);
            btnAnt.addActionListener(e -> { mesActual = mesActual.minusMonths(1); actualizarCalendario(); });
            btnSig.addActionListener(e -> { mesActual = mesActual.plusMonths(1); actualizarCalendario(); });

            lblMesAnio = new JLabel("", SwingConstants.CENTER);
            lblMesAnio.setFont(new Font("SansSerif", Font.BOLD, 14));
            lblMesAnio.setForeground(Color.WHITE);

            nav.add(btnAnt, BorderLayout.WEST);
            nav.add(lblMesAnio, BorderLayout.CENTER);
            nav.add(btnSig, BorderLayout.EAST);

            JPanel encDias = new JPanel(new GridLayout(1, 7, 2, 2));
            encDias.setBackground(Color.WHITE);
            encDias.setBorder(new EmptyBorder(6, 4, 4, 4));
            for (String d : new String[]{"Lu", "Ma", "Mi", "Ju", "Vi", "Sá", "Do"}) {
                JLabel lbl = new JLabel(d, SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", Font.BOLD, 11));
                lbl.setForeground(new Color(80, 80, 80));
                encDias.add(lbl);
            }

            gridDias = new JPanel(new GridLayout(0, 7, 3, 3));
            gridDias.setBackground(Color.WHITE);
            gridDias.setBorder(new EmptyBorder(4, 4, 4, 4));

            JPanel body = new JPanel(new BorderLayout());
            body.setBackground(Color.WHITE);
            body.add(encDias, BorderLayout.NORTH);
            body.add(gridDias, BorderLayout.CENTER);

            panel.add(nav, BorderLayout.NORTH);
            panel.add(body, BorderLayout.CENTER);

            actualizarCalendario();
            return panel;
        }

        private void actualizarCalendario() {
            String mesNombre = mesActual.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "MX"));
            lblMesAnio.setText(mesNombre.substring(0, 1).toUpperCase() + mesNombre.substring(1) + " " + mesActual.getYear());

            gridDias.removeAll();
            LocalDate primerDia = mesActual.atDay(1);
            int offset = primerDia.getDayOfWeek().getValue() - 1;
            for (int i = 0; i < offset; i++) gridDias.add(new JLabel());

            LocalDate hoy = LocalDate.now();
            int diasEnMes = mesActual.lengthOfMonth();

            for (int d = 1; d <= diasEnMes; d++) {
                final LocalDate fecha = mesActual.atDay(d);
                boolean esHoy = fecha.equals(hoy);
                boolean esSeleccionada = fecha.equals(fechaSeleccionada);

                JPanel celda = new JPanel(new BorderLayout());
                celda.setBackground(esSeleccionada ? Estilos.AMARILLO : (esHoy ? new Color(255, 240, 200) : Color.WHITE));
                celda.setBorder(new LineBorder(new Color(230, 230, 230), 1));
                celda.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                JLabel lbl = new JLabel(String.valueOf(d), SwingConstants.CENTER);
                lbl.setFont(new Font("SansSerif", esSeleccionada ? Font.BOLD : Font.PLAIN, 13));
                lbl.setForeground(esSeleccionada ? Color.BLACK : Color.DARK_GRAY);
                celda.add(lbl, BorderLayout.CENTER);

                celda.addMouseListener(new MouseAdapter() {
                    @Override public void mouseClicked(MouseEvent e) {
                        fechaSeleccionada = fecha;
                        actualizarFecha();
                        actualizarCalendario();
                        actualizarPreview();
                    }
                    @Override public void mouseEntered(MouseEvent e) {
                        if (!esSeleccionada) celda.setBackground(new Color(255, 245, 225));
                    }
                    @Override public void mouseExited(MouseEvent e) {
                        if (!esSeleccionada) celda.setBackground(esHoy ? new Color(255, 240, 200) : Color.WHITE);
                    }
                });
                gridDias.add(celda);
            }

            gridDias.revalidate();
            gridDias.repaint();
        }

        private void actualizarFecha() {
            txtFecha.setText(fechaSeleccionada.toString());
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
            cmbCliente.setSelectedItem(null);
        }

        private void cargarTodasLasPuertas() {
            cmbPuerta.removeAllItems();
            String sql = "SELECT p.id_puerta, p.id_direccion, p.color, p.id_tipo_motor, tm.nombre AS motor_nombre, " +
                         "d.calle, d.numero " +
                         "FROM puerta p " +
                         "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                         "LEFT JOIN tipo_motor tm ON p.id_tipo_motor = tm.id_tipo_motor " +
                         "ORDER BY p.id_puerta";
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    Integer idMotor = rs.getObject("id_tipo_motor") != null ? rs.getInt("id_tipo_motor") : null;
                    String motor = rs.getString("motor_nombre");
                    String texto = (motor != null ? motor + ", " : "") + rs.getString("color") +
                            " (" + rs.getString("calle") + " " + rs.getString("numero") + ")";
                    cmbPuerta.addItem(new PuertaItem(rs.getInt("id_puerta"), rs.getInt("id_direccion"),
                            idMotor, motor, texto));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            if (cmbPuerta.getItemCount() > 0) cmbPuerta.setSelectedIndex(0);
            actualizarMotor();
        }

        private void cargarPuertasDelCliente() {
            cmbPuerta.removeAllItems();
            ClienteItem cliente = (ClienteItem) cmbCliente.getSelectedItem();
            if (cliente == null) { cargarTodasLasPuertas(); return; }
            String sql = "SELECT p.id_puerta, p.id_direccion, p.color, p.id_tipo_motor, tm.nombre AS motor_nombre, " +
                         "d.calle, d.numero " +
                         "FROM puerta p " +
                         "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                         "LEFT JOIN tipo_motor tm ON p.id_tipo_motor = tm.id_tipo_motor " +
                         "WHERE d.id_cliente = ? " +
                         "ORDER BY p.id_puerta";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, cliente.id);
                ResultSet rs = ps.executeQuery();
                boolean hayPuertas = false;
                while (rs.next()) {
                    hayPuertas = true;
                    Integer idMotor = rs.getObject("id_tipo_motor") != null ? rs.getInt("id_tipo_motor") : null;
                    String motor = rs.getString("motor_nombre");
                    String texto = (motor != null ? motor + ", " : "") + rs.getString("color") +
                            " (" + rs.getString("calle") + " " + rs.getString("numero") + ")";
                    cmbPuerta.addItem(new PuertaItem(rs.getInt("id_puerta"), rs.getInt("id_direccion"),
                            idMotor, motor, texto));
                }
                if (!hayPuertas) {
                    JOptionPane.showMessageDialog(this,
                            "Este cliente no tiene puertas registradas.\nAgregue una puerta desde la pestaña Puertas.",
                            "Sin puertas", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            if (cmbPuerta.getItemCount() > 0) cmbPuerta.setSelectedIndex(0);
            actualizarMotor();
        }

        private void cargarServicios() {
            cmbServicio.removeAllItems();
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_tipo_servicio, nombre FROM tipo_servicio ORDER BY nombre")) {
                while (rs.next()) {
                    cmbServicio.addItem(new ServicioItem(rs.getInt("id_tipo_servicio"), rs.getString("nombre")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            cmbServicio.setSelectedItem(null);
        }

        private void actualizarMotor() {
            PuertaItem puerta = (PuertaItem) cmbPuerta.getSelectedItem();
            if (puerta == null) lblMotor.setText("—");
            else if (puerta.idTipoMotor == null) lblMotor.setText("Sin motor asignado");
            else lblMotor.setText(puerta.tipoMotorNombre);
        }

        private void actualizarPreview() {
            PuertaItem puerta = (PuertaItem) cmbPuerta.getSelectedItem();
            if (puerta == null || txtHora.getText().trim().isEmpty()) {
                lblPreview.setText("<html><i>(Seleccione fecha, hora y puerta)</i></html>");
                return;
            }

            try {
                LocalTime hora = LocalTime.parse(txtHora.getText().trim());
                LocalDateTime primerCita = LocalDateTime.of(fechaSeleccionada, hora);

                int intervaloMeses = cmbTipoPoliza.getSelectedIndex() == 0 ? 3 : 6;
                int numCitas = intervaloMeses == 3 ? 4 : 2;

                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                StringBuilder sb = new StringBuilder("<html>");
                for (int i = 0; i < numCitas; i++) {
                    LocalDateTime cita = primerCita.plusMonths(i * intervaloMeses);
                    if (i > 0) sb.append("<br>");
                    sb.append("• ").append(cita.format(fmt));
                }
                sb.append("</html>");
                lblPreview.setText(sb.toString());
            } catch (Exception e) {
                lblPreview.setText("<html><i>Hora no válida</i></html>");
            }
        }

        private void guardarPoliza() {
            ClienteItem cliente = (ClienteItem) cmbCliente.getSelectedItem();
            PuertaItem puerta = (PuertaItem) cmbPuerta.getSelectedItem();
            ServicioItem servicio = (ServicioItem) cmbServicio.getSelectedItem();

            if (cliente == null || puerta == null || servicio == null || txtHora.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Complete todos los campos obligatorios (*).");
                return;
            }

            try {
                LocalTime hora = LocalTime.parse(txtHora.getText().trim());
                LocalDateTime primerCita = LocalDateTime.of(fechaSeleccionada, hora);

                int intervalo = cmbTipoPoliza.getSelectedIndex() == 0 ? 3 : 6;
                int numCitas = intervalo == 3 ? 4 : 2;

                String notaAuto = "Cita creada a partir de póliza de " + (intervalo == 3 ? "3 meses" : "6 meses");
                List<Integer> idsCitas = new ArrayList<>();

                try (Connection conn = Conexion.get()) {
                    conn.setAutoCommit(false);

                    for (int i = 0; i < numCitas; i++) {
                        LocalDateTime fechaCita = primerCita.plusMonths(i * intervalo);
                        Timestamp ts = Timestamp.valueOf(fechaCita);

                        String sqlCita = "INSERT INTO cita (fecha_hora, notas, id_puerta, id_usuario) VALUES (?, ?, ?, ?) RETURNING id_cita";
                        int idCita;
                        try (PreparedStatement ps = conn.prepareStatement(sqlCita)) {
                            ps.setTimestamp(1, ts);
                            String notas = (i == 0) ? "" : notaAuto;
                            ps.setString(2, notas);
                            ps.setInt(3, puerta.idPuerta);
                            ps.setInt(4, idUsuarioActual);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) idCita = rs.getInt(1);
                            else throw new SQLException("No se pudo obtener id de cita");
                        }

                        String sqlServ = "INSERT INTO servicio_cita (id_tipo_servicio, id_cita) VALUES (?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(sqlServ)) {
                            ps.setInt(1, servicio.id);
                            ps.setInt(2, idCita);
                            ps.executeUpdate();
                        }

                        idsCitas.add(idCita);
                    }

                    String tabla = intervalo == 3 ? "poliza_3m" : "poliza_6m";
                    String columnas = intervalo == 3 ?
                        "(id_cita_1, id_cita_2, id_cita_3, id_cita_4, id_cliente)" :
                        "(id_cita_1, id_cita_2, id_cliente)";
                    String placeholders = intervalo == 3 ? "(?,?,?,?,?)" : "(?,?,?)";

                    String sqlPoliza = "INSERT INTO " + tabla + " " + columnas + " VALUES " + placeholders;
                    try (PreparedStatement ps = conn.prepareStatement(sqlPoliza)) {
                        for (int i = 0; i < idsCitas.size(); i++) {
                            ps.setInt(i + 1, idsCitas.get(i));
                        }
                        ps.setInt(idsCitas.size() + 1, cliente.id);
                        ps.executeUpdate();
                    }

                    conn.commit();
                    JOptionPane.showMessageDialog(this, "Póliza y citas creadas correctamente.");
                    dispose();
                    cargarTabla();
                }
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "La hora no es válida. Use formato HH:mm.");
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
            }
        }

        private void estilizarCombo(JComboBox<?> combo) {
            combo.setBackground(Color.WHITE);
            combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            combo.setBorder(new LineBorder(new Color(180, 180, 180), 1));
        }
    }
}