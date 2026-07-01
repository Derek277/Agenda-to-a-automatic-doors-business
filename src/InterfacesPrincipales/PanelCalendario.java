package InterfacesPrincipales;

import utilidad.Conexion;
import utilidad.Estilos;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.sql.Date;

public class PanelCalendario extends JPanel {

    private YearMonth mesActual = YearMonth.now();
    private Set<Integer> diasConCitas = new HashSet<>();

    private JLabel lblMesAnio;
    private JPanel gridDias;

    private DefaultTableModel modeloTabla;
    private JTable tablaCitas;
    private JLabel lblTituloLista;

    private List<LocalDate> fechaPorFila = new ArrayList<>();

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FMT_FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public PanelCalendario() {
        setLayout(new BorderLayout(14, 14));
        setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        setBackground(Color.WHITE);

        add(buildPanelCalendario(), BorderLayout.WEST);
        add(buildPanelListaCitas(), BorderLayout.CENTER);

        cargarMes();
        // ═══ MOSTRAR LAS PRÓXIMAS CITAS AL ENTRAR ═══
        mostrarProximasCitas();
        
        configurarAtajosTeclado();
    }

    // ── Construcción del calendario ──────────────────────────────────────────

    private JPanel buildPanelCalendario() {
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setPreferredSize(new Dimension(360, 0));
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(220, 220, 220)));

        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(Estilos.FONDO_OSCURO);
        nav.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Botones de flecha (sin depender de Estilos.botonFlecha)
        JButton btnAnterior = new JButton("◀");
        JButton btnSiguiente = new JButton("▶");
        Font flechaFont = new Font("SansSerif", Font.BOLD, 18);
        btnAnterior.setFont(flechaFont);
        btnSiguiente.setFont(flechaFont);
        btnAnterior.setForeground(Color.WHITE);
        btnSiguiente.setForeground(Color.WHITE);
        btnAnterior.setContentAreaFilled(false);
        btnSiguiente.setContentAreaFilled(false);
        btnAnterior.setBorderPainted(false);
        btnSiguiente.setBorderPainted(false);
        btnAnterior.setFocusPainted(false);
        btnSiguiente.setFocusPainted(false);
        btnAnterior.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnSiguiente.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        btnAnterior.setToolTipText("Mes anterior");
        btnSiguiente.setToolTipText("Mes siguiente");
        btnAnterior.addActionListener(e -> { mesActual = mesActual.minusMonths(1); cargarMes(); });
        btnSiguiente.addActionListener(e -> { mesActual = mesActual.plusMonths(1); cargarMes(); });

        lblMesAnio = new JLabel("", SwingConstants.CENTER);
        lblMesAnio.setFont(new Font("SansSerif", Font.BOLD, 17));
        lblMesAnio.setForeground(Color.WHITE);

        nav.add(btnAnterior, BorderLayout.WEST);
        nav.add(lblMesAnio, BorderLayout.CENTER);
        nav.add(btnSiguiente, BorderLayout.EAST);

        

        JButton btnProximas = new JButton("Ver próximas citas");
        Estilos.estilizarBoton(btnProximas, Estilos.AMARILLO, Color.BLACK);
        btnProximas.addActionListener(e -> mostrarProximasCitas());

        JPanel panelBotonesInferior = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 6));
        panelBotonesInferior.setBackground(Color.WHITE);
        panelBotonesInferior.add(btnProximas);

        JPanel encDias = new JPanel(new GridLayout(1, 7));
        encDias.setBackground(Color.WHITE);
        encDias.setBorder(BorderFactory.createEmptyBorder(8, 10, 0, 10));
        for (String d : new String[]{"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"}) {
            JLabel lbl = new JLabel(d, SwingConstants.CENTER);
            lbl.setFont(new Font("SansSerif", Font.BOLD, 12));
            lbl.setForeground(new Color(90, 90, 90));
            encDias.add(lbl);
        }

        gridDias = new JPanel(new GridLayout(0, 7, 4, 4));
        gridDias.setBackground(Color.WHITE);
        gridDias.setBorder(BorderFactory.createEmptyBorder(6, 10, 14, 10));

        JPanel cabecera = new JPanel(new BorderLayout());
        cabecera.add(nav, BorderLayout.NORTH);
        cabecera.add(panelBotonesInferior, BorderLayout.SOUTH);

        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(Color.WHITE);
        body.add(encDias, BorderLayout.NORTH);
        body.add(gridDias, BorderLayout.CENTER);

        panel.add(cabecera, BorderLayout.NORTH);
        panel.add(body, BorderLayout.CENTER);
        return panel;
    }

    private void cargarMes() {
        String mesNombre = mesActual.getMonth().getDisplayName(TextStyle.FULL, new Locale("es", "MX"));
        lblMesAnio.setText(mesNombre.substring(0, 1).toUpperCase() + mesNombre.substring(1) + " " + mesActual.getYear());

        diasConCitas = obtenerDiasConCitas(mesActual.getYear(), mesActual.getMonthValue());
        dibujarDias();

        modeloTabla.setRowCount(0);
        fechaPorFila.clear();
        lblTituloLista.setText("");
    }

    private void dibujarDias() {
        gridDias.removeAll();

        LocalDate primerDia = mesActual.atDay(1);
        int offset = primerDia.getDayOfWeek().getValue() - 1; // 0 = lunes
        for (int i = 0; i < offset; i++) {
            JLabel vacio = new JLabel();
            vacio.setOpaque(true);
            vacio.setBackground(Color.WHITE);
            gridDias.add(vacio);
        }

        LocalDate hoy = LocalDate.now();
        int diasEnMes = mesActual.lengthOfMonth();

        for (int d = 1; d <= diasEnMes; d++) {
            final int dia = d;
            boolean tieneCita = diasConCitas.contains(dia);
            boolean esHoy = mesActual.atDay(d).equals(hoy);

            JPanel celda = construirCeldaDia(d, tieneCita, esHoy);
            celda.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    seleccionarDia(mesActual.atDay(dia));
                }
                @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                    celda.setBackground(new Color(255, 245, 225));
                }
                @Override public void mouseExited(java.awt.event.MouseEvent e) {
                    celda.setBackground(esHoy ? Estilos.AMARILLO : Color.WHITE);
                }
            });
            gridDias.add(celda);
        }

        gridDias.revalidate();
        gridDias.repaint();
    }

    private JPanel construirCeldaDia(int dia, boolean tieneCita, boolean esHoy) {
        JPanel celda = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (tieneCita) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(esHoy ? Color.BLACK : Estilos.AMARILLO);
                    int diametro = 6;
                    g2.fillOval(getWidth() / 2 - diametro / 2, getHeight() - 10, diametro, diametro);
                    g2.dispose();
                }
            }
        };
        celda.setLayout(new BorderLayout());
        celda.setBackground(esHoy ? Estilos.AMARILLO : Color.WHITE);
        celda.setBorder(BorderFactory.createLineBorder(new Color(235, 235, 235)));
        celda.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        celda.setPreferredSize(new Dimension(44, 44));

        JLabel lbl = new JLabel(String.valueOf(dia), SwingConstants.CENTER);
        lbl.setFont(new Font("SansSerif", tieneCita ? Font.BOLD : Font.PLAIN, 13));
        lbl.setForeground(esHoy ? Color.BLACK : Color.DARK_GRAY);
        celda.add(lbl, BorderLayout.CENTER);

        if (tieneCita) celda.setToolTipText("Hay citas agendadas este día");
        return celda;
    }

    private void seleccionarDia(LocalDate dia) {
        mostrarCitasDeUnDia(obtenerCitasPorDia(dia), dia);
    }

    // ── Lista de citas (tabla derecha) ───────────────────────────────────────

    private JPanel buildPanelListaCitas() {
        modeloTabla = new DefaultTableModel(new String[]{"Fecha /Hora", "Cliente", "Puerta", "Servicio"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        tablaCitas = new JTable(modeloTabla);
        Estilos.estilizarTabla(tablaCitas);
        tablaCitas.setDefaultRenderer(Object.class, new SeparadorDiasRenderer());

        lblTituloLista = new JLabel("Selecciona un día para ver sus citas");
        lblTituloLista.setFont(new Font("SansSerif", Font.BOLD, 16));

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setBackground(Color.WHITE);
        panel.add(lblTituloLista, BorderLayout.NORTH);
        panel.add(Estilos.scrollParaTabla(tablaCitas), BorderLayout.CENTER);
        return panel;
    }

    private void mostrarCitasDeUnDia(List<Object[]> citas, LocalDate dia) {
        modeloTabla.setRowCount(0);
        fechaPorFila.clear();
        for (Object[] fila : citas) {
            modeloTabla.addRow(new Object[]{fila[0], fila[1], fila[2], fila[3]});
            fechaPorFila.add(dia);
        }
        DateTimeFormatter fmtTitulo = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es", "MX"));
        String texto = dia.format(fmtTitulo);
        lblTituloLista.setText(texto.substring(0, 1).toUpperCase() + texto.substring(1)
                + "  (" + citas.size() + (citas.size() == 1 ? " cita)" : " citas)"));
    }

    private void mostrarProximasCitas() {
        List<Object[]> filas = new ArrayList<>();
        List<LocalDate> fechas = new ArrayList<>();

        String sql = "SELECT c.fecha_hora, cl.nombre AS cliente, p.color AS puerta, ts.nombre AS servicio " +
                     "FROM cita c " +
                     "JOIN puerta p ON c.id_puerta = p.id_puerta " +
                     "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                     "JOIN cliente cl ON d.id_cliente = cl.id_cliente " +
                     "LEFT JOIN servicio_cita sc ON c.id_cita = sc.id_cita " +
                     "LEFT JOIN tipo_servicio ts ON sc.id_tipo_servicio = ts.id_tipo_servicio " +
                     "WHERE c.fecha_hora >= NOW() " +
                     "ORDER BY c.fecha_hora ASC";
        try (Connection conn = Conexion.get();
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha_hora");
                LocalDateTime fh = ts.toLocalDateTime();
                String fecha = fh.format(FMT_FECHA_CORTA) + "  " + fh.format(FMT_HORA);
                filas.add(new Object[]{fecha, rs.getString("cliente"), rs.getString("puerta"),
                        rs.getString("servicio") != null ? rs.getString("servicio") : "—"});
                fechas.add(fh.toLocalDate());
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        modeloTabla.setRowCount(0);
        fechaPorFila.clear();
        for (int i = 0; i < filas.size(); i++) {
            modeloTabla.addRow(filas.get(i));
            fechaPorFila.add(fechas.get(i));
        }
        lblTituloLista.setText("Próximas citas  (" + filas.size() + (filas.size() == 1 ? " cita)" : " citas)"));
    }

    // ── Acceso a datos ───────────────────────────────────────────────────────

    private Set<Integer> obtenerDiasConCitas(int anio, int mes) {
        Set<Integer> dias = new HashSet<>();
        String sql = "SELECT DISTINCT EXTRACT(DAY FROM fecha_hora)::int AS dia FROM cita " +
                     "WHERE EXTRACT(YEAR FROM fecha_hora) = ? AND EXTRACT(MONTH FROM fecha_hora) = ?";
        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, anio);
            ps.setInt(2, mes);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) dias.add(rs.getInt("dia"));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return dias;
    }

    private List<Object[]> obtenerCitasPorDia(LocalDate dia) {
        List<Object[]> lista = new ArrayList<>();
        String sql = "SELECT c.fecha_hora, cl.nombre AS cliente, p.color AS puerta, ts.nombre AS servicio " +
                     "FROM cita c " +
                     "JOIN puerta p ON c.id_puerta = p.id_puerta " +
                     "JOIN direccion d ON p.id_direccion = d.id_direccion " +
                     "JOIN cliente cl ON d.id_cliente = cl.id_cliente " +
                     "LEFT JOIN servicio_cita sc ON c.id_cita = sc.id_cita " +
                     "LEFT JOIN tipo_servicio ts ON sc.id_tipo_servicio = ts.id_tipo_servicio " +
                     "WHERE DATE(c.fecha_hora) = ? " +
                     "ORDER BY c.fecha_hora";
        try (Connection conn = Conexion.get();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(dia));
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha_hora");
                String hora = ts.toLocalDateTime().format(FMT_HORA);
                lista.add(new Object[]{hora, rs.getString("cliente"), rs.getString("puerta"),
                        rs.getString("servicio") != null ? rs.getString("servicio") : "—"});
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error al cargar citas: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
        return lista;
    }

    private class SeparadorDiasRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column) {
            JLabel label = (JLabel) super.getTableCellRendererComponent(
                    table, value, isSelected, hasFocus, row, column);

            boolean cambioDeDia = false;
            if (row > 0 && row < fechaPorFila.size()) {
                LocalDate actual = fechaPorFila.get(row);
                LocalDate anterior = fechaPorFila.get(row - 1);
                cambioDeDia = actual != null && !actual.equals(anterior);
            }

            label.setBorder(cambioDeDia
                    ? BorderFactory.createCompoundBorder(
                          BorderFactory.createMatteBorder(2, 0, 0, 0, new Color(180, 180, 180)),
                          BorderFactory.createEmptyBorder(0, 8, 0, 8))
                    : BorderFactory.createEmptyBorder(0, 8, 0, 8));

            if (!isSelected) {
                label.setBackground(row % 2 == 0 ? Color.WHITE : new Color(247, 247, 247));
                label.setForeground(Color.BLACK);
            }
            return label;
        }
    }
    
    private void configurarAtajosTeclado() {
        InputMap im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
        ActionMap am = getActionMap();

        

        // Flecha derecha → mes siguiente
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "flechaDerecha");
        am.put("flechaDerecha", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                mesSiguiente();
            }
        });

        // Flecha izquierda → mes anterior
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "flechaIzquierda");
        am.put("flechaIzquierda", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                mesAnterior();
            }
        });
    }
    
    private void mesAnterior() {
        mesActual = mesActual.minusMonths(1);
        cargarMes();
    }

    private void mesSiguiente() {
        mesActual = mesActual.plusMonths(1);
        cargarMes();
    }
}