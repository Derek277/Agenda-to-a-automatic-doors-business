/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package innovatec;

import InterfacesPrincipales.login;
import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.UIManager;

/**
 *
 * @author Derek
 */
public class innovatec {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        try {
            // Usamos FlatLightLaf (tema claro) en lugar de FlatDarkLaf
            UIManager.setLookAndFeel(new FlatLightLaf());

        } catch (Exception e) {
            e.printStackTrace();
        }

        java.awt.EventQueue.invokeLater(() -> {
            new login().setVisible(true);
        });
    }
    
}
