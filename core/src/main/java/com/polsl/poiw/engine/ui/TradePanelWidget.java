package com.polsl.poiw.engine.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Button;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.Window;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Scaling;
import com.polsl.poiw.engine.inventory.InventoryStack;
import com.polsl.poiw.engine.inventory.ItemDefinition;
import com.polsl.poiw.gameplay.trade.TradeOfferDefinition;

import java.util.ArrayList;
import java.util.List;

public class TradePanelWidget extends UserWidget {
    private static final float CONTENT_FONT_SCALE = 0.46f;
    private static final float SLOT_SIZE = 14f;
    private static final float SLOT_PADDING = 0.6f;
    private static final float ICON_SIZE = 8f;
    private static final int SELL_SLOT_COUNT = 4;
    private static final float OFFERS_WIDTH = 162f;
    private static final float OFFERS_HEIGHT = 94f;
    private static final float ITEM_COLUMN_WIDTH = 70f;
    private static final float PRICE_COLUMN_WIDTH = 30f;
    private static final float BUTTON_COLUMN_WIDTH = 28f;
    private static final float BUY_BUTTON_WIDTH = 26f;
    private static final float BUY_BUTTON_HEIGHT = 14f;

    public interface TradePanelActionListener {
        void onBuyRequested(int traderSlotIndex, String itemId);
        void onSellRequested();
        void onSellSlotTransferRequested(int slotIndex, String itemId, boolean wholeStack);
    }

    public record TradeOfferView(
        int traderSlotIndex,
        ItemDefinition itemDefinition,
        int quantity,
        String buyPriceLabel,
        String sellPriceLabel
    ) {}

    private final Skin skin;
    private final TextureAtlas itemsAtlas;
    private final Window window;
    private final Label traderLabel;
    private final ScrollPane offersScrollPane;
    private final Table offersTable;
    private final Table sellSlotsTable;
    private final TextButton sellButton;

    private TradePanelActionListener actionListener;
    private List<TradeOfferView> offers = List.of();
    private List<InventoryStack> sellItems = List.of();

    public TradePanelWidget(Skin skin, TextureAtlas itemsAtlas) {
        super();
        this.skin = skin;
        this.itemsAtlas = itemsAtlas;
        this.window = new Window("Handel", skin);
        this.window.setMovable(false);

        this.traderLabel = new Label("Kupiec", UiSkinStyles.resolveLabelStyle(skin, "default"));
        this.traderLabel.setColor(Color.WHITE);
        this.traderLabel.setFontScale(0.56f);

        this.offersTable = new Table();
        this.offersTable.top().left();

        ScrollPane.ScrollPaneStyle scrollStyle = skin.has("default", ScrollPane.ScrollPaneStyle.class)
            ? new ScrollPane.ScrollPaneStyle(skin.get("default", ScrollPane.ScrollPaneStyle.class))
            : new ScrollPane.ScrollPaneStyle();
        scrollStyle.vScroll = null;
        scrollStyle.vScrollKnob = null;
        scrollStyle.hScroll = null;
        scrollStyle.hScrollKnob = null;

        this.offersScrollPane = new ScrollPane(offersTable, scrollStyle);
        this.offersScrollPane.setFadeScrollBars(false);
        this.offersScrollPane.setScrollingDisabled(true, false);
        this.offersScrollPane.setOverscroll(false, false);
        this.offersScrollPane.setForceScroll(false, true);
        this.offersScrollPane.setFlickScroll(false);
        this.offersScrollPane.setScrollbarsVisible(false);
        this.offersScrollPane.setScrollbarsOnTop(true);
        this.offersScrollPane.setCancelTouchFocus(false);
        this.offersScrollPane.setScrollBarTouch(false);

        Label sellLabel = new Label("Towary do sprzedazy", UiSkinStyles.resolveLabelStyle(skin, "default"));
        sellLabel.setFontScale(0.5f);

        this.sellSlotsTable = new Table();
        this.sellButton = new TextButton("Sprzedaj", UiSkinStyles.copyTextButtonStyle(skin, "default"));
        this.sellButton.getLabel().setFontScale(0.6f);
        this.sellButton.addListener(new ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                if (actionListener != null) {
                    actionListener.onSellRequested();
                }
            }
        });

        Table content = new Table();
        content.defaults().left().padBottom(1f);
        content.add(traderLabel).left().padTop(1f).row();
        content.add(offersScrollPane).width(OFFERS_WIDTH).height(OFFERS_HEIGHT).left().padBottom(2f).row();
        content.add(sellLabel).left().padTop(1f).row();
        content.add(sellSlotsTable).left().padBottom(1f).row();
        content.add(sellButton).right().width(50f).height(BUY_BUTTON_HEIGHT).padTop(1f).row();

        window.add(content).pad(4f);
        window.pack();

        addActor(window);
        rebuildOffers();
        rebuildSellSlots();
        syncSize();
        setVisibility(EVisibility.HIDDEN);
    }

    public void setActionListener(TradePanelActionListener actionListener) {
        this.actionListener = actionListener;
    }

    public void setTraderName(String traderName) {
        traderLabel.setText(traderName != null && !traderName.isBlank() ? traderName : "Kupiec");
        syncSize();
    }

    public void setOffers(List<TradeOfferView> offers) {
        this.offers = offers != null ? List.copyOf(offers) : List.of();
        rebuildOffers();
    }

    public void setSellItems(List<InventoryStack> sellItems) {
        this.sellItems = sellItems != null ? List.copyOf(sellItems) : List.of();
        rebuildSellSlots();
    }

    @Override
    public void construct() {
        super.construct();
        syncScrollFocus();
    }

    @Override
    public void destruct() {
        clearScrollFocus();
        super.destruct();
    }

    @Override
    public void setVisibility(EVisibility visibility) {
        super.setVisibility(visibility);
        syncScrollFocus();
    }

    private void rebuildOffers() {
        offersTable.clearChildren();

        Label headerItem = new Label("Przedmiot", scaledLabelStyle());
        Label headerBuy = new Label("Kupno", scaledLabelStyle());
        Label headerSell = new Label("Sprzedaz", scaledLabelStyle());
        headerItem.setColor(Color.WHITE);
        headerBuy.setColor(Color.WHITE);
        headerSell.setColor(Color.WHITE);

        offersTable.add(headerItem).left().width(ITEM_COLUMN_WIDTH).padBottom(1f).padRight(2f);
        offersTable.add(headerBuy).left().width(PRICE_COLUMN_WIDTH).padBottom(1f).padRight(2f);
        offersTable.add(headerSell).left().width(PRICE_COLUMN_WIDTH).padBottom(1f).padRight(2f);
        offersTable.add().width(BUTTON_COLUMN_WIDTH);
        offersTable.row();

        for (TradeOfferView offer : offers) {
            ItemDefinition definition = offer.itemDefinition();
            String itemLabel = definition != null
                ? definition.getDisplayName() + " (" + Math.max(0, offer.quantity()) + ")"
                : "Brak";

            Label itemNameLabel = new Label(itemLabel, scaledLabelStyle());
            Label buyLabel = new Label(offer.buyPriceLabel(), scaledLabelStyle());
            Label sellLabel = new Label(offer.sellPriceLabel(), scaledLabelStyle());
            TextButton buyButton = new TextButton("Kup", UiSkinStyles.copyTextButtonStyle(skin, "default"));
            buyButton.getLabel().setFontScale(0.55f);
            buyButton.setDisabled(offer.quantity() <= 0 || definition == null);
            buyButton.addListener(new ClickListener() {
                @Override
                public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y,
                                         int pointer, int buttonCode) {
                    if (buttonCode == Input.Buttons.LEFT
                        && actionListener != null
                        && definition != null
                        && offer.quantity() > 0
                        && !buyButton.isDisabled()) {
                        actionListener.onBuyRequested(offer.traderSlotIndex(), definition.getItemId());
                        return true;
                    }
                    return super.touchDown(event, x, y, pointer, buttonCode);
                }
            });

            offersTable.add(itemNameLabel).left().width(ITEM_COLUMN_WIDTH).padBottom(0.8f).padRight(2f);
            offersTable.add(buyLabel).left().width(PRICE_COLUMN_WIDTH).padBottom(0.8f).padRight(2f);
            offersTable.add(sellLabel).left().width(PRICE_COLUMN_WIDTH).padBottom(0.8f).padRight(2f);
            offersTable.add(buyButton).width(BUY_BUTTON_WIDTH).height(BUY_BUTTON_HEIGHT).padBottom(0.8f);
            offersTable.row();
        }

        offersTable.pack();
        offersScrollPane.layout();
        syncSize();
    }

    private void rebuildSellSlots() {
        sellSlotsTable.clearChildren();
        List<InventoryStack> paddedItems = new ArrayList<>(sellItems);
        while (paddedItems.size() < SELL_SLOT_COUNT) {
            paddedItems.add(null);
        }

        for (int index = 0; index < SELL_SLOT_COUNT; index++) {
            sellSlotsTable.add(createSellSlot(paddedItems.get(index)))
                .size(SLOT_SIZE, SLOT_SIZE)
                .pad(SLOT_PADDING);
        }

        sellSlotsTable.pack();
        syncSize();
    }

    private Button createSellSlot(InventoryStack stack) {
        Button button = new Button(UiSkinStyles.copyButtonStyle(skin, "default"));
        if (stack == null || stack.getDefinition() == null) {
            button.setDisabled(true);
            return button;
        }

        Image icon = createIcon(stack.getDefinition());
        Label quantityLabel = new Label(String.valueOf(stack.getQuantity()), UiSkinStyles.resolveLabelStyle(skin, "list"));
        quantityLabel.setFontScale(0.6f);

        Table content = new Table();
        content.add(icon).size(ICON_SIZE, ICON_SIZE);
        content.row();
        content.add(quantityLabel).right();
        button.add(content).grow();
        button.addListener(new ClickListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer, int buttonCode) {
                if (buttonCode == Input.Buttons.RIGHT && actionListener != null) {
                    actionListener.onSellSlotTransferRequested(
                        stack.getSlotIndex(),
                        stack.getDefinition().getItemId(),
                        isShiftPressed()
                    );
                    return true;
                }
                return super.touchDown(event, x, y, pointer, buttonCode);
            }
        });
        return button;
    }

    private Image createIcon(ItemDefinition definition) {
        TextureRegion region = itemsAtlas != null && definition != null
            ? itemsAtlas.findRegion(definition.getTextureRegionName())
            : null;
        if (region != null) {
            Image icon = new Image(region);
            icon.setScaling(Scaling.contain);
            return icon;
        }

        Image fallback = new Image(skin.newDrawable("white", definition != null ? definition.getDisplayColor() : Color.WHITE));
        fallback.setScaling(Scaling.stretch);
        return fallback;
    }

    private Label.LabelStyle scaledLabelStyle() {
        Label.LabelStyle style = UiSkinStyles.copyScaledLabelStyle(skin, "font", CONTENT_FONT_SCALE);
        style.fontColor = Color.WHITE;
        return style;
    }

    private boolean isShiftPressed() {
        return Gdx.input != null
            && (Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT));
    }

    private void syncSize() {
        window.pack();
        window.setSize(window.getPrefWidth(), window.getPrefHeight());
        setSize(window.getWidth(), window.getHeight());
    }

    private void syncScrollFocus() {
        if (root.getStage() == null) {
            return;
        }

        if (isVisible()) {
            root.getStage().setScrollFocus(offersScrollPane);
        } else {
            clearScrollFocus();
        }
    }

    private void clearScrollFocus() {
        if (root.getStage() != null && root.getStage().getScrollFocus() == offersScrollPane) {
            root.getStage().setScrollFocus(null);
        }
    }
}
