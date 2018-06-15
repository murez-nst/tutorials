package com.julisa.ui;

import com.julisa.remote.EventListenable;
import com.murez.entity.DataPackage;
import com.murez.remote.Authenticable;
import javafx.application.Platform;
import javafx.beans.binding.DoubleExpression;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import java.rmi.registry.LocateRegistry;
import static sample.Main.*;
/**
 * @author Murez Nasution
 */
public class SigninPane {
    private static SigninPane instance;

    public static synchronized Pane getInstance() {
        return (instance == null? instance = new SigninPane() : instance).ROOT;
    }

    public static synchronized EventListenable getListener() {
        return (instance == null? instance = new SigninPane() : instance).listener;
    }

    private final VBox BOX;
    private final Label NOTIFIER;
    private final GridPane ROOT;
    private final Button SUBMIT;
    private final TextField USERNAME, PASSWORD;
    private EventListenable listener;

    private SigninPane() {
        EventImplementer listener = new EventImplementer();
        (USERNAME = new TextField()).setPromptText("Your Email");
            USERNAME.setStyle("-fx-font-size:15px");
            USERNAME.setOnAction(listener);
        (PASSWORD = new PasswordField()).setPromptText("Password");
            PASSWORD.setStyle("-fx-font-size:15px");
            PASSWORD.setOnAction(listener);
        (NOTIFIER = new Label()).setStyle("-fx-text-fill:red");
        (SUBMIT = new Button("Sign in")).setPrefWidth(120);
            SUBMIT.setStyle("-fx-font-size:16px;-fx-padding:5px 20px");
            SUBMIT.setOnAction(listener);
        HBox box = new HBox(20);
            box.setAlignment(Pos.CENTER_RIGHT);
            box.getChildren().addAll(NOTIFIER, SUBMIT);
        Hyperlink register = new Hyperlink("Don't you have an account? Register now");
        //register.setOnAction(e -> ROOT.getScene().setRoot(signupContainer()));
        (BOX = new VBox(10)).setAlignment(Pos.CENTER_RIGHT);
            BOX.getChildren().addAll(box, register);
        Label title = new Label("Welcome to Priceless");
            title.setStyle(STYLE_TITLE);
        (ROOT = new GridPane()).setPadding(new Insets(85, 25, 15, 25));
            ROOT.setAlignment(Pos.TOP_CENTER);
            ROOT.setVgap(10);
            ROOT.setHgap(5);
            ROOT.widthProperty().addListener(observable -> USERNAME.setPrefWidth(((DoubleExpression) observable).get() - 350));
            ROOT.add(title, 0, 0);
            ROOT.add(USERNAME, 0, 1);
            ROOT.add(PASSWORD, 0, 2);
            ROOT.add(BOX, 0, 3);
    }

    private class EventImplementer implements EventHandler<ActionEvent> {
        private final String NAME = "Authentication";
        private ProgressIndicator indicator;
        private boolean active;

        private EventImplementer() {
            listener = (response) -> {
                final int CODE = response.getNumber(0).intValue();
                if(CODE == 200) {
                    if(response.getPackage().get("session") == null) {
                        Platform.runLater(() -> {
                            NOTIFIER.setText(response.getPackage().get("message"));
                            USERNAME.requestFocus();
                        });
                        USERNAME.setEditable(true);
                        USERNAME.setOpacity(1);
                        PASSWORD.setEditable(true);
                        PASSWORD.setOpacity(1);
                        SUBMIT.setDisable(false);
                    } else {
                        Platform.runLater(() -> {
                            NOTIFIER.setText("Hello, " + response.getPackage().get("name"));
                            NOTIFIER.setStyle("-fx-text-fill:green");
                            NOTIFIER.requestFocus();
                        });
                    }
                    USERNAME.setText("");
                    PASSWORD.setText("");
                }
                Platform.runLater(() -> BOX.getChildren().remove(indicator));
                synchronized(NAME) {
                    if(active) active = false;
                }
            };
        }

        @Override
        public void handle(ActionEvent event) {
            String username, password;
            if((username = TEXT.apply(USERNAME)) == null) return;
            if((password = TEXT.apply(PASSWORD)) == null) return;
            synchronized(NAME) {
                if(!active)
                    active = !active;
                else
                    return;
            }
            BOX.getChildren().add(indicator = new ProgressIndicator());
            USERNAME.setEditable(false);
            PASSWORD.setEditable(false);
            SUBMIT.setDisable(true);
            USERNAME.setOpacity(.5);
            PASSWORD.setOpacity(.5);
            DataPackage<String> dataPack = DataPackage.create(0, LOCALHOST);
            dataPack.getPackage().put("name", "AuthCallback");
            try {
                final Authenticable REMOTE;
                try {
                    REMOTE = (Authenticable) LocateRegistry.getRegistry().lookup(NAME);
                } catch(java.rmi.NotBoundException e) {
                    throw new UnsupportedOperationException("There's no remote reference for name \"" + NAME + "\"", e);
                }
                REMOTE.signin(username, password, dataPack);
            } catch(Exception e) { e.printStackTrace(); }
        }
    }
}