package inf112.skeleton.app.model;

import inf112.skeleton.app.model.entities.*;
import inf112.skeleton.app.utils.B2DPhysics.B2dContactListener;
import inf112.skeleton.app.utils.B2DPhysics.CollisionCallBack;
import inf112.skeleton.app.utils.B2DPhysics.PhysicsFactory;

import static inf112.skeleton.app.utils.Constants.*;

import java.util.List;
import java.util.ArrayList;
import inf112.skeleton.app.controller.myInput.SoundController;
import java.util.Iterator;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.PolygonMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Polygon;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.World;
import inf112.skeleton.app.model.entities.enemies.*;
import inf112.skeleton.app.model.entities.weapons.*;

/**
 * GameLogic handles the game logic including player and enemy interactions.
 */
public class GameLogic implements CollisionCallBack {
    // State
    private GameState gameState;
    private int wave;

    // Entities
    private Player player;
    private Weapon weapon;
    private List<Enemy> enemies = new ArrayList<>();
    private List<Coin> coins = new ArrayList<>();
    private List<Entity> entities = new ArrayList<>();
    private List<Polygon> spikePolygons = new ArrayList<>();

    // Time
    private long lastHitTime;
    private final long hitCooldown = HIT_COOLDOWN;
    private final int hitWarningDuration = HIT_WARNING_DURATION;
    private boolean showHitWarning = false;
    private long hitWarningStartTime = 0;
    private int coinValue = 0;
    private TiledMap map;
    public World world;
    // sounds
    private SoundController soundController = new SoundController();

    private float waveTimer = 0;
    private float waveDelay = 5; // 5 seconds delay for respawning enemies

    /**
     * Constructs a new GameLogic instance with the given game state.
     *
     * @param gameState the initial game state
     */

    public GameLogic(GameState gameState) {
        this.gameState = gameState;
        setWorld(new World(new Vector2(0, 0), true));
        world.setContactListener(new B2dContactListener(this));
        if (this.gameState == GameState.GAME_TITLE) {
            soundController.initializeBackgroundMusic();
        }
    }

    public void resetGame() {
        System.out.println("Resetting game");
        // Reset player
        this.player.setHealth(PLAYER_HEALTH);
        this.player.setPos(PLAYER_SPAWN_X, PLAYER_SPAWN_Y);
        this.player.getBody().setLinearVelocity(new Vector2().scl(0));
        this.player.getBody().applyForceToCenter(new Vector2().scl(0), true);

        // Clear entities
        destroyInactiveEntities();
        entities.clear();
        coins.clear();
        enemies.clear();

        // Initialize new entities
        coins = new ArrayList<>();
        initializeCoins();
        enemies = new ArrayList<>();
        initializeEnemies();

        entities.add(player);
        entities.add(weapon);
        entities.addAll(enemies);
        entities.addAll(coins);
    }

    public int getCoinValue() {
        return coinValue;
    }

    public void setCoinValue(int coinValue) {
        this.coinValue = coinValue;
    }

    public World getWorld() {
        return world;
    }

    public void setWorld(World world) {
        this.world = world;
    }

    public void setPlayer(Player player) {
        this.player = player;
    }

    /**
     * Gets the list of entities in the game.
     *
     * @return the list of entities
     */
    public List<Entity> getAllEntities() {
        return entities;
    }

    public List<Enemy> getAllEnemies() {
        return enemies;
    }

    /**
     * Gets the player entity.
     *
     * @return the player entity
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Gets the current game state.
     *
     * @return the current game state
     */
    public GameState getGameState() {
        return gameState;
    }

    /**
     * Checks if the hit warning should be displayed.
     *
     * @return true if the hit warning should be displayed, false otherwise
     */
    public boolean isShowHitWarning() {
        return showHitWarning;
    }

    // Ensure there is a way to access the specific weapon
    public Weapon getWeapon() {
        return weapon;
    }

    public void setWeapon(Weapon weapon) {
        removeEntity(this.weapon);
        this.weapon = weapon;
        entities.add(weapon);
    }

    private void initializeEntities() {
        initializePlayer();
        initializeEnemies();
        initializeWeapon(new MetalSword(world));
        initializeCoins();
    }

    private void setUserDataToParent(Entity entity) {
        entity.getBody().getFixtureList().get(0).setUserData(entity);
    }

    private void initializePlayer() {
        Body playerBody = PhysicsFactory.createDynamicEntityBody(world,
                new Vector2(PLAYER_SPAWN_X, PLAYER_SPAWN_Y),
                PLAYER_WIDTH,
                PLAYER_HEIGHT,
                true);
        this.player = new Player(playerBody, PLAYER_SPRITE, "player");
        setUserDataToParent(player);
        entities.add(this.player);
    }

    private void initializeWeapon(Weapon weapon) {
        this.weapon = weapon;
        // this.weapon = new DiamondSword(world);
        entities.add(this.weapon);
    }

    private void initializeEnemies() {
        System.out.println("Initializing enemies");
        int num_enemies = NUM_STARTER_ENEMIES + 2 * this.wave;
        int num_heavy = (int) (num_enemies * 0.2f);
        int num_medium = (int) (num_enemies * 0.4f);
        int num_light = num_enemies - num_heavy - num_medium;
        int[] enemyTypes = {num_light, num_medium, num_heavy};
        for (int i = 0; i < enemyTypes.length; i++) {
            for (int j = 0; j < enemyTypes[i]; j++)   {
                Enemy enemy;
                if (enemyTypes[i] == num_light) {
                    enemy = new Light(world);
                } else if (enemyTypes[i] == num_medium) {
                    enemy = new Medium(world);
                } else if (enemyTypes[i] == num_heavy) {
                    enemy = new Heavy(world);
                } else {
                    throw new IllegalStateException("Unexpected value: " + enemyTypes[i]);
                }
                Vector2 randomPosition = getRandomEntityPosition();
                enemy.setPos(randomPosition.x, randomPosition.y);
                enemies.add(enemy);
            }
        }
        entities.addAll(enemies);
    }

    private void initializeCoins() {
        for (int i = 0; i < NUM_COINS; i++) {
            Body coinBody = PhysicsFactory.createStaticEntityBody(world,
                    getRandomEntityPosition(),
                    COIN_WIDTH,
                    COIN_HEIGHT);
            coinBody.setUserData("coin");
            Coin coin = switch ((int) (Math.random() * 5)) {
                case 0 -> new Coin(coinBody, COIN_SPRITE, COIN_VALUE_ONE, "coin");
                case 1 -> new Coin(coinBody, COIN_SPRITE, COIN_VALUE_TWO, "coin");
                case 2 -> new Coin(coinBody, COIN_SPRITE, COIN_VALUE_THREE, "coin");
                case 3 -> new Coin(coinBody, COIN_SPRITE, COIN_VALUE_FOUR, "coin");
                case 4 -> new Coin(coinBody, COIN_SPRITE, COIN_VALUE_FIVE, "coin");
                default -> throw new IllegalStateException("Unexpected value");
            };
            coins.add(coin);
        }
        entities.addAll(coins);
    }

    private Vector2 getRandomEntityPosition() {
        Vector2 randomPosition;
        do {
            randomPosition = new Vector2(
                    MathUtils.random(0, WINDOW_WIDTH),
                    MathUtils.random(0, WINDOW_HEIGHT));
        } while (!isLegalSpawnPosition(randomPosition));

        return randomPosition;
    }

    private void checkForSpikeCollisions() {
        Vector2 playerPosition = this.player.getPosition();
        for (Polygon spikePolygon : spikePolygons) {
            if (isPointInPolygon(spikePolygon.getTransformedVertices(), playerPosition)) {
                applyHitToPlayer();
                break;
            }
        }
    }

    public void removeEntity(Entity entity) {
        System.out.println("Entities: " + entities);
        if (entity instanceof Enemy) {
            enemies.remove(entity);
        }
        world.destroyBody(entity.getBody());
        entities.remove(entity);
        entity.setIsDestroyed(true);
    }

    private void checkForCoinCollisions() {
        for (Coin coin : coins) {
            if (player.collidesWith(coin)) {
                if (!coin.isCollected()) {
                    coin.setCollected();
                    coinValue += coin.getValue();
                }
            }
        }
    }

    private boolean isLegalSpawnPosition(Vector2 position) {
        MapLayer layer = map.getLayers().get("out-of-bounds-layer");
        if (layer == null) {
            return true;
            
        }
        for (MapObject object : layer.getObjects()) {
            if (object instanceof PolygonMapObject) {
                PolygonMapObject polygonObject = (PolygonMapObject) object;
                Polygon polygon = polygonObject.getPolygon();

                if (isPointInPolygon(polygon.getTransformedVertices(), position)) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isPointInPolygon(float[] polygonVertices, Vector2 point) {
        int intersects = 0;
        float x = point.x;
        float y = point.y;

        for (int i = 0; i < polygonVertices.length; i += 2) {
            float x1 = polygonVertices[i];
            float y1 = polygonVertices[i + 1];
            float x2 = polygonVertices[(i + 2) % polygonVertices.length];
            float y2 = polygonVertices[(i + 3) % polygonVertices.length];

            if (((y1 <= y && y < y2) || (y2 <= y && y < y1)) &&
                    (x < (x2 - x1) * (y - y1) / (y2 - y1) + x1)) {
                intersects++;
            }
        }

        return (intersects % 2) == 1;
    }

    /**
     * Updates the game logic.
     */
    public void update() {
        updateWorld();
        destroyInactiveEntities();
        updatePlayerPosition();
        checkPlayerHit();
        checkForSpikeCollisions();
        checkForCoinCollisions();
        checkGameOver();
        updateHitWarning();
        updateEnemyStunTimer();
        updateEnemyPositions();
        updateWeapon();
        updateWave();
    }

    public List<Entity> getActiveEntities() {
        List<Entity> activeEntities = new ArrayList<>();
        for (Entity entity : entities) {
            if (entity.isActive()) {
                activeEntities.add(entity);
            }
        }
        return activeEntities;
    }

    private void destroyInactiveEnemies() {
        List<Enemy> activeEnemies = enemies;
        Iterator<Enemy> iterator = getAllEnemies().iterator();
        while (iterator.hasNext()) {
            Enemy enemy = iterator.next();
            if (!activeEnemies.contains(enemy)) {
                if (!enemy.getIsDestroyed()) {
                    enemy.setIsDestroyed(true);
                    enemy.getBody().setTransform(0, 0, 0);
                    world.destroyBody(enemy.getBody());
                    playEnemyDeathSound(enemy);
                    iterator.remove();
                }
            }
        }
    }

    private void destroyInactiveEntities() {
        List<Entity> activeEntities = getActiveEntities();
        List<Enemy> enemiesToRemove = new ArrayList<>();
        for (Entity entity : entities) {
            if (!activeEntities.contains(entity)) {
                if (!entity.getIsDestroyed()) {
                    if (entity instanceof Enemy) {
                        enemiesToRemove.add((Enemy) entity);
                    }
                }
            }
        }
        for (Enemy enemy : enemiesToRemove) {
            removeEntity(enemy);
        }
    }

    private void playEnemyDeathSound(Enemy enemy) {
        if (enemy instanceof Light) {
            soundController.playTechoShotSound();
            ;
            ;
        } else if (enemy instanceof Medium) {
            soundController.playDamageSound();
            ;
        } else if (enemy instanceof Heavy) {
            soundController.playPestilenceSound();
        }
    }

    private void updateWeapon() {
        updateWeaponCooldownTimer();
        updateWeaponPos();
        updateWeaponAngle();
    }

    private void updateWeaponPos() {
        weapon.setPos(player.getX(), player.getY());
    }

    private void updateWeaponAngle() {
        weapon.setAngle(getAngleToMouse(400 - (PLAYER_WIDTH / 2), 400 + (PLAYER_HEIGHT / 2)));
    }

    private void updateWeaponCooldownTimer() {
        weapon.setCooldownTimer((weapon.getCooldownTimer() - (float) 1 / 60));
        if (weapon.getCooldownTimer() > 0) {
            weapon.setOpacity(0.5f);
        } else {
            weapon.setOpacity(1f);
        }
    }

    private void updateEnemyStunTimer() {
        for (Enemy enemy : enemies) {
            enemy.setStunTimer((enemy.getStunTimer() - (float) 1 / 60));
            if (enemy.getStunTimer() > 0) {
                enemy.setOpacity(0.5f);
            } else {
                enemy.setOpacity(1f);
            }
        }
    }

    private float getAngleToMouse(float x1, float y1) {
        float angle = (float) Math.toDegrees(Math.atan2(Gdx.input.getY() - y1, Gdx.input.getX() - x1));
        angle += 90;
        if (angle < 0) {
            angle += 360;
        }
        angle %= 360;
        return angle;
    }

    private void updateWorld() {
        float deltaTime = Gdx.graphics.getDeltaTime();
        world.step(deltaTime, 6, 2); // The numbers 6 and 2 are velocity and position iterations, you can adjust
    }

    private void updatePlayerPosition() {
        player.move();
    }

    private void updateWave() {
        waveTimer += Gdx.graphics.getDeltaTime();
        if (enemies.size() == 0 && waveTimer >= waveDelay) {
            player.setHealth(PLAYER_HEALTH);
            initializeEnemies();
            if (coins.size() == 0) {
                initializeCoins();
            }
            wave += 1;
            waveTimer = 0;
        }
    }

    private void checkPlayerHit() {
        for (Enemy enemy : enemies) {
            if (player.collidesWith(enemy)) {
                applyHitToPlayer();

            }
        }
    }

    private void applyHitToPlayer() {
        if (System.currentTimeMillis() - lastHitTime > hitCooldown) {
            player.takeDamage(HIT_DAMAGE);
            lastHitTime = System.currentTimeMillis();
            showHitWarning = true;
            hitWarningStartTime = System.currentTimeMillis();
        }
        soundController.playDamageSound();
    }

    private void checkGameOver() {
        if (player.getHealth() <= 0) {
            gameState = GameState.GAME_OVER;
            soundController.playGameOverSound();
        }

    }

    private void updateHitWarning() {
        if (showHitWarning && System.currentTimeMillis() - hitWarningStartTime > hitWarningDuration) {
            showHitWarning = false;
        }
    }

    private void updateEnemyPositions() {
        for (Enemy enemy : enemies) {
            if (enemy.getStunTimer() <= 0) {
                enemy.moveTowards(player.getX(), player.getY());
            } // If enemy has no stun remaining
        }
    }

    public void setGameState(GameState gameState) {
        this.gameState = gameState;
    }

    public void setMap(TiledMap map) {
        this.map = map;
        initializeEntities();
    }

    @Override
    public void onPlayerSpikeCollision(Player player, Spike spike) {
        applyHitToPlayer();
    }

    public TiledMap getMap() {
        return map;
    }
}
