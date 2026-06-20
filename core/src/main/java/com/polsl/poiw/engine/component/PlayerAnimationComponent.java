package com.polsl.poiw.engine.component;

import com.badlogic.ashley.core.ComponentMapper;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.polsl.poiw.gameplay.tool.PlayerToolType;

import java.util.EnumMap;

/**
 * Trzyma animacje gracza i wybiera odpowiednia klatke zależnie od kierunku, ruchu i ataku.
 * To nie rysuje nic samo z siebie, tylko daje gotowy frame dla systemu renderowania.
 */
public class PlayerAnimationComponent extends AbstractActorComponent {
    public static final ComponentMapper<PlayerAnimationComponent> MAPPER =
        ComponentMapper.getFor(PlayerAnimationComponent.class);

    public enum Direction {
        DOWN,
        LEFT,
        RIGHT,
        UP
    }

    private static final float WALK_FRAME_DURATION = 0.10f;
    private static final float IDLE_FRAME_DURATION = 0.16f;
    private static final float SWORD_ATTACK_FRAME_DURATION = 0.05f;
    private static final float TOOL_ACTION_FRAME_DURATION = 0.10f;
    private static final float DAMAGE_FLASH_DURATION = 0.12f;
    private static final float MOVE_EPSILON = 0.001f;

    private final Animation<TextureRegion> idleDown;
    private final Animation<TextureRegion> idleLeft;
    private final Animation<TextureRegion> idleRight;
    private final Animation<TextureRegion> idleUp;
    private final Animation<TextureRegion> walkDown;
    private final Animation<TextureRegion> walkLeft;
    private final Animation<TextureRegion> walkRight;
    private final Animation<TextureRegion> walkUp;
    private final EnumMap<PlayerToolType, DirectionalAnimationSet> attackAnimations =
        new EnumMap<>(PlayerToolType.class);

    private Direction facingDirection = Direction.DOWN;
    private PlayerToolType currentAttackTool = PlayerToolType.SWORD;
    private boolean moving = false;
    private boolean attacking = false;
    private float stateTime = 0f;
    private float damageFlashRemaining = 0f;
    private float attackVisualRemaining = 0f;

    public PlayerAnimationComponent(TextureAtlas atlas, TextureAtlas playerActionsAtlas) {
        this.idleDown = createLoopAnimation(atlas, "player_idle/idle_down", IDLE_FRAME_DURATION);
        this.idleLeft = createLoopAnimation(atlas, "player_idle/idle_left", IDLE_FRAME_DURATION);
        this.idleRight = createLoopAnimation(atlas, "player_idle/idle_right", IDLE_FRAME_DURATION);
        this.idleUp = createLoopAnimation(atlas, "player_idle/idle_up", IDLE_FRAME_DURATION);
        this.walkDown = createLoopAnimation(atlas, "player_walk/walk_down", WALK_FRAME_DURATION);
        this.walkLeft = createLoopAnimation(atlas, "player_walk/walk_left", WALK_FRAME_DURATION);
        this.walkRight = createLoopAnimation(atlas, "player_walk/walk_right", WALK_FRAME_DURATION);
        this.walkUp = createLoopAnimation(atlas, "player_walk/walk_up", WALK_FRAME_DURATION);
        buildAttackAnimations(playerActionsAtlas);
    }

    /** Aktualizuje stan animacji na podstawie samego kierunku ruchu. */
    public void update(Vector2 direction, float delta) {
        update(direction, 1f, delta);
    }

    /** Aktualizuje stan animacji i pozwala przyspieszyc klatki chodzenia. */
    public void update(Vector2 direction, float movementAnimationSpeedScale, float delta) {
        boolean currentlyMoving = direction != null && !direction.isZero(MOVE_EPSILON);
        Direction resolvedDirection = resolveDirection(direction);

        applyState(resolvedDirection, currentlyMoving, false, movementAnimationSpeedScale, delta);
    }

    /** Ustawia stan animacji recznie, np. gdy postac atakuje. */
    public void applyState(Direction direction, boolean currentlyMoving, boolean currentlyAttacking, float delta) {
        applyState(direction, currentlyMoving, currentlyAttacking, 1f, delta);
    }

    /**
     * Glowny update animacji.
     * Pilnuje, kiedy trzeba zresetowac licznik czasu, a kiedy tylko go przesunac dalej.
     */
    public void applyState(
        Direction direction,
        boolean currentlyMoving,
        boolean currentlyAttacking,
        float movementAnimationSpeedScale,
        float delta
    ) {
        Direction resolvedDirection = direction != null ? direction : facingDirection;
        boolean effectiveAttacking = currentlyAttacking || attackVisualRemaining > 0f;

        if (resolvedDirection != facingDirection
            || currentlyMoving != moving
            || effectiveAttacking != attacking) {
            facingDirection = resolvedDirection;
            moving = currentlyMoving;
            attacking = effectiveAttacking;
            stateTime = 0f;
        } else {
            stateTime += resolveStateTimeDelta(delta, currentlyMoving, effectiveAttacking, movementAnimationSpeedScale);
        }
    }

    /** Zwraca klatke, która w danym momencie powinna być narysowana. */
    public TextureRegion getCurrentFrame() {
        return getCurrentAnimation().getKeyFrame(stateTime);
    }

    /** Startuje animacje ataku mieczem, bez podawania narzedzia. */
    public void startAttack(Direction direction) {
        startAttack(direction, PlayerToolType.SWORD);
    }

    /** Startuje animacje ataku dla konkretnego narzedzia. */
    public void startAttack(Direction direction, PlayerToolType toolType) {
        Direction resolvedDirection = direction != null ? direction : facingDirection;
        currentAttackTool = toolType != null ? toolType : PlayerToolType.SWORD;
        attackVisualRemaining = getAttackAnimation(currentAttackTool, resolvedDirection).getAnimationDuration();
        facingDirection = resolvedDirection;
        moving = false;
        attacking = true;
        stateTime = 0f;
    }

    /** Odpalany przy otrzymaniu damage flasha na czerwono. */
    public void triggerDamageFlash() {
        damageFlashRemaining = DAMAGE_FLASH_DURATION;
    }

    /** Zmniejsza czas trwania czerwonego flasha. */
    public void tickDamageFlash(float delta) {
        if (damageFlashRemaining > 0f) {
            damageFlashRemaining = Math.max(0f, damageFlashRemaining - delta);
        }
    }

    /** Zmniejsza czas wizualizacji ataku, zeby animacja mogla wrocic do idle/walk. */
    public void tickAttackVisual(float delta) {
        if (attackVisualRemaining > 0f) {
            attackVisualRemaining = Math.max(0f, attackVisualRemaining - delta);
        }
    }

    /** Sprawdza, czy gracz nadal ma wlaczony czerwony flash obrazen. */
    public boolean isDamageFlashActive() {
        return damageFlashRemaining > 0f;
    }

    /** Sprawdza, czy animacja ataku nadal powinna byc widoczna. */
    public boolean isAttackVisualActive() {
        return attackVisualRemaining > 0f;
    }

    /** Zwraca kierunek, w ktorym gracz jest teraz ustawiony. */
    public Direction getFacingDirection() {
        return facingDirection;
    }

    private float resolveStateTimeDelta(
        float delta,
        boolean currentlyMoving,
        boolean effectiveAttacking,
        float movementAnimationSpeedScale
    ) {
        if (!currentlyMoving || effectiveAttacking) {
            return delta;
        }

        return delta * Math.max(1f, movementAnimationSpeedScale);
    }

    private Animation<TextureRegion> getCurrentAnimation() {
        if (attacking) {
            return getAttackAnimation(currentAttackTool, facingDirection);
        }

        return switch (facingDirection) {
            case DOWN -> moving ? walkDown : idleDown;
            case LEFT -> moving ? walkLeft : idleLeft;
            case RIGHT -> moving ? walkRight : idleRight;
            case UP -> moving ? walkUp : idleUp;
        };
    }

    private Direction resolveDirection(Vector2 direction) {
        if (direction == null || direction.isZero(MOVE_EPSILON)) {
            return facingDirection;
        }

        if (Math.abs(direction.x) > Math.abs(direction.y)) {
            return direction.x < 0f ? Direction.LEFT : Direction.RIGHT;
        }

        return direction.y < 0f ? Direction.DOWN : Direction.UP;
    }

    /** Szuka animacji ataku dla konkretnego narzedzia i kierunku. */
    private Animation<TextureRegion> getAttackAnimation(PlayerToolType toolType, Direction direction) {
        DirectionalAnimationSet directionalAnimationSet = attackAnimations.get(toolType);
        if (directionalAnimationSet == null) {
            directionalAnimationSet = attackAnimations.get(PlayerToolType.SWORD);
        }

        return switch (direction) {
            case DOWN -> directionalAnimationSet.down();
            case LEFT -> directionalAnimationSet.left();
            case RIGHT -> directionalAnimationSet.right();
            case UP -> directionalAnimationSet.up();
        };
    }

    /** Buduje mapę animacji ataku dla wszystkich narzedzi gracza. */
    private void buildAttackAnimations(TextureAtlas playerActionsAtlas) {
        attackAnimations.put(
            PlayerToolType.SWORD,
            createDirectionalAnimationSet(
                playerActionsAtlas,
                "player_attack/attack_down",
                "player_attack/attack_left",
                "player_attack/attack_right",
                "player_attack/attack_up",
                SWORD_ATTACK_FRAME_DURATION
            )
        );
        attackAnimations.put(
            PlayerToolType.AXE,
            createDirectionalAnimationSetWithFlippedLeft(
                playerActionsAtlas,
                "player_cutting/player_cutting_down",
                "player_cutting/player_cutting_right",
                "player_cutting/player_cutting_up",
                TOOL_ACTION_FRAME_DURATION
            )
        );
        attackAnimations.put(
            PlayerToolType.PICKAXE,
            createDirectionalAnimationSetWithFlippedLeft(
                playerActionsAtlas,
                "player_mining/player_mining_down",
                "player_mining/player_mining_right",
                "player_mining/player_mining_up",
                TOOL_ACTION_FRAME_DURATION
            )
        );
        attackAnimations.put(
            PlayerToolType.HOE,
            createDirectionalAnimationSetWithFlippedLeft(
                playerActionsAtlas,
                "player_hoe/player_hoe_down",
                "player_hoe/player_hoe_right",
                "player_hoe/player_hoe_up",
                TOOL_ACTION_FRAME_DURATION
            )
        );
        attackAnimations.put(
            PlayerToolType.WATERING_CAN,
            createDirectionalAnimationSetWithFlippedLeft(
                playerActionsAtlas,
                "player_watering/player_watering_down",
                "player_watering/player_watering_right",
                "player_watering/player_watering_up",
                TOOL_ACTION_FRAME_DURATION
            )
        );
    }

    /** Tworzy cztery animacje kierunkowe z atlasu. */
    private DirectionalAnimationSet createDirectionalAnimationSet(
        TextureAtlas atlas,
        String downRegion,
        String leftRegion,
        String rightRegion,
        String upRegion,
        float frameDuration
    ) {
        return new DirectionalAnimationSet(
            createSingleAnimation(atlas, downRegion, frameDuration),
            createSingleAnimation(atlas, leftRegion, frameDuration),
            createSingleAnimation(atlas, rightRegion, frameDuration),
            createSingleAnimation(atlas, upRegion, frameDuration)
        );
    }

    /** Tworzy animacje kierunkowe i odbija lewy kierunek, jesli atlas go nie ma. */
    private DirectionalAnimationSet createDirectionalAnimationSetWithFlippedLeft(
        TextureAtlas atlas,
        String downRegion,
        String rightRegion,
        String upRegion,
        float frameDuration
    ) {
        return new DirectionalAnimationSet(
            createSingleAnimation(atlas, downRegion, frameDuration),
            createSingleAnimation(createFlippedFrames(atlas, rightRegion), frameDuration),
            createSingleAnimation(atlas, rightRegion, frameDuration),
            createSingleAnimation(atlas, upRegion, frameDuration)
        );
    }

    /** Wczytuje prosta animacje z atlasu i ustawia tryb zapetlenia. */
    private Animation<TextureRegion> createLoopAnimation(TextureAtlas atlas, String regionName, float frameDuration) {
        var frames = atlas.findRegions(regionName);
        if (frames == null || frames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek animacji: " + regionName);
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames, Animation.PlayMode.LOOP);
        animation.setPlayMode(Animation.PlayMode.LOOP);
        return animation;
    }

    /** Wczytuje pojedyncza animacje, ktora nie ma sie zapetlac. */
    private Animation<TextureRegion> createSingleAnimation(TextureAtlas atlas, String regionName, float frameDuration) {
        var frames = atlas.findRegions(regionName);
        if (frames == null || frames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek animacji: " + regionName);
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames, Animation.PlayMode.NORMAL);
        animation.setPlayMode(Animation.PlayMode.NORMAL);
        return animation;
    }

    /** Wczytuje pojedyncza animacje z gotowej listy klatek. */
    private Animation<TextureRegion> createSingleAnimation(Array<TextureRegion> frames, float frameDuration) {
        if (frames == null || frames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek animacji narzędzia");
        }

        Animation<TextureRegion> animation = new Animation<>(frameDuration, frames, Animation.PlayMode.NORMAL);
        animation.setPlayMode(Animation.PlayMode.NORMAL);
        return animation;
    }

    /** Kopiuje klatki i odbija je w poziomie, zeby zrobic lustrzaną wersję animacji. */
    private Array<TextureRegion> createFlippedFrames(TextureAtlas atlas, String regionName) {
        var sourceFrames = atlas.findRegions(regionName);
        if (sourceFrames == null || sourceFrames.size == 0) {
            throw new IllegalArgumentException("Nie znaleziono klatek animacji: " + regionName);
        }

        Array<TextureRegion> flippedFrames = new Array<>(sourceFrames.size);
        for (TextureRegion sourceFrame : sourceFrames) {
            TextureRegion flippedFrame = new TextureRegion(sourceFrame);
            flippedFrame.flip(true, false);
            flippedFrames.add(flippedFrame);
        }
        return flippedFrames;
    }

    /** Prosty zestaw animacji kierunkowych dla jednego typu ruchu lub ataku. */
    private record DirectionalAnimationSet(
        Animation<TextureRegion> down,
        Animation<TextureRegion> left,
        Animation<TextureRegion> right,
        Animation<TextureRegion> up
    ) {
    }
}
