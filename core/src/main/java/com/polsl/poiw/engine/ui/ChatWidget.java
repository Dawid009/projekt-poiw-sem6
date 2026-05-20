package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.polsl.poiw.GameInstance;
import com.polsl.poiw.shared.protocol.NetworkProtocol;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * ChatWidget — Minecraft-style multiplayer chat.
 * <p>
 * Passive mode: shows last few messages at bottom-left, no background, fading.
 * Active mode (T): full chat box with background, scrollable messages, input field.
 */
public class ChatWidget extends UserWidget {

    private static final float WIDTH = 320f;
    private static final float ACTIVE_HEIGHT = 180f;
    private static final float INPUT_HEIGHT = 26f;
    private static final int MAX_VISIBLE_MESSAGES = 50;
    private static final int PASSIVE_VISIBLE_COUNT = 5;
    private static final float PASSIVE_FADE_TIME = 8f;
    private static final float FONT_SCALE = 0.8f;

    private final Skin skin;

    // Active mode UI
    private Table activeContainer;
    private Table activeMessagesTable;
    private ScrollPane activeScrollPane;
    private TextField inputField;

    // Passive mode UI (recent messages overlay)
    private Table passiveContainer;
    private final List<PassiveEntry> passiveEntries = new ArrayList<>();

    private Consumer<String> sendCallback;
    private boolean inputActive = false;
    private boolean pendingDeactivate = false;

    // Stores all messages for re-rendering
    private final List<NetworkProtocol.ChatMessage> allMessages = new ArrayList<>();

    public ChatWidget(Skin skin) {
        super();
        this.skin = skin;
        root.setTouchable(Touchable.childrenOnly);
        buildUI();
    }

    @Override
    public void setVisibility(EVisibility visibility) {
        super.setVisibility(visibility);
        // Never let the root intercept touches — only children (input field) should
        root.setTouchable(Touchable.childrenOnly);
    }

    private void buildUI() {
        buildActiveUI();
        buildPassiveUI();

        // Start in passive mode
        activeContainer.setVisible(false);
        passiveContainer.setVisible(true);

        setSize(WIDTH, ACTIVE_HEIGHT + INPUT_HEIGHT + 4f);
    }

    private void buildActiveUI() {
        activeContainer = new Table();
        activeContainer.setSize(WIDTH, ACTIVE_HEIGHT + INPUT_HEIGHT + 4f);
        activeContainer.bottom().left();

        Drawable bg = skin.newDrawable("white", new Color(0, 0, 0, 0.45f));
        activeContainer.setBackground(bg);

        activeMessagesTable = new Table();
        activeMessagesTable.bottom().left();
        activeMessagesTable.pad(3f);

        activeScrollPane = new ScrollPane(activeMessagesTable, skin);
        activeScrollPane.setFadeScrollBars(true);
        activeScrollPane.setScrollingDisabled(true, false);
        activeScrollPane.setOverscroll(false, false);
        activeScrollPane.setForceScroll(false, true);
        activeScrollPane.setScrollbarsVisible(false);

        inputField = new TextField("", skin);
        inputField.setMaxLength(NetworkProtocol.MAX_CHAT_MESSAGE_LENGTH);
        inputField.setMessageText("Press Enter to send...");

        inputField.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == com.badlogic.gdx.Input.Keys.ENTER) {
                    sendCurrentMessage();
                    return true;
                }
                if (keycode == com.badlogic.gdx.Input.Keys.ESCAPE) {
                    deactivateInput();
                    return true;
                }
                return false;
            }
        });

        activeContainer.add(activeScrollPane).width(WIDTH - 4f).height(ACTIVE_HEIGHT).left().pad(2f).row();
        activeContainer.add(inputField).width(WIDTH - 4f).height(INPUT_HEIGHT).left().pad(2f);

        addActor(activeContainer);
    }

    private void buildPassiveUI() {
        passiveContainer = new Table();
        passiveContainer.setSize(WIDTH, ACTIVE_HEIGHT + INPUT_HEIGHT + 4f);
        passiveContainer.bottom().left();
        passiveContainer.pad(4f);
        passiveContainer.setTouchable(Touchable.disabled);

        addActor(passiveContainer);
    }

    @Override
    public void tick(float delta) {
        super.tick(delta);

        // Deferred deactivation — clears chatInputActive one frame after Escape,
        // so MainPlayerController.tick() still sees chatInputActive=true this frame.
        if (pendingDeactivate) {
            pendingDeactivate = false;
            GameInstance.setChatInputActive(false);
        }

        if (!inputActive) {
            // Fade passive entries
            boolean changed = false;
            for (int i = passiveEntries.size() - 1; i >= 0; i--) {
                PassiveEntry entry = passiveEntries.get(i);
                entry.age += delta;
                if (entry.age > PASSIVE_FADE_TIME) {
                    passiveEntries.remove(i);
                    changed = true;
                } else if (entry.age > PASSIVE_FADE_TIME - 2f) {
                    float alpha = (PASSIVE_FADE_TIME - entry.age) / 2f;
                    entry.label.getColor().a = alpha;
                }
            }
            if (changed) {
                rebuildPassiveDisplay();
            }
        }
    }

    public void addMessage(NetworkProtocol.ChatMessage msg) {
        // Store message
        allMessages.add(msg);
        while (allMessages.size() > MAX_VISIBLE_MESSAGES) {
            allMessages.remove(0);
        }

        String text;
        Color color;
        if (msg.type == NetworkProtocol.ChatMessageType.SYSTEM) {
            text = "* " + msg.message;
            color = Color.YELLOW.cpy();
        } else {
            text = msg.playerName + ": " + msg.message;
            color = Color.WHITE.cpy();
        }

        // Add to active scroll
        Label activeLabel = new Label(text, skin);
        activeLabel.setColor(color);
        activeLabel.setWrap(true);
        activeLabel.setFontScale(FONT_SCALE);
        activeMessagesTable.add(activeLabel).width(WIDTH - 16f).left().padBottom(1f).row();

        while (activeMessagesTable.getCells().size > MAX_VISIBLE_MESSAGES) {
            activeMessagesTable.getCells().first().getActor().remove();
            activeMessagesTable.getCells().removeIndex(0);
        }

        activeScrollPane.layout();
        activeScrollPane.setScrollPercentY(1f);

        // Add to passive display
        Label passiveLabel = new Label(text, skin);
        passiveLabel.setColor(color);
        passiveLabel.setWrap(true);
        passiveLabel.setFontScale(FONT_SCALE);

        passiveEntries.add(new PassiveEntry(passiveLabel, 0f));
        while (passiveEntries.size() > PASSIVE_VISIBLE_COUNT) {
            passiveEntries.remove(0);
        }
        rebuildPassiveDisplay();
    }

    private void rebuildPassiveDisplay() {
        passiveContainer.clearChildren();
        for (PassiveEntry entry : passiveEntries) {
            passiveContainer.add(entry.label).width(WIDTH - 8f).left().padBottom(1f).row();
        }
    }

    public void loadHistory(List<NetworkProtocol.ChatMessage> history) {
        allMessages.clear();
        activeMessagesTable.clearChildren();
        passiveEntries.clear();
        passiveContainer.clearChildren();
        for (NetworkProtocol.ChatMessage msg : history) {
            addMessage(msg);
        }
    }

    public void setSendCallback(Consumer<String> callback) {
        this.sendCallback = callback;
    }

    public void activateInput() {
        inputActive = true;
        GameInstance.setChatInputActive(true);
        activeContainer.setVisible(true);
        passiveContainer.setVisible(false);

        // Scroll to bottom in active view
        activeScrollPane.layout();
        activeScrollPane.setScrollPercentY(1f);

        if (root.getStage() != null) {
            root.getStage().setKeyboardFocus(inputField);
            root.getStage().setScrollFocus(activeScrollPane);
        }
    }

    public void deactivateInput() {
        inputActive = false;
        pendingDeactivate = true;  // clear chatInputActive next frame
        inputField.setText("");
        activeContainer.setVisible(false);
        passiveContainer.setVisible(true);

        // Reset passive timers so recent messages show fresh
        for (PassiveEntry e : passiveEntries) {
            e.age = 0f;
            e.label.getColor().a = 1f;
        }

        if (root.getStage() != null) {
            root.getStage().setKeyboardFocus(null);
            root.getStage().setScrollFocus(null);
        }
    }

    public boolean isInputActive() {
        return inputActive;
    }

    private void sendCurrentMessage() {
        String text = inputField.getText().trim();
        if (!text.isEmpty() && sendCallback != null) {
            sendCallback.accept(text);
        }
        inputField.setText("");
        deactivateInput();
    }

    private static class PassiveEntry {
        final Label label;
        float age;

        PassiveEntry(Label label, float age) {
            this.label = label;
            this.age = age;
        }
    }
}
