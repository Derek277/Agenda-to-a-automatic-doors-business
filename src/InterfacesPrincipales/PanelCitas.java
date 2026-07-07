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
import java.util.*;
import java.util.List;
import java.util.Date;

public class PanelCitas extends JPanel {

    private JTable tablaCitas;
    private DefaultTableModel modelo;
    private JButton btnNuevo, btnEditar, btnEliminar, btnRefrescar;
    private JButton btnFiltrar, btnLimpiar;
    private JButton btnPaginaAnterior, btnPaginaSiguiente;
    private JSpinner spnFechaDesde, spnFechaHasta;
    private JCheckBox chkUsarFechaDesde, chkUsarFechaHasta;
    private JComboBox<String> cmbFiltroCliente, cmbFiltroPuerta, cmbFiltroServicio;
    private JComboBox<String> cmbOrden;
    private JLabel lblPaginaInfo;

    private List<Integer> idsCitas = new ArrayList<>();

    private int paginaActual = 0;
    private final int TAMANO_PAGINA = 50;
    private final int idUsuarioActual;

    public PanelCitas(int idUsuario) {
        this.idUsuarioActual = idUsuario;
        setLayout(new BorderLayout(0, 10));
        setBorder(new EmptyBorder(10, 10, 10, 10));
        initComponents();
        cargarCombosFiltro();
         configurarFiltroInicial();
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

        // ── Fila 1 ──
        int y = 0, col = 0;

        chkUsarFechaDesde = new JCheckBox("Desde:");
        chkUsarFechaDesde.setBackground(Color.WHITE);
        gbc.gridx = col++; gbc.gridy = y;
        panelFiltros.add(chkUsarFechaDesde, gbc);

        spnFechaDesde = crearSpinnerFecha();
        gbc.gridx = col++; panelFiltros.add(spnFechaDesde, gbc);

        chkUsarFechaHasta = new JCheckBox("Hasta:");
        chkUsarFechaHasta.setBackground(Color.WHITE);
        gbc.gridx = col++; panelFiltros.add(chkUsarFechaHasta, gbc);

        spnFechaHasta = crearSpinnerFecha();
        gbc.gridx = col++; panelFiltros.add(spnFechaHasta, gbc);

        JButton btnProximos30 = new JButton("Próximos 30 días");
        btnProximos30.setToolTipText("Rango desde hoy hasta dentro de 30 días.");
        Estilos.estilizarBoton(btnProximos30, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        btnProximos30.addActionListener(e -> {
            chkUsarFechaDesde.setSelected(true);
            chkUsarFechaHasta.setSelected(true);
            spnFechaDesde.setValue(java.sql.Date.valueOf(LocalDate.now()));
            spnFechaHasta.setValue(java.sql.Date.valueOf(LocalDate.now().plusDays(30)));
        });
        gbc.gridx = col++; panelFiltros.add(btnProximos30, gbc);

        // Ordenar: etiqueta y combo en panel juntos
        JPanel panelOrden = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelOrden.setOpaque(false);
        panelOrden.add(new JLabel("Ordenar:"));
        cmbOrden = new JComboBox<>(new String[]{
                "Fecha (más antiguas primero)",
                "Fecha (próximas primero)",
                "Cliente A-Z",
                "Cliente Z-A"
        });
        estilizarCombo(cmbOrden);
        panelOrden.add(cmbOrden);
        gbc.gridx = col++; gbc.gridwidth = 1;
        panelFiltros.add(panelOrden, gbc);

        // ── Fila 2 ──
        y = 1; col = 0;
        gbc.gridwidth = 1;

        gbc.gridx = col++; gbc.gridy = y;
        panelFiltros.add(new JLabel("Cliente:"), gbc);
        cmbFiltroCliente = new JComboBox<>();
        cmbFiltroCliente.addItem("-- Todos los clientes --");
        estilizarCombo(cmbFiltroCliente);
        gbc.gridx = col++; panelFiltros.add(cmbFiltroCliente, gbc);

        gbc.gridx = col++; panelFiltros.add(new JLabel("Puerta:"), gbc);
        cmbFiltroPuerta = new JComboBox<>();
        cmbFiltroPuerta.addItem("-- Todas las puertas --");
        estilizarCombo(cmbFiltroPuerta);
        gbc.gridx = col++; panelFiltros.add(cmbFiltroPuerta, gbc);

        // Servicio: etiqueta y combo juntos
        gbc.gridx = col++;
        JPanel panelServicio = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        panelServicio.setOpaque(false);
        panelServicio.add(new JLabel("Servicio:"));
        cmbFiltroServicio = new JComboBox<>();
        cmbFiltroServicio.addItem("-- Todos los servicios --");
        estilizarCombo(cmbFiltroServicio);
        panelServicio.add(cmbFiltroServicio);
        panelFiltros.add(panelServicio, gbc);

        // Botones Limpiar / Filtrar (mismo tamaño, orden invertido)
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

        btnNuevo = new JButton("+ Nueva cita");
        btnEditar = new JButton("Editar");
        btnEliminar = new JButton("Eliminar");
        btnRefrescar = new JButton("Refrescar");                     // ← nuevo botón

        Estilos.estilizarBoton(btnNuevo, Estilos.AMARILLO, Color.BLACK);
        Estilos.estilizarBoton(btnEditar, Estilos.BOTON_SECUNDARIO, Color.WHITE);
        Estilos.estilizarBoton(btnEliminar, Estilos.ROJO_PELIGRO, Color.WHITE);
        Estilos.estilizarBoton(btnRefrescar, Estilos.AMARILLO, Color.BLACK); // ← amarillo

        btnNuevo.addActionListener(e -> abrirDialogoCita(null));
        btnEditar.addActionListener(e -> abrirDialogoCita(obtenerIdSeleccionado()));
        btnEliminar.addActionListener(e -> eliminarCita());
        btnRefrescar.addActionListener(e -> cargarTabla());         // ← acción

        toolBar.add(btnNuevo);
        toolBar.add(btnEditar);
        toolBar.add(btnEliminar);
        toolBar.add(btnRefrescar);                                   // ← añadido

        // Tabla
        modelo = new DefaultTableModel(
                new String[]{"Fecha/Hora", "Cliente", "Puerta", "Dirección", "Servicio", "Notas"}, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
        };
        tablaCitas = new JTable(modelo);
        tablaCitas.setRowHeight(30);
        tablaCitas.setShowGrid(false);
        tablaCitas.setIntercellSpacing(new Dimension(0, 0));
        tablaCitas.setDefaultRenderer(Object.class, new RendererTablaCitas());
        Estilos.estilizarTabla(tablaCitas);
        tablaCitas.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) actualizarBotones();
        });
        tablaCitas.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tablaCitas.getSelectedRow() != -1) {
                    abrirDialogoCita(obtenerIdSeleccionado());
                }
            }
        });

        JScrollPane scrollTabla = Estilos.scrollParaTabla(tablaCitas);

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

        // Layout
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

        // Cliente → Filtro dinámico de puertas
        cmbFiltroCliente.addActionListener(e -> actualizarPuertasFiltro());
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
    
    private void eliminarCita() {
        Integer id = obtenerIdSeleccionado();
        if (id == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una cita para eliminar.");
            return;
        }

        // Verificar si la cita pertenece a alguna póliza
        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT '6m' AS origen FROM poliza_6m WHERE ? IN (id_cita_1, id_cita_2) " +
                 "UNION SELECT '3m' FROM poliza_3m WHERE ? IN (id_cita_1, id_cita_2, id_cita_3, id_cita_4)")) {
            ps.setInt(1, id);
            ps.setInt(2, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String tipo = rs.getString("origen");
                JOptionPane.showMessageDialog(this,
                        "No se puede eliminar esta cita porque pertenece a una póliza " +
                        (tipo.equals("6m") ? "de 6 meses" : "de 3 meses") + ".\n" +
                        "Elimine primero la póliza o quite la cita de la misma.",
                        "Cita asociada a póliza", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al verificar pólizas: " + e.getMessage());
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this, "¿Eliminar la cita seleccionada?", "Confirmar",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = Conexion.get()) {
                conn.setAutoCommit(false);
                // Primero eliminar servicio_cita asociado (si existe)
                try (PreparedStatement psServ = conn.prepareStatement("DELETE FROM servicio_cita WHERE id_cita = ?")) {
                    psServ.setInt(1, id);
                    psServ.executeUpdate();
                }
                // Luego eliminar la cita
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM cita WHERE id_cita = ?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                }
                conn.commit();
                cargarTabla();
            } catch (SQLException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al eliminar: " + e.getMessage());
            }
        }
    }
    
    private void configurarFiltroInicial() {
    chkUsarFechaDesde.setSelected(true);
    spnFechaDesde.setValue(java.sql.Date.valueOf(LocalDate.now()));
    // El orden ya está en “Fecha (próximas primero)” por defecto
}

    private JSpinner crearSpinnerFecha() {
        JSpinner spinner = new JSpinner(new SpinnerDateModel());
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(110, 28));
        spinner.setValue(java.sql.Date.valueOf(LocalDate.now()));
        return spinner;
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
             ResultSet rs = st.executeQuery("SELECT id_cliente, nombre FROM cliente ORDER BY nombre")) {
            while (rs.next()) {
                cmbFiltroCliente.addItem(rs.getInt("id_cliente") + " - " + rs.getString("nombre"));
            }
        } catch (SQLException e) { e.printStackTrace(); }

        // Puertas (carga inicial completa)
        cargarTodasLasPuertasEnFiltro();

        // Servicios
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT id_tipo_servicio, nombre FROM tipo_servicio")) {
            while (rs.next()) {
                cmbFiltroServicio.addItem(rs.getInt("id_tipo_servicio") + " - " + rs.getString("nombre"));
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void cargarTodasLasPuertasEnFiltro() {
        cmbFiltroPuerta.removeAllItems();
        cmbFiltroPuerta.addItem("-- Todas las puertas --");
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT p.id_puerta, tm.nombre AS motor, p.color, d.calle, d.numero " +
                     "FROM puerta p " +
                     "LEFT JOIN tipo_motor tm ON p.id_tipo_motor = tm.id_tipo_motor " +
                     "LEFT JOIN direccion d ON p.id_direccion = d.id_direccion")) {
            while (rs.next()) {
                String motor = rs.getString("motor") != null ? rs.getString("motor") + ", " : "";
                cmbFiltroPuerta.addItem(rs.getInt("id_puerta") + " - " + motor + rs.getString("color") +
                        " (" + rs.getString("calle") + " " + rs.getString("numero") + ")");
            }
        } catch (SQLException e) { e.printStackTrace(); }
    }

    private void actualizarPuertasFiltro() {
        int selectedIndex = cmbFiltroCliente.getSelectedIndex();
        if (selectedIndex <= 0) {
            cargarTodasLasPuertasEnFiltro();
        } else {
            int idCliente = extraerId(cmbFiltroCliente.getSelectedItem().toString());
            cmbFiltroPuerta.removeAllItems();
            cmbFiltroPuerta.addItem("-- Todas las puertas --");
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(
                         "SELECT p.id_puerta, tm.nombre AS motor, p.color, d.calle, d.numero " +
                         "FROM puerta p " +
                         "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                         "LEFT JOIN tipo_motor tm ON p.id_tipo_motor = tm.id_tipo_motor " +
                         "WHERE d.id_cliente = ?")) {
                ps.setInt(1, idCliente);
                ResultSet rs = ps.executeQuery();
                while (rs.next()) {
                    String motor = rs.getString("motor") != null ? rs.getString("motor") + ", " : "";
                    cmbFiltroPuerta.addItem(rs.getInt("id_puerta") + " - " + motor + rs.getString("color") +
                            " (" + rs.getString("calle") + " " + rs.getString("numero") + ")");
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    private void cargarTabla() {
        modelo.setRowCount(0);
        idsCitas.clear();

        StringBuilder sql = new StringBuilder(
            "SELECT c.id_cita, c.fecha_hora, cl.nombre AS cliente, " +
            "tp.nombre AS tipo_puerta, p.color AS puerta_color, " +
            "d.calle, d.numero, d.colonia, ts.nombre AS servicio, c.notas " +
            "FROM cita c " +
            "JOIN puerta p ON c.id_puerta = p.id_puerta " +
            "JOIN direccion d ON p.id_direccion = d.id_direccion " +
            "JOIN cliente cl ON d.id_cliente = cl.id_cliente " +
            "LEFT JOIN servicio_cita sc ON c.id_cita = sc.id_cita " +
            "LEFT JOIN tipo_servicio ts ON sc.id_tipo_servicio = ts.id_tipo_servicio " +
            "LEFT JOIN tipo_puerta tp ON p.id_tipo_puerta = tp.id_tipo_puerta " +
            "WHERE 1=1 "
        );
        List<Object> parametros = new ArrayList<>();

        if (chkUsarFechaDesde.isSelected()) {
            LocalDate desde = obtenerFechaDeSpinner(spnFechaDesde);
            sql.append("AND c.fecha_hora >= ? ");
            parametros.add(Timestamp.valueOf(desde.atStartOfDay()));
        }
        if (chkUsarFechaHasta.isSelected()) {
            LocalDate hasta = obtenerFechaDeSpinner(spnFechaHasta);
            sql.append("AND c.fecha_hora <= ? ");
            parametros.add(Timestamp.valueOf(hasta.atTime(23, 59, 59)));
        }
        if (cmbFiltroCliente.getSelectedIndex() > 0) {
            int idCliente = extraerId(cmbFiltroCliente.getSelectedItem().toString());
            sql.append("AND cl.id_cliente = ? ");
            parametros.add(idCliente);
        }
        if (cmbFiltroPuerta.getSelectedIndex() > 0) {
            int idPuerta = extraerId(cmbFiltroPuerta.getSelectedItem().toString());
            sql.append("AND p.id_puerta = ? ");
            parametros.add(idPuerta);
        }
        if (cmbFiltroServicio.getSelectedIndex() > 0) {
            int idServicio = extraerId(cmbFiltroServicio.getSelectedItem().toString());
            sql.append("AND ts.id_tipo_servicio = ? ");
            parametros.add(idServicio);
        }

        int orden = cmbOrden.getSelectedIndex();
switch (orden) {
    case 0 -> sql.append("ORDER BY c.fecha_hora ASC ");   // más antiguas primero
    case 1 -> sql.append("ORDER BY c.fecha_hora DESC ");    // más próximas primero
    case 2 -> sql.append("ORDER BY cl.nombre ASC, c.fecha_hora ASC ");
    case 3 -> sql.append("ORDER BY cl.nombre DESC, c.fecha_hora ASC ");
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
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    String fecha = ts != null ?
                            ts.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "—";
                    String cliente = rs.getString("cliente");

                    String tipoPuerta = rs.getString("tipo_puerta");
                    String color = rs.getString("puerta_color");
                    String puerta = (tipoPuerta != null ? tipoPuerta + ", " : "") + color;

                    String direccion = rs.getString("calle") + " " + rs.getString("numero") + ", " + rs.getString("colonia");
                    String servicio = rs.getString("servicio") != null ? rs.getString("servicio") : "—";
                    String notas = rs.getString("notas") != null ? rs.getString("notas") : "";

                    modelo.addRow(new Object[]{fecha, cliente, puerta, direccion, servicio, notas});
                    idsCitas.add(rs.getInt("id_cita"));
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
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + e.getMessage());
        }
    }

    private LocalDate obtenerFechaDeSpinner(JSpinner spinner) {
        Date fecha = (Date) spinner.getValue();
        return fecha.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate();
    }

    private int extraerId(String itemCombo) {
        return Integer.parseInt(itemCombo.split(" - ")[0].trim());
    }

    private void limpiarFiltros() {
        chkUsarFechaDesde.setSelected(false);
        chkUsarFechaHasta.setSelected(false);
        spnFechaDesde.setValue(java.sql.Date.valueOf(LocalDate.now()));
        spnFechaHasta.setValue(java.sql.Date.valueOf(LocalDate.now()));
        cmbFiltroCliente.setSelectedIndex(0);
        cmbFiltroPuerta.setSelectedIndex(0);
        cmbFiltroServicio.setSelectedIndex(0);
        cmbOrden.setSelectedIndex(0);
        paginaActual = 0;
        cargarTabla();
    }

    private void abrirDialogoCita(Integer idCita) {
        DialogoCita dialog = new DialogoCita(this, idCita, idUsuarioActual);
        dialog.setVisible(true);
        if (dialog.isGuardadoExitoso()) {
            cargarTabla();
        }
    }

    private Integer obtenerIdSeleccionado() {
        int row = tablaCitas.getSelectedRow();
        if (row == -1 || row >= idsCitas.size()) return null;
        return idsCitas.get(row);
    }

    

    private void actualizarBotones() {
        boolean seleccionado = tablaCitas.getSelectedRow() != -1;
        btnEditar.setEnabled(seleccionado);
        btnEliminar.setEnabled(seleccionado);
    }

    private class RendererTablaCitas extends DefaultTableCellRenderer {
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

            label.setBorder(column == 0 ? new EmptyBorder(0, 8, 0, 0) : new EmptyBorder(0, 8, 0, 8));

            if (column == 0 && row > 0) {
                String fechaActual = (String) table.getValueAt(row, 0);
                String fechaAnterior = (String) table.getValueAt(row - 1, 0);
                if (fechaActual != null && fechaAnterior != null) {
                    String diaActual = fechaActual.split(" ")[0];
                    String diaAnt = fechaAnterior.split(" ")[0];
                    if (!diaActual.equals(diaAnt)) {
                        label.setBorder(BorderFactory.createCompoundBorder(
                                new LineBorder(new Color(180, 180, 180), 1, true),
                                new EmptyBorder(4, 8, 4, 8)
                        ));
                    }
                }
            }
            return label;
        }
    }

    // ─── DIÁLOGO DE CITA ────────────────────────────────────────────────────

    private static class DialogoCita extends JDialog {
        private final Integer idCitaEditar;
        private final int idUsuario;
        private boolean guardadoExitoso = false;

        private JTextField txtFecha, txtHora;
        private JComboBox<ClienteItem> cmbCliente;
        private JComboBox<PuertaItem> cmbPuerta;
        private JComboBox<ServicioItem> cmbTipoServicio;
        private JLabel lblTipoMotor;
        private JLabel lblGarantia;
        private JTextArea txtNotas;
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
            int id; String texto; boolean garantiaAnual;
            ServicioItem(int id, String texto, boolean garantiaAnual) {
                this.id = id; this.texto = texto; this.garantiaAnual = garantiaAnual;
            }
            @Override public String toString() { return texto; }
        }

        public DialogoCita(PanelCitas padre, Integer idCita, int idUsuario) {
            super(SwingUtilities.getWindowAncestor(padre),
                    (idCita == null) ? "Nueva cita" : "Editar cita", ModalityType.APPLICATION_MODAL);
            this.idCitaEditar = idCita;
            this.idUsuario = idUsuario;
            initUI();
            cargarClientes();
            cargarTodasLasPuertas();
            if (idCitaEditar != null) cargarDatos();
            setSize(780, 650);  // más grande
            setLocationRelativeTo(padre);
            setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            getContentPane().setBackground(Color.WHITE);
            getRootPane().setDefaultButton(btnGuardar); // Enter → Guardar
        }

        private void initUI() {
            JPanel panel = new JPanel(new BorderLayout(15, 15));
            panel.setBorder(new EmptyBorder(15, 15, 15, 15));
            panel.setBackground(Color.WHITE);

            JPanel panelCalendario = crearPanelCalendario();
            panelCalendario.setPreferredSize(new Dimension(300, 0));

            JPanel panelFormulario = new JPanel(new GridBagLayout());
            panelFormulario.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(6, 8, 6, 8);
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int y = 0;

            gbc.gridx = 0; gbc.gridy = y;
            panelFormulario.add(new JLabel("Fecha:"), gbc);
            txtFecha = new JTextField(12);
            txtFecha.setEditable(false);
            txtFecha.setBackground(new Color(245, 245, 245));
            Estilos.estilizarCampo(txtFecha);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelFormulario.add(txtFecha, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelFormulario.add(new JLabel("Hora:"), gbc);
            txtHora = new JTextField(6);
            txtHora.setText("09:00");
            Estilos.estilizarCampo(txtHora);
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelFormulario.add(txtHora, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelFormulario.add(new JLabel("Cliente: *"), gbc);
            cmbCliente = new JComboBox<>();
            estilizarCombo(cmbCliente);
            cmbCliente.addActionListener(e -> {
                if (cmbCliente.getSelectedItem() == null) cargarTodasLasPuertas();
                else cargarPuertasDelCliente();
            });
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelFormulario.add(cmbCliente, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelFormulario.add(new JLabel("Puerta: *"), gbc);
            cmbPuerta = new JComboBox<>();
            estilizarCombo(cmbPuerta);
            cmbPuerta.addActionListener(e -> actualizarMotorSegunPuerta());
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelFormulario.add(cmbPuerta, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelFormulario.add(new JLabel("Tipo de motor:"), gbc);
            lblTipoMotor = new JLabel("—");
            lblTipoMotor.setFont(new Font("SansSerif", Font.ITALIC, 13));
            lblTipoMotor.setForeground(new Color(90, 90, 90));
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelFormulario.add(lblTipoMotor, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelFormulario.add(new JLabel("Servicio: *"), gbc);
            cmbTipoServicio = new JComboBox<>();
            estilizarCombo(cmbTipoServicio);
            cmbTipoServicio.addActionListener(e -> actualizarAvisoGarantia());
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelFormulario.add(cmbTipoServicio, gbc);
            gbc.gridwidth = 1;
            cargarServicios();

            gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 1;
            panelFormulario.add(new JLabel(""), gbc);
            lblGarantia = new JLabel(" ");
            lblGarantia.setFont(new Font("SansSerif", Font.BOLD, 12));
            lblGarantia.setForeground(new Color(200, 30, 30));
            gbc.gridx = 1; gbc.gridwidth = 2;
            panelFormulario.add(lblGarantia, gbc);
            gbc.gridwidth = 1;

            gbc.gridx = 0; gbc.gridy = ++y;
            panelFormulario.add(new JLabel("Notas:"), gbc);
            txtNotas = new JTextArea(4, 20);
            txtNotas.setLineWrap(true);
            txtNotas.setWrapStyleWord(true);
            txtNotas.setBorder(new LineBorder(new Color(180, 180, 180), 1));
            txtNotas.setFont(new Font("SansSerif", Font.PLAIN, 13));
            JScrollPane scrollNotas = new JScrollPane(txtNotas);
            scrollNotas.setBorder(new LineBorder(new Color(200, 200, 200), 1));
            gbc.gridx = 1; gbc.gridy = y; gbc.gridwidth = 2;
            panelFormulario.add(scrollNotas, gbc);
            gbc.gridwidth = 1;

            JLabel lblAyuda = new JLabel("<html><i style='color:gray'>* Campos obligatorios</i></html>");
            gbc.gridx = 0; gbc.gridy = ++y; gbc.gridwidth = 3;
            panelFormulario.add(lblAyuda, gbc);

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
            panelFormulario.add(panelBotones, gbc);

            JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, panelCalendario, panelFormulario);
            split.setResizeWeight(0.4);
            split.setDividerSize(6);
            split.setBorder(null);
            panel.add(split, BorderLayout.CENTER);

            getContentPane().add(panel);
            actualizarFechaSeleccionada();
        }

        private void estilizarCombo(JComboBox<?> combo) {
            combo.setBackground(Color.WHITE);
            combo.setFont(new Font("SansSerif", Font.PLAIN, 13));
            combo.setBorder(new LineBorder(new Color(180, 180, 180), 1));
        }

        // ── Calendario embebido ──
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
                        actualizarFechaSeleccionada();
                        actualizarCalendario();
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

        private void actualizarFechaSeleccionada() {
            txtFecha.setText(fechaSeleccionada.toString());
            actualizarAvisoGarantia();
        }

        private void actualizarAvisoGarantia() {
            if (lblGarantia == null) return;
            ServicioItem servicio = (ServicioItem) cmbTipoServicio.getSelectedItem();
            if (servicio != null && servicio.garantiaAnual) {
                LocalDate vencimiento = fechaSeleccionada.plusYears(1);
                lblGarantia.setText("La garantía de este servicio termina el " +
                        vencimiento.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            } else {
                lblGarantia.setText(" ");
            }
        }

        private void cargarClientes() {
            cmbCliente.removeAllItems();
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_cliente, nombre FROM cliente ORDER BY nombre")) {
                while (rs.next()) {
                    cmbCliente.addItem(new ClienteItem(rs.getInt("id_cliente"), rs.getString("nombre")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            cmbCliente.setSelectedItem(null);
        }

        private void cargarTodasLasPuertas() {
            cmbPuerta.removeAllItems();
            String sql = "SELECT p.id_puerta, p.id_direccion, p.color, p.id_tipo_motor, tm.nombre AS motor_nombre, " +
                         "tp.nombre AS tipo_puerta_nombre, d.calle, d.numero " +
                         "FROM puerta p " +
                         "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                         "LEFT JOIN tipo_motor tm ON p.id_tipo_motor = tm.id_tipo_motor " +
                         "LEFT JOIN tipo_puerta tp ON p.id_tipo_puerta = tp.id_tipo_puerta " +
                         "ORDER BY p.id_puerta";
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    Integer idMotor = rs.getObject("id_tipo_motor") != null ? rs.getInt("id_tipo_motor") : null;
                    String motor = rs.getString("motor_nombre");
                    String tipoPuerta = rs.getString("tipo_puerta_nombre");
                    String texto = (tipoPuerta != null ? tipoPuerta + ", " : "") + rs.getString("color") +
                            " (" + rs.getString("calle") + " " + rs.getString("numero") + ")";
                    cmbPuerta.addItem(new PuertaItem(rs.getInt("id_puerta"), rs.getInt("id_direccion"),
                            idMotor, motor, texto));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            if (cmbPuerta.getItemCount() > 0) cmbPuerta.setSelectedIndex(0);
            actualizarMotorSegunPuerta();
        }

        private void cargarPuertasDelCliente() {
            cmbPuerta.removeAllItems();
            ClienteItem cliente = (ClienteItem) cmbCliente.getSelectedItem();
            if (cliente == null) { cargarTodasLasPuertas(); return; }
            String sql = "SELECT p.id_puerta, p.id_direccion, p.color, p.id_tipo_motor, tm.nombre AS motor_nombre, " +
                         "tp.nombre AS tipo_puerta_nombre, d.calle, d.numero " +
                         "FROM puerta p " +
                         "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                         "LEFT JOIN tipo_motor tm ON p.id_tipo_motor = tm.id_tipo_motor " +
                         "LEFT JOIN tipo_puerta tp ON p.id_tipo_puerta = tp.id_tipo_puerta " +
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
                    String tipoPuerta = rs.getString("tipo_puerta_nombre");
                    String texto = (tipoPuerta != null ? tipoPuerta + ", " : "") + rs.getString("color") +
                            " (" + rs.getString("calle") + " " + rs.getString("numero") + ")";
                    cmbPuerta.addItem(new PuertaItem(rs.getInt("id_puerta"), rs.getInt("id_direccion"),
                            idMotor, motor, texto));
                }
                if (!hayPuertas) {
                    JOptionPane.showMessageDialog(this,
                            "Este cliente no tiene puertas registradas.\nAgrega una puerta desde la pestaña Puertas.",
                            "Sin puertas", JOptionPane.WARNING_MESSAGE);
                }
            } catch (SQLException e) { e.printStackTrace(); }
            if (cmbPuerta.getItemCount() > 0) cmbPuerta.setSelectedIndex(0);
            actualizarMotorSegunPuerta();
        }

        private void actualizarMotorSegunPuerta() {
            PuertaItem puerta = (PuertaItem) cmbPuerta.getSelectedItem();
            if (puerta == null) lblTipoMotor.setText("—");
            else if (puerta.idTipoMotor == null) lblTipoMotor.setText("Sin motor asignado");
            else lblTipoMotor.setText(puerta.tipoMotorNombre);
        }

        private void cargarServicios() {
            cmbTipoServicio.removeAllItems();
            try (Connection conn = Conexion.get();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT id_tipo_servicio, nombre, garantia_anual FROM tipo_servicio ORDER BY nombre")) {
                while (rs.next()) {
                    cmbTipoServicio.addItem(new ServicioItem(rs.getInt("id_tipo_servicio"), rs.getString("nombre"),
                            rs.getBoolean("garantia_anual")));
                }
            } catch (SQLException e) { e.printStackTrace(); }
            cmbTipoServicio.setSelectedItem(null);
        }

        private void cargarDatos() {
            String sql = "SELECT c.fecha_hora, c.notas, c.id_puerta, " +
                         "cl.id_cliente, sc.id_tipo_servicio " +
                         "FROM cita c " +
                         "JOIN puerta p ON c.id_puerta = p.id_puerta " +
                         "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                         "JOIN cliente cl ON d.id_cliente = cl.id_cliente " +
                         "LEFT JOIN servicio_cita sc ON c.id_cita = sc.id_cita " +
                         "WHERE c.id_cita = ?";
            try (Connection conn = Conexion.get();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, idCitaEditar);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    if (ts != null) {
                        fechaSeleccionada = ts.toLocalDateTime().toLocalDate();
                        txtFecha.setText(fechaSeleccionada.toString());
                        txtHora.setText(ts.toLocalDateTime().toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm")));
                        actualizarCalendario();
                    }
                    int idClienteCita = rs.getInt("id_cliente");
                    for (int i = 0; i < cmbCliente.getItemCount(); i++) {
                        if (cmbCliente.getItemAt(i).id == idClienteCita) {
                            cmbCliente.setSelectedIndex(i);
                            break;
                        }
                    }
                    int idPuertaCita = rs.getInt("id_puerta");
                    for (int i = 0; i < cmbPuerta.getItemCount(); i++) {
                        if (cmbPuerta.getItemAt(i).idPuerta == idPuertaCita) {
                            cmbPuerta.setSelectedIndex(i);
                            break;
                        }
                    }
                    int idServicioCita = rs.getInt("id_tipo_servicio");
                    for (int i = 0; i < cmbTipoServicio.getItemCount(); i++) {
                        if (cmbTipoServicio.getItemAt(i).id == idServicioCita) {
                            cmbTipoServicio.setSelectedIndex(i);
                            break;
                        }
                    }
                    actualizarAvisoGarantia();
                    txtNotas.setText(rs.getString("notas"));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }

        private void guardar() {
            if (cmbCliente.getSelectedItem() == null) {
                JOptionPane.showMessageDialog(this, "Seleccione un cliente.");
                return;
            }
            PuertaItem puerta = (PuertaItem) cmbPuerta.getSelectedItem();
            if (puerta == null) {
                JOptionPane.showMessageDialog(this, "Seleccione una puerta.");
                return;
            }
            ServicioItem servicio = (ServicioItem) cmbTipoServicio.getSelectedItem();
            if (servicio == null) {
                JOptionPane.showMessageDialog(this, "Seleccione el tipo de servicio.");
                return;
            }
            if (txtHora.getText().trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Indique la hora de la cita.");
                return;
            }

            try {
                LocalTime hora = LocalTime.parse(txtHora.getText().trim());
                LocalDateTime fechaHora = LocalDateTime.of(fechaSeleccionada, hora);
                Timestamp timestamp = Timestamp.valueOf(fechaHora);

                // Si el servicio tiene garantía anual, vence 1 año después de la fecha de la cita
                Timestamp vencimientoGarantia = servicio.garantiaAnual
                        ? Timestamp.valueOf(fechaHora.plusYears(1))
                        : null;

                try (Connection conn = Conexion.get()) {
                    conn.setAutoCommit(false);
                    if (idCitaEditar == null) {
                        String sqlCita = "INSERT INTO cita (fecha_hora, notas, id_puerta, id_usuario) VALUES (?, ?, ?, ?) RETURNING id_cita";
                        int idCitaNueva;
                        try (PreparedStatement ps = conn.prepareStatement(sqlCita)) {
                            ps.setTimestamp(1, timestamp);
                            ps.setString(2, txtNotas.getText().trim());
                            ps.setInt(3, puerta.idPuerta);
                            ps.setInt(4, idUsuario);
                            ResultSet rs = ps.executeQuery();
                            if (rs.next()) idCitaNueva = rs.getInt(1);
                            else throw new SQLException("No se pudo obtener id_cita");
                        }
                        String sqlServicio = "INSERT INTO servicio_cita (fecha_vencimiento_garantia, id_tipo_servicio, id_cita) VALUES (?, ?, ?)";
                        try (PreparedStatement ps = conn.prepareStatement(sqlServicio)) {
                            if (vencimientoGarantia != null) ps.setTimestamp(1, vencimientoGarantia);
                            else ps.setNull(1, Types.TIMESTAMP);
                            ps.setInt(2, servicio.id);
                            ps.setInt(3, idCitaNueva);
                            ps.executeUpdate();
                        }
                    } else {
                        String sqlCita = "UPDATE cita SET fecha_hora=?, notas=?, id_puerta=? WHERE id_cita=?";
                        try (PreparedStatement ps = conn.prepareStatement(sqlCita)) {
                            ps.setTimestamp(1, timestamp);
                            ps.setString(2, txtNotas.getText().trim());
                            ps.setInt(3, puerta.idPuerta);
                            ps.setInt(4, idCitaEditar);
                            ps.executeUpdate();
                        }
                        // Verificar si ya existe un registro de servicio_cita para esta cita
                        boolean existeServicioCita;
                        try (PreparedStatement psCheck = conn.prepareStatement(
                                "SELECT 1 FROM servicio_cita WHERE id_cita = ?")) {
                            psCheck.setInt(1, idCitaEditar);
                            try (ResultSet rsCheck = psCheck.executeQuery()) {
                                existeServicioCita = rsCheck.next();
                            }
                        }
                        if (existeServicioCita) {
                            String sqlServicio = "UPDATE servicio_cita SET id_tipo_servicio=?, fecha_vencimiento_garantia=? WHERE id_cita=?";
                            try (PreparedStatement ps = conn.prepareStatement(sqlServicio)) {
                                ps.setInt(1, servicio.id);
                                if (vencimientoGarantia != null) ps.setTimestamp(2, vencimientoGarantia);
                                else ps.setNull(2, Types.TIMESTAMP);
                                ps.setInt(3, idCitaEditar);
                                ps.executeUpdate();
                            }
                        } else {
                            String sqlServicio = "INSERT INTO servicio_cita (fecha_vencimiento_garantia, id_tipo_servicio, id_cita) VALUES (?, ?, ?)";
                            try (PreparedStatement ps = conn.prepareStatement(sqlServicio)) {
                                if (vencimientoGarantia != null) ps.setTimestamp(1, vencimientoGarantia);
                                else ps.setNull(1, Types.TIMESTAMP);
                                ps.setInt(2, servicio.id);
                                ps.setInt(3, idCitaEditar);
                                ps.executeUpdate();
                            }
                        }
                    }
                    conn.commit();
                    guardadoExitoso = true;
                    JOptionPane.showMessageDialog(this, "Cita guardada correctamente.");
                    dispose();
                }
            } catch (java.time.format.DateTimeParseException ex) {
                JOptionPane.showMessageDialog(this, "La hora no es válida. Usa el formato HH:mm, ejemplo: 14:30.");
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "Error al guardar: " + e.getMessage());
            }
        }

        public boolean isGuardadoExitoso() {
            return guardadoExitoso;
        }
    }
}