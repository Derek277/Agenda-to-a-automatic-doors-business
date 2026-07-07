package InterfacesPrincipales;

import Clases.Usuario;
import InterfacesSecundarias.PanelUsuarios;
import javax.swing.*;
import java.awt.*;

public class PanelGeneral extends JFrame {
    private Usuario usuarioActual; // tu clase Usuario con id, nombre, rol (0=admin,1=encargado)
    private JLabel lblRolUsuario;
    private JButton btnCerrarSesion;
    private JButton btnAdminUsuarios;
    private JTabbedPane tabbedPane;

    public PanelGeneral(Usuario usuario) {
        this.usuarioActual = usuario;
        initComponents();
        configurarPorRol();
        setExtendedState(JFrame.MAXIMIZED_BOTH); // Pantalla completa
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    private void initComponents() {
        setTitle("INNOVATEC - Agenda de Servicios");

        // Panel cabecera
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Color.BLACK);
        header.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
JPanel panelTitulo = new JPanel(new GridBagLayout());
panelTitulo.setOpaque(false);
JLabel lblInnovatec = new JLabel("INNOVATEC");
lblInnovatec.setFont(new Font("SansSerif", Font.BOLD, 30));
lblInnovatec.setForeground(utilidad.Estilos.AMARILLO);
JLabel lblPuertas = new JLabel("<html><u>puertas automáticas</u></html>");


lblPuertas.setFont(new Font("SansSerif", Font.BOLD, 25));


lblPuertas.setForeground(utilidad.Estilos.AMARILLO);
GridBagConstraints gbc = new GridBagConstraints();
gbc.gridx = 0;
gbc.gridy = 0;
gbc.anchor = GridBagConstraints.BASELINE;
gbc.insets = new Insets(0, 0, 0, 8);
panelTitulo.add(lblInnovatec, gbc);
gbc.gridx = 1;
gbc.insets = new Insets(0, 0, 0, 0);
panelTitulo.add(lblPuertas, gbc);

header.add(panelTitulo, BorderLayout.WEST);

        lblRolUsuario = new JLabel();
        lblRolUsuario.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblRolUsuario.setForeground(Color.LIGHT_GRAY);

        btnCerrarSesion = new JButton("Cerrar sesión");
        btnCerrarSesion.addActionListener(e -> cerrarSesion());

        btnAdminUsuarios = new JButton("Administrar Usuarios");
        btnAdminUsuarios.addActionListener(e -> abrirAdminUsuarios());

        JPanel panelDer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 0));
        panelDer.setOpaque(false);
        panelDer.add(btnAdminUsuarios);
        panelDer.add(lblRolUsuario);
        panelDer.add(btnCerrarSesion);

        header.add(panelDer, BorderLayout.EAST);

        // TabbedPane
        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("SansSerif", Font.PLAIN, 14));

        // Crear los paneles internos (se implementarán más adelante)
        tabbedPane.addTab("Calendario", new PanelCalendario());
        tabbedPane.addTab("Citas", new PanelCitas(usuarioActual.getIdUsuario()));
        tabbedPane.addTab("Clientes", new PanelClientes());
        tabbedPane.addTab("Puertas", new PanelPuertas());
        tabbedPane.addTab("Catálogos", new PanelCatalogos()); // tipo_motor y tipo_servicio
        
        tabbedPane.addTab("Polizas", new PanelPolizas(usuarioActual.getIdUsuario()));

        add(header, BorderLayout.NORTH);
        add(tabbedPane, BorderLayout.CENTER);
    }

    private void configurarPorRol() {
        String rolTexto = (usuarioActual.getRol() == 0) ? "Administrador" : "Encargado";
        lblRolUsuario.setText(rolTexto);
        // Solo administrador puede gestionar usuarios
        btnAdminUsuarios.setVisible(usuarioActual.getRol() == 0);
    }

    private void cerrarSesion() {
        int confirm = JOptionPane.showConfirmDialog(this, "¿Cerrar sesión?", "Salir",
                JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            new login().setVisible(true); // tu ventana de login
            dispose();
        }
    }

   private void abrirAdminUsuarios() {
    PanelUsuarios panel = new PanelUsuarios(usuarioActual, this);
    panel.setVisible(true);
}

    
}