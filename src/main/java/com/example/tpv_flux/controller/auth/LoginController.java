package com.example.tpv_flux.controller.auth;

import com.example.tpv_flux.HelloApplication;
import com.example.tpv_flux.config.DatabaseSeeder;
import com.example.tpv_flux.config.HibernateUtil;
import com.example.tpv_flux.config.SessionManager;
import com.example.tpv_flux.dao.UsuarioDao;
import com.example.tpv_flux.model.Usuario;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.util.Optional;

/**
 * Controlador para la Plantilla de Inicio de Sesión (Mockup Página 1).
 * Autentica contra la tabla 'usuarios' en PostgreSQL y abre el MainLayout.
 */
public class LoginController {

    @FXML
    private TextField txtUsuario;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private ComboBox<String> cmbTerminal;
    @FXML
    private Label lblError;
    @FXML
    private Button btnIniciarSesion;
    @FXML
    private Label lblDbStatus;

    private final UsuarioDao usuarioDao = new UsuarioDao();

    @FXML
    public void initialize() {
        cmbTerminal.setItems(FXCollections.observableArrayList(
                "Sucursal Centro — Caja 1",
                "Sucursal Centro — Caja 2",
                "Sucursal Norte — Caja 1"
        ));
        cmbTerminal.getSelectionModel().selectFirst();

        try {
            if (HibernateUtil.isConnected()) {
                DatabaseSeeder.seedIfEmpty();
                lblDbStatus.setText("v1.0 · Conectado a PostgreSQL local");
            } else {
                lblDbStatus.setText("v1.0 · Modo local de respaldo");
            }
        } catch (Exception e) {
            lblDbStatus.setText("v1.0 · Modo local de respaldo");
        }
    }

    @FXML
    private void onIniciarSesion() {
        String user = txtUsuario.getText();
        String pass = txtPassword.getText();
        String terminal = cmbTerminal.getValue();

        Optional<Usuario> autenticado = usuarioDao.autenticar(user, pass);
        if (autenticado.isEmpty()) {
            lblError.setText("Usuario o contraseña incorrectos (ej. ana.martinez / 123456)");
            lblError.setVisible(true);
            lblError.setManaged(true);
            return;
        }

        lblError.setVisible(false);
        lblError.setManaged(false);

        SessionManager.iniciarSesion(autenticado.get(), terminal);

        try {
            FXMLLoader loader = new FXMLLoader(
                    HelloApplication.class.getResource("/com/example/tpv_flux/views/layout/MainLayout.fxml")
            );
            Parent mainRoot = loader.load();
            Scene currentScene = btnIniciarSesion.getScene();
            if (currentScene != null) {
                currentScene.setRoot(mainRoot);
            }
        } catch (Exception e) {
            lblError.setText("Error al abrir el panel principal: " + e.getMessage());
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }
}
