package InterfacesSecundarias;

import utilidad.Conexion;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.IOException;

public class configuracionBD extends JFrame {
    private JTextField urlField;
    private JTextField usuarioField;
    private JPasswordField claveField;
    private JButton guardarButton; // para referencia

    // colores...
    private static final Color COLOR_FONDO = new Color(18, 18, 18);
    private static final Color COLOR_TEXTO = new Color(240, 240, 240);
    private static final Color COLOR_ETIQUETA = new Color(200, 200, 200);
    private static final Color COLOR_BOTON_PRINCIPAL = new Color(228, 191, 52);
    private static final Color COLOR_BOTON_SECUNDARIO = new Color(70, 70, 70);
    private static final Color COLOR_CAMPO_TEXTO = new Color(50, 50, 50);
    private static final Color COLOR_BORDE_CAMPO = new Color(100, 100, 100);

    public configuracionBD() {
        initUI();
        cargarValoresActuales();
        getRootPane().setDefaultButton(guardarButton); // Enter guarda
    }

    private void initUI() {
        setTitle("Configuración de Base de Datos");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(500, 350);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                GradientPaint gp = new GradientPaint(0, 0, COLOR_FONDO, 0, getHeight(), new Color(40, 40, 45));
                g2d.setPaint(gp);
                g2d.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 30, 20, 30));
        setContentPane(mainPanel);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        JLabel tituloLabel = new JLabel("Configuración de conexión");
        tituloLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        tituloLabel.setForeground(COLOR_TEXTO);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(tituloLabel, gbc);

        gbc.gridy = 1;
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)), gbc);

        // URL
        JLabel urlLabel = new JLabel("URL");
        urlLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        urlLabel.setForeground(COLOR_ETIQUETA);
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.anchor = GridBagConstraints.WEST;
        centerPanel.add(urlLabel, gbc);

        urlField = new JTextField();
        styleTextField(urlField);
        urlField.setPreferredSize(new Dimension(300, 35));
        gbc.gridx = 1;
        centerPanel.add(urlField, gbc);

        // Usuario
        JLabel usuarioLabel = new JLabel("Usuario");
        usuarioLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        usuarioLabel.setForeground(COLOR_ETIQUETA);
        gbc.gridx = 0;
        gbc.gridy = 3;
        centerPanel.add(usuarioLabel, gbc);

        usuarioField = new JTextField();
        styleTextField(usuarioField);
        usuarioField.setPreferredSize(new Dimension(300, 35));
        gbc.gridx = 1;
        centerPanel.add(usuarioField, gbc);

        // Clave
        JLabel claveLabel = new JLabel("Clave");
        claveLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        claveLabel.setForeground(COLOR_ETIQUETA);
        gbc.gridx = 0;
        gbc.gridy = 4;
        centerPanel.add(claveLabel, gbc);

        claveField = new JPasswordField();
        styleTextField(claveField);
        claveField.setPreferredSize(new Dimension(300, 35));
        gbc.gridx = 1;
        centerPanel.add(claveField, gbc);

        // Botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        buttonPanel.setOpaque(false);

        guardarButton = new JButton("Guardar");
        styleButton(guardarButton, COLOR_BOTON_PRINCIPAL, Color.BLACK);
        guardarButton.addActionListener(this::guardarAction);
        buttonPanel.add(guardarButton);

        JButton cancelarButton = new JButton("Cancelar");
        styleButton(cancelarButton, COLOR_BOTON_SECUNDARIO, COLOR_TEXTO);
        cancelarButton.addActionListener(e -> dispose());
        buttonPanel.add(cancelarButton);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 2;
        gbc.anchor = GridBagConstraints.CENTER;
        centerPanel.add(buttonPanel, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);
    }

    private void styleTextField(JTextField field) {
        field.setBackground(COLOR_CAMPO_TEXTO);
        field.setForeground(COLOR_TEXTO);
        field.setCaretColor(COLOR_TEXTO);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDE_CAMPO, 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)
        ));
        field.setFont(new Font("SansSerif", Font.PLAIN, 14));
    }

    private void styleButton(JButton button, Color bg, Color fg) {
        button.setBackground(bg);
        button.setForeground(fg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setFont(new Font("SansSerif", Font.BOLD, 13));
        button.setOpaque(true);
        button.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
    }

    private void cargarValoresActuales() {
        urlField.setText(Conexion.getUrl());
        usuarioField.setText(Conexion.getUsuario());
        claveField.setText(Conexion.getClave());
    }

    private void guardarAction(ActionEvent e) {
        String nuevaUrl = urlField.getText().trim();
        String nuevoUsuario = usuarioField.getText().trim();
        String nuevaClave = new String(claveField.getPassword());

        if (nuevaUrl.isEmpty() || nuevoUsuario.isEmpty()) {
            JOptionPane.showMessageDialog(this, "URL y Usuario son obligatorios.", "Campos vacíos", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!Conexion.probarConexion(nuevaUrl, nuevoUsuario, nuevaClave)) {
            JOptionPane.showMessageDialog(this, "No se pudo conectar a la base de datos.\nVerifique los parámetros.", "Error de conexión", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            Conexion.actualizarConfiguracion(nuevaUrl, nuevoUsuario, nuevaClave);
            JOptionPane.showMessageDialog(this, "Configuración guardada correctamente.\nConexión exitosa.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error al guardar: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}