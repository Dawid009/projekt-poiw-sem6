package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.polsl.poiw.engine.auth.AuthService;

public class AuthPanelWidget extends UserWidget {

    private static final float CONTENT_FONT_SCALE = 0.5f;
    private static final float FIELD_WIDTH = 76f;
    private static final float FIELD_HEIGHT = 12f;
    private static final float BUTTON_WIDTH = 56f;
    private static final float BUTTON_HEIGHT = 12f;
    private static final float CONTENT_PADDING = 3f;
    private static final float ROW_SPACING = 1f;

    private final Window window;
    private final TextField loginField;
    private final TextField passwordField;
    private final TextField emailField;
    private final Label statusLabel;
    private final Table emailRow;
    private final Cell<Table> emailRowCell;
    private final TextButton submitButton;
    private final TextButton modeButton;

    private AuthPanelActionListener actionListener;
    private boolean registerMode;

    public AuthPanelWidget(Skin skin) {
        super();

        this.window = new Window("Logowanie", skin, "atlas");
        this.window.setMovable(false);
        UiSkinStyles.centerWindowTitle(window);

        Label.LabelStyle labelStyle = UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE);
        TextField.TextFieldStyle fieldStyle = UiSkinStyles.copyCompactTextFieldStyle(skin, "atlas", "font", 26f, 12f, CONTENT_FONT_SCALE);
        TextButton.TextButtonStyle buttonStyle = UiSkinStyles.copyCompactTextButtonStyle(skin, "atlas", "font", 20f, 12f, CONTENT_FONT_SCALE);

        Label loginLabel = new Label("Login", labelStyle);
        Label passwordLabel = new Label("Haslo", labelStyle);
        Label emailLabel = new Label("Email", labelStyle);
        this.statusLabel = new Label("", labelStyle);
        this.statusLabel.setWrap(true);
        this.statusLabel.setVisible(false);

        this.loginField = new TextField("", fieldStyle);
        this.loginField.setMessageText("login");

        this.passwordField = new TextField("", fieldStyle);
        this.passwordField.setMessageText("haslo");
        this.passwordField.setPasswordMode(true);
        this.passwordField.setPasswordCharacter('*');

        this.emailField = new TextField("", fieldStyle);
        this.emailField.setMessageText("email@adres.pl");

        this.submitButton = new TextButton("Zaloguj", buttonStyle);
        this.modeButton = new TextButton("Rejestracja", buttonStyle);

        Table content = new Table();
        content.defaults().left().padBottom(ROW_SPACING);
        content.add(loginLabel).padRight(4f).padTop(10f);
        content.add(loginField).width(FIELD_WIDTH).height(FIELD_HEIGHT).padTop(10f).left().row();
        content.add(passwordLabel).padRight(4f);
        content.add(passwordField).width(FIELD_WIDTH).height(FIELD_HEIGHT).left().row();

        emailRow = new Table();
        emailRow.add(emailLabel).left().padRight(4f);
        emailRow.add(emailField).width(FIELD_WIDTH).height(FIELD_HEIGHT).left();
        emailRowCell = content.add(emailRow).colspan(2).left().padBottom(ROW_SPACING);
        content.row();

        Table buttons = new Table();
        buttons.defaults().width(BUTTON_WIDTH).height(BUTTON_HEIGHT);
        buttons.add(submitButton).padRight(2f);
        buttons.add(modeButton);

        content.add(buttons).colspan(2).center().padTop(1.5f).padBottom(1.5f).row();
        content.add(statusLabel).colspan(2).width(116f).left().row();

        window.add(content).pad(4f, CONTENT_PADDING, CONTENT_PADDING, CONTENT_PADDING);
        addActor(window);
        setRegisterMode(false);
        syncSize();

        submitButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener == null) {
                    return;
                }

                if (registerMode) {
                    actionListener.onRegisterRequested(
                        loginField.getText().trim(),
                        emailField.getText().trim(),
                        passwordField.getText()
                    );
                } else {
                    actionListener.onLoginRequested(loginField.getText().trim(), passwordField.getText());
                }
            }
        });

        modeButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                setRegisterMode(!registerMode);
                clearStatus();
            }
        });
    }

    public void setActionListener(AuthPanelActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setRememberedCredentials(AuthService.RememberedCredentials credentials) {
        if (credentials == null) {
            loginField.setText("");
            passwordField.setText("");
            emailField.setText("");
            return;
        }

        loginField.setText(credentials.login() != null ? credentials.login() : "");
        passwordField.setText(credentials.password() != null ? credentials.password() : "");
        emailField.setText(credentials.email() != null ? credentials.email() : "");
    }

    public void setBusy(boolean busy) {
        loginField.setDisabled(busy);
        passwordField.setDisabled(busy);
        emailField.setDisabled(busy);
        submitButton.setDisabled(busy);
        modeButton.setDisabled(busy);
    }

    public void resetToLoginMode() {
        setRegisterMode(false);
    }

    public void clearStatus() {
        statusLabel.setText("");
        statusLabel.setVisible(false);
        syncSize();
    }

    public void setStatus(String text, Color color) {
        statusLabel.setText(text == null ? "" : text);
        statusLabel.setColor(color == null ? Color.WHITE : color);
        statusLabel.setVisible(text != null && !text.isBlank());
        syncSize();
    }

    private void setRegisterMode(boolean enabled) {
        this.registerMode = enabled;
        window.getTitleLabel().setText(enabled ? "Rejestracja" : "Logowanie");
        submitButton.setText(enabled ? "Zarejestruj" : "Zaloguj");
        modeButton.setText(enabled ? "Logowanie" : "Rejestracja");
        UiSkinStyles.centerWindowTitle(window);

        emailRow.pack();
        emailRow.setVisible(enabled);
        emailRowCell.width(enabled ? emailRow.getPrefWidth() : 0f);
        emailRowCell.height(enabled ? emailRow.getPrefHeight() : 0f);
        emailRowCell.padBottom(enabled ? ROW_SPACING : 0f);
        syncSize();
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }

    public interface AuthPanelActionListener {
        void onLoginRequested(String login, String password);
        void onRegisterRequested(String login, String email, String password);
    }
}