package InterfacesPrincipales;

import InterfacesSecundarias.configuracionBD;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.util.logging.Level;
import Clases.Usuario;
import java.awt.event.ActionEvent;
import utilidad.Conexion;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.mindrot.jbcrypt.BCrypt;

public class login extends JFrame {

    private static final java.util.logging.Logger logger = java.util.logging.Logger.getLogger(login.class.getName());

    
    
    private JLabel logoLabel;
    private JLabel tituloLabel;
    private JLabel usuarioLabel;
    private JLabel passwordLabel;
    private JTextField usuarioField;
    private JPasswordField passwordField;
    private JButton ingresarButton;
    private JButton configBDButton;

    private static final Color COLOR_FONDO = new Color(18, 18, 18);
    private static final Color COLOR_TEXTO = new Color(240, 240, 240);
    private static final Color COLOR_ETIQUETA = new Color(200, 200, 200);
    private static final Color COLOR_BOTON_PRINCIPAL = new Color(228, 191, 52);
    private static final Color COLOR_BOTON_SECUNDARIO = new Color(70, 70, 70);
    private static final Color COLOR_CAMPO_TEXTO = new Color(50, 50, 50);
    private static final Color COLOR_BORDE_CAMPO = new Color(100, 100, 100);

    public login() {
        initComponents();
        setupWindow();
        loadAndScaleImage();
        getRootPane().setDefaultButton(ingresarButton);
        configurarNavegacionFlechas();
    }

    
    
    private void initComponents() {
        setTitle("Inicio de sesión - Agenda");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(800, 550));

        // Panel principal con fondo degradado
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

        // Panel central con GridBagLayout (logo izquierda, formulario derecha)
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.CENTER;

        // Logo
        logoLabel = new JLabel();
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0.4;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(logoLabel, gbc);

        // Panel formulario (usa GridBagLayout para alinear etiquetas a la izquierda)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints formGbc = new GridBagConstraints();
        formGbc.insets = new Insets(5, 10, 5, 10);
        formGbc.anchor = GridBagConstraints.WEST;
        formGbc.fill = GridBagConstraints.HORIZONTAL;

        // Título AGENDA (centrado dentro del formPanel)
        tituloLabel = new JLabel("AGENDA", SwingConstants.CENTER);
        tituloLabel.setFont(new Font("SansSerif", Font.BOLD, 42));
        tituloLabel.setForeground(COLOR_TEXTO);
        formGbc.gridx = 0;
        formGbc.gridy = 0;
        formGbc.gridwidth = 2;
        formGbc.anchor = GridBagConstraints.CENTER;
        formPanel.add(tituloLabel, formGbc);

        // Espaciador
        formGbc.gridy = 1;
        formPanel.add(Box.createRigidArea(new Dimension(0, 20)), formGbc);

        // Etiqueta Usuario
        formGbc.gridy = 2;
        formGbc.gridwidth = 1;
        formGbc.anchor = GridBagConstraints.WEST;
        usuarioLabel = new JLabel("Usuario");
        usuarioLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        usuarioLabel.setForeground(COLOR_ETIQUETA);
        formPanel.add(usuarioLabel, formGbc);

        // Campo Usuario (más ancho: 320px)
        formGbc.gridy = 3;
        usuarioField = new JTextField(15);
        styleTextField(usuarioField);
        usuarioField.setPreferredSize(new Dimension(320, 40));
        usuarioField.setMinimumSize(new Dimension(320, 40));
        formPanel.add(usuarioField, formGbc);

        // Etiqueta Contraseña
        formGbc.gridy = 4;
        passwordLabel = new JLabel("Contraseña");
        passwordLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));
        passwordLabel.setForeground(COLOR_ETIQUETA);
        formPanel.add(passwordLabel, formGbc);

        // Campo Contraseña (más ancho: 320px)
        formGbc.gridy = 5;
        passwordField = new JPasswordField(15);
        styleTextField(passwordField);
        passwordField.setPreferredSize(new Dimension(320, 40));
        passwordField.setMinimumSize(new Dimension(320, 40));
        formPanel.add(passwordField, formGbc);

        // Espacio antes del botón
        formGbc.gridy = 6;
        formPanel.add(Box.createRigidArea(new Dimension(0, 15)), formGbc);

        // Botón Ingresar (más corto horizontalmente: 130px)
        formGbc.gridy = 7;
        formGbc.anchor = GridBagConstraints.CENTER; // centrar botón
        ingresarButton = new JButton("Ingresar");
        styleButton(ingresarButton, COLOR_BOTON_PRINCIPAL, Color.BLACK);
        ingresarButton.setFont(new Font("SansSerif", Font.BOLD, 16));
        ingresarButton.setPreferredSize(new Dimension(130, 45));
        ingresarButton.setMinimumSize(new Dimension(130, 40));
        ingresarButton.addActionListener(this::ingresarActionPerformed);
        formPanel.add(ingresarButton, formGbc);

        // Añadir formPanel al centro del GridBagLayout principal
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.gridwidth = 1;
        gbc.gridheight = 2;
        gbc.weightx = 0.6;
        gbc.fill = GridBagConstraints.BOTH;
        centerPanel.add(formPanel, gbc);

        mainPanel.add(centerPanel, BorderLayout.CENTER);

        // Botón inferior izquierdo
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        bottomPanel.setOpaque(false);
        configBDButton = new JButton("Conexión a la base de datos");
        styleButton(configBDButton, COLOR_BOTON_SECUNDARIO, COLOR_TEXTO);
        configBDButton.addActionListener(this::configBDActionPerformed);
        bottomPanel.add(configBDButton);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);
    }

    private void styleTextField(JTextField field) {
        field.setBackground(COLOR_CAMPO_TEXTO);
        field.setForeground(COLOR_TEXTO);
        field.setCaretColor(COLOR_TEXTO);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(COLOR_BORDE_CAMPO, 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
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
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
    }

    private void loadAndScaleImage() {
        try {
            String imagePath = "/utilidad/negativo transparente.png";
            InputStream imgStream = getClass().getResourceAsStream(imagePath);
            if (imgStream == null) {
                logger.warning("No se pudo encontrar la imagen: " + imagePath);
                logoLabel.setText("[Logo no disponible]");
                return;
            }
            BufferedImage original = ImageIO.read(imgStream);
            if (original == null) {
                logger.warning("La imagen no se pudo decodificar: " + imagePath);
                logoLabel.setText("[Formato no soportado]");
                return;
            }
            int targetWidth = 250;
            int targetHeight = 250;
            Image scaled = getScaledImageWithQuality(original, targetWidth, targetHeight);
            logoLabel.setIcon(new ImageIcon(scaled));
        } catch (Exception ex) {
            logger.log(Level.SEVERE, "Error al cargar/escalar la imagen", ex);
            logoLabel.setText("[Error imagen]");
        }
    }

    private Image getScaledImageWithQuality(BufferedImage original, int targetWidth, int targetHeight) {
        int originalWidth = original.getWidth();
        int originalHeight = original.getHeight();
        double scale = Math.min((double) targetWidth / originalWidth, (double) targetHeight / originalHeight);
        int newWidth = (int) (originalWidth * scale);
        int newHeight = (int) (originalHeight * scale);
        BufferedImage scaledImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = scaledImage.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();
        return scaledImage;
    }

    private void setupWindow() {
        setResizable(true);
        setSize(950, 600); // Ligeramente más ancho para acomodar campos de 320px
        setLocationRelativeTo(null);
    }

    private void ingresarActionPerformed(java.awt.event.ActionEvent evt) {
    String user = usuarioField.getText().trim();
    String pass = new String(passwordField.getPassword());

    if (user.isEmpty() || pass.isEmpty()) {
        JOptionPane.showMessageDialog(this,
                "Por favor, complete ambos campos.",
                "Campos vacíos", JOptionPane.WARNING_MESSAGE);
        return;
    }

    String sql = "SELECT u.id_usuario, u.nombre, u.contrasenia, c.id_cargo " +
                 "FROM usuario u " +
                 "JOIN cargo c ON u.id_cargo = c.id_cargo " +
                 "WHERE u.nombre = ?";

    try (Connection conn = Conexion.get();
         PreparedStatement ps = conn.prepareStatement(sql)) {

        ps.setString(1, user);
        ResultSet rs = ps.executeQuery();

        if (!rs.next()) {
            JOptionPane.showMessageDialog(this,
                    "Usuario no encontrado.",
                    "Error de autenticación", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String contrasenaBD = rs.getString("contrasenia");
        boolean autenticado = false;

        // 1️⃣ Intentar con BCrypt (contraseñas hasheadas)
        try {
            if (contrasenaBD.startsWith("$2a$") && BCrypt.checkpw(pass, contrasenaBD)) {
                autenticado = true;
            }
        } catch (Exception e) {
            // Si falla BCrypt (ej: hash mal formado), continuamos con el fallback
        }

        // 2️⃣ Fallback a texto plano (solo si no se autenticó con BCrypt)
        if (!autenticado && contrasenaBD.equals(pass)) {
            autenticado = true;
        }

        if (autenticado) {
            int idUsuario = rs.getInt("id_usuario");
            int idCargo = rs.getInt("id_cargo");
            int rol = (idCargo == 1) ? 0 : 1; // 1=Admin (rol 0), 2=Encargado (rol 1)

            Usuario usuario = new Usuario(idUsuario, user, pass, rol);
            new PanelGeneral(usuario).setVisible(true);
            this.dispose();
        } else {
            JOptionPane.showMessageDialog(this,
                    "Contraseña incorrecta.",
                    "Error de autenticación", JOptionPane.ERROR_MESSAGE);
        }

    } catch (SQLException e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this,
                "Error al conectar con la base de datos.\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}

    private void configBDActionPerformed(java.awt.event.ActionEvent evt) {
    new configuracionBD().setVisible(true);}
    
  private void configurarNavegacionFlechas() {
        // Desde el campo usuario, DOWN → focus a contraseña
        usuarioField.getInputMap().put(KeyStroke.getKeyStroke("DOWN"), "irAPassword");
        usuarioField.getActionMap().put("irAPassword", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                passwordField.requestFocusInWindow();
            }
        });

        // Desde el campo contraseña, UP → focus a usuario
        passwordField.getInputMap().put(KeyStroke.getKeyStroke("UP"), "irAUsuario");
        passwordField.getActionMap().put("irAUsuario", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                usuarioField.requestFocusInWindow();
            }
        });
    }  
}