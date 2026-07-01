package utilidad;

import java.awt.*;
import javax.swing.*;
import javax.swing.table.*;

/**
 * Constantes visuales y métodos de estilo reutilizables para toda la aplicación.
 * Centraliza la paleta de colores y los helpers de UI para evitar duplicación.
 */
public final class Estilos {

    // --- Paleta de colores de la app ---
    public static final Color FONDO_OSCURO      = new Color(18, 18, 18);
    public static final Color FONDO_DEGRADADO   = new Color(40, 40, 45);
    public static final Color TEXTO_PRINCIPAL   = new Color(240, 240, 240);
    public static final Color TEXTO_ETIQUETA    = new Color(200, 200, 200);
    public static final Color AMARILLO          = new Color(228, 191, 52);
    public static final Color BOTON_SECUNDARIO  = new Color(70, 70, 70);
    public static final Color CAMPO_FONDO       = new Color(50, 50, 50);
    public static final Color CAMPO_BORDE       = new Color(100, 100, 100);
    public static final Color ROJO_PELIGRO      = new Color(200, 60, 60);

    // Colores para tablas (siempre blancas, independiente del tema)
    public static final Color TABLA_FONDO            = Color.WHITE;
    public static final Color TABLA_FONDO_SELECCION  = new Color(228, 191, 52, 180);
    public static final Color TABLA_TEXTO            = Color.BLACK;
    public static final Color TABLA_TEXTO_SELECCION  = Color.BLACK;
    public static final Color TABLA_HEADER_FONDO     = new Color(30, 30, 30);
    public static final Color TABLA_HEADER_TEXTO     = Color.WHITE;
    public static final Color TABLA_GRID             = new Color(220, 220, 220);
    public static final Color TABLA_FILA_ALT         = new Color(248, 248, 248);

    private Estilos() {} // No instanciar

    /** Aplica estilo oscuro a un JTextField o JPasswordField. */
    public static void estilizarCampo(JTextField field) {
        field.setBackground(CAMPO_FONDO);
        field.setForeground(TEXTO_PRINCIPAL);
        field.setCaretColor(TEXTO_PRINCIPAL);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CAMPO_BORDE, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    /** Aplica estilo a un JButton con colores personalizados. */
    public static void estilizarBoton(JButton btn, Color fondo, Color texto) {
        btn.setBackground(fondo);
        btn.setForeground(texto);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setFont(new Font("SansSerif", Font.BOLD, 13));
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
    }

    /**
     * Botón circular de navegación (flechas para cambiar de mes en el calendario).
     * Dibuja un triángulo vectorial con Graphics2D en lugar de usar glifos
     * Unicode (◀ ▶), que en muchas fuentes/SO se renderizan como puntos
     * suspensivos o cuadros vacíos.
     */
    public static JButton botonFlecha(boolean apuntaDerecha) {
        JButton btn = new JButton() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? AMARILLO : TEXTO_PRINCIPAL);
                int w = getWidth(), h = getHeight();
                int triW = w / 4, triH = h / 3;
                int cx = w / 2, cy = h / 2;
                Polygon p = new Polygon();
                if (apuntaDerecha) {
                    p.addPoint(cx - triW / 2, cy - triH);
                    p.addPoint(cx - triW / 2, cy + triH);
                    p.addPoint(cx + triW / 2, cy);
                } else {
                    p.addPoint(cx + triW / 2, cy - triH);
                    p.addPoint(cx + triW / 2, cy + triH);
                    p.addPoint(cx - triW / 2, cy);
                }
                g2.fillPolygon(p);
                g2.dispose();
            }
        };
        btn.setPreferredSize(new Dimension(38, 32));
        btn.setBackground(BOTON_SECUNDARIO);
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(true);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    /**
     * Aplica estilo visual blanco a una JTable para que se vea en modo claro
     * incluso cuando el tema general de la app es oscuro (FlatDarkLaf).
     */
    public static void estilizarTabla(JTable tabla) {
        // Fondo y texto de las celdas
        tabla.setBackground(TABLA_FONDO);
        tabla.setForeground(TABLA_TEXTO);
        tabla.setSelectionBackground(TABLA_FONDO_SELECCION);
        tabla.setSelectionForeground(TABLA_TEXTO_SELECCION);
        tabla.setGridColor(TABLA_GRID);
        tabla.setShowGrid(true);
        tabla.setRowHeight(32);
        tabla.setFont(new Font("SansSerif", Font.PLAIN, 13));
        tabla.setFillsViewportHeight(true);

        // Renderer alternado (rayas)
        tabla.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (isSelected) {
                    setBackground(TABLA_FONDO_SELECCION);
                    setForeground(TABLA_TEXTO_SELECCION);
                } else {
                    setBackground(row % 2 == 0 ? TABLA_FONDO : TABLA_FILA_ALT);
                    setForeground(TABLA_TEXTO);
                }
                setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
                return this;
            }
        });

        // Encabezado oscuro con texto blanco
        JTableHeader header = tabla.getTableHeader();
        header.setBackground(TABLA_HEADER_FONDO);
        header.setForeground(TABLA_HEADER_TEXTO);
        header.setFont(new Font("SansSerif", Font.BOLD, 13));
        header.setReorderingAllowed(false);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setDefaultRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                setBackground(TABLA_HEADER_FONDO);
                setForeground(TABLA_HEADER_TEXTO);
                setFont(new Font("SansSerif", Font.BOLD, 13));
                setHorizontalAlignment(SwingConstants.LEFT);
                setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 2, 1, AMARILLO),
                        BorderFactory.createEmptyBorder(0, 8, 0, 8)
                ));
                return this;
            }
        });
    }

    /**
     * Envuelve una JTable en un JScrollPane con fondo blanco correcto.
     */
    public static JScrollPane scrollParaTabla(JTable tabla) {
        JScrollPane scroll = new JScrollPane(tabla);
        scroll.getViewport().setBackground(TABLA_FONDO);
        scroll.setBorder(BorderFactory.createLineBorder(CAMPO_BORDE));
        return scroll;
    }
}
