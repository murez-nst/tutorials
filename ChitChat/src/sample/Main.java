package sample;

import com.julisa.ui.SigninPane;
import javafx.application.Application;
import javafx.beans.binding.DoubleExpression;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Accordion;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.function.Function;
/**
 * @author Murez Nasution
 */
public class Main extends Application {
    public final static String STYLE_TITLE =
          "-fx-font-size:25px;"
        + "-fx-text-fill:#9a9a9a;"
        + "-fx-font-family:\"Cambria\";"
        + "-fx-font-weight:bold;"
        + "-fx-effect:innershadow(three-pass-box, rgba(0, 0, 0, 0.7), 6, 0.0, 0, 2)";
    public final static String LOCALHOST;
    public final static Function<TextInputControl, String> TEXT = textCtrl -> {
        String text = textCtrl.getText();
        if((text = text != null && text.length() > 0? text : null) == null)
            textCtrl.requestFocus();
        return text;
    };

    static {
        String localhost;
        try {
            localhost = java.net.InetAddress.getLocalHost().getHostAddress();
        } catch(Exception e) {
            e.printStackTrace();
            localhost = null;
        }
        LOCALHOST = localhost;
    }

    public static void main(String[] args) {
        System.setProperty("java.security.policy", "client.policy");
        System.setProperty("java.rmi.server.useCodebaseOnly", "false");
        System.setProperty("java.rmi.server.codebase", "http://localhost/chitchat/ file:/D:/Murez/Home/Project/Java/ChitChat/auth.jar");
        if(System.getSecurityManager() == null)
            System.setSecurityManager(new SecurityManager());
        try {
            LocateRegistry.getRegistry().rebind("AuthCallback", UnicastRemoteObject.exportObject(SigninPane.getListener(), 0));
        } catch(Exception e) {
            e.printStackTrace();
            return;
        }
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        //Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setScene(new Scene(SigninPane.getInstance(), 800, 400));
        primaryStage.setTitle("Priceless");
        primaryStage.setMinHeight(350);
        primaryStage.setMinWidth(650);
        primaryStage.show();
    }

    public static Pane mainContainer() {
        final GridPane ROOT = new GridPane();
        ROOT.setPadding(new Insets(15, 25, 15, 25));
        ROOT.setVgap(5);
        ROOT.setHgap(3);
        final ListView<String> LIST_VIEW = new ListView<>();
        final TextField TEXTFIELD = new TextField(); {
            TEXTFIELD.setPromptText("Type your message");
            TEXTFIELD.setOnAction(e -> {
                TextField src = (TextField) e.getSource();
                LIST_VIEW.getItems().add(src.getText());
                src.setText("");
            });
        }
        final Accordion ACCORDION = new Accordion(); {
            /*TitledPane[] panes = {
                    new TitledPane("Muhammad Reza Nasution", new Button("1"))
                    ,new TitledPane("Emma Charlotte Duerre Watson", new Button("2"))
                    ,new TitledPane("Julisa Masita Rangkuti", new Button("3"))
            };
            ACCORDION.getPanes().addAll(panes);*/
        }
        ROOT.widthProperty().addListener(
                (observable) -> LIST_VIEW.setPrefWidth(((DoubleExpression) observable).get() - (ACCORDION.getWidth()))
        );
        ROOT.heightProperty().addListener((observable) -> {
            double width = ((DoubleExpression) observable).get();
            ACCORDION.setPrefHeight(width);
            LIST_VIEW.setPrefHeight(width - TEXTFIELD.getHeight());
        });
        ROOT.add(ACCORDION, 0, 0, 1, 2);
        ROOT.add(LIST_VIEW, 1, 0);
        ROOT.add(TEXTFIELD, 1, 1);
        return ROOT;
    }

    /*
    private static Pane signupContainer() {
        final GridPane ROOT = new GridPane();
        ROOT.setAlignment(Pos.CENTER);
        ROOT.setVgap(10);
        ROOT.setHgap(5);
        Label title = new Label("Register now"); {
            title.setStyle(STYLE_TITLE);
        }
        final TextField EMAIL = new TextField(); {
            EMAIL.setPromptText("Email");
            EMAIL.setStyle("-fx-font-size:15px");
        }
        final PasswordField PASSWORD = new PasswordField(); {
            PASSWORD.setPromptText("Password");
            PASSWORD.setStyle("-fx-font-size:15px");
        }
        TextField name = new TextField(); {
            name.setPromptText("Your name");
            name.setStyle("-fx-font-size:15px");
        }
        javafx.scene.layout.HBox submitBox = new javafx.scene.layout.HBox(10); {
            Button submit = new Button("Sign up"); {
                submit.setPrefWidth(120);
                submit.setStyle("-fx-font-size:16px;-fx-padding:5px 20px");
                submit.setOnAction(e -> {

                });
            }
            Button cancel = new Button("Cancel"); {
                cancel.setPrefWidth(120);
                cancel.setStyle("-fx-font-size:16px;-fx-padding:5px 20px");
                cancel.setOnAction(e -> ROOT.getScene().setRoot(signinPane));
            }
            submitBox.getChildren().addAll(cancel, submit);
            submitBox.setAlignment(Pos.CENTER_RIGHT);
        }
        ROOT.widthProperty().addListener(observable -> EMAIL.setPrefWidth(((DoubleExpression) observable).get() - 350));
        ROOT.add(title, 0, 0);
        ROOT.add(EMAIL, 0, 1);
        ROOT.add(PASSWORD, 0, 2);
        ROOT.add(name, 0, 3);
        ROOT.add(submitBox, 0, 4);
        return ROOT;
    }
    */
}