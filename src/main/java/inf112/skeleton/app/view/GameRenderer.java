package inf112.skeleton.app.view;

import inf112.skeleton.app.controller.myInput.MyInputAdapter;
import inf112.skeleton.app.model.GameLogic;
import inf112.skeleton.app.model.GameState;
import inf112.skeleton.app.model.entities.Entity;
import inf112.skeleton.app.utils.TiledObjectUtil;
//import inf112.skeleton.app.utils.TiledObjectUtil;
import inf112.skeleton.app.view.HUD.HUD;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import java.util.ArrayList;

import static inf112.skeleton.app.model.Constants.*;

/**
 * The GameRenderer class is responsible for rendering the game.
 * It manages the rendering of entities, HUD, and game UI elements.
 */
public class GameRenderer extends Game {
    private SpriteBatch batch;
    private OrthographicCamera cam;
    private GameLogic gameLogic;
    private ArrayList<TextureRegion> entitySprites = new ArrayList<>();

    private Box2DDebugRenderer debugRenderer;

    private Texture spriteSheet;
    private BitmapFont font;
    private TiledMap map;
    private OrthogonalTiledMapRenderer tmr;
    private HUD hud;

    /**
     * Constructs a GameRenderer with the specified GameLogic.
     *
     * @param gameLogic the GameLogic instance to render
     */
    public GameRenderer(GameLogic gameLogic) {
        this.gameLogic = gameLogic;
    }

    @Override
    public void create() {

        debugRenderer = new Box2DDebugRenderer();
        batch = new SpriteBatch();
        spriteSheet = getSpriteSheet(DUNGEON_SHEET_IMG);
        for (Entity entity : gameLogic.getEntities()) {
            entitySprites.add(getSpriteFromSheet(spriteSheet, entity.getSpriteSheetX(), entity.getSpriteSheetY(),
                    entity.getSpriteWidth(), entity.getSpriteHeight()));
        }

        cam = new OrthographicCamera();
        cam.setToOrtho(false, WINDOW_WIDTH, WINDOW_HEIGHT);
    
        font = new BitmapFont();
        Gdx.input.setInputProcessor(new MyInputAdapter(gameLogic.getPlayer()));

        map = new TmxMapLoader().load(MAP_IMG);
        tmr = new OrthogonalTiledMapRenderer(map);

        TiledObjectUtil.parseTiledObjectLayer(gameLogic.world, 
                                              map.getLayers().get("collision-layer").getObjects());

        Texture heartTexture = new Texture(HEART_IMG);
        hud = new HUD(heartTexture, gameLogic.getPlayer().getHealth());
    }

    @Override
    public void render() {
        clearScreen();
        updateCamera();
        gameLogic.update();

        // map
        tmr.setView(cam);
        tmr.render();

        debugRenderer.render(gameLogic.world, cam.combined);
        
        // batch   
        batch.begin();
        batch.setProjectionMatrix(cam.combined);
        drawEntities();
        drawHUD();
        drawGameUI();
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        spriteSheet.dispose();
        for (TextureRegion textureRegion : entitySprites) {
            textureRegion.getTexture().dispose();
        }
        font.dispose();
        map.dispose();
        tmr.dispose();
    }

    private void clearScreen() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }

    private void updateCamera() {
        cam.position.set(gameLogic.getPlayer().getX() + PLAYER_WIDTH / 2,
                gameLogic.getPlayer().getY() + PLAYER_HEIGHT / 2, 0);
        cam.update();
        float zoomLevel = 0.7f;
        cam.zoom = zoomLevel;
    }

    private void drawEntities() {
        for (Entity entity : gameLogic.getEntities()) {
            TextureRegion entitySprite = entitySprites.get(gameLogic.getEntities().indexOf(entity));
            batch.draw(entitySprite, entity.getX(), entity.getY(), PLAYER_WIDTH, PLAYER_HEIGHT);
        }
    }

    private void drawHUD() {
        hud.updateHearts(gameLogic.getPlayer().getHealth());
        hud.draw(batch);
    }

    private void drawGameUI() {
        if (gameLogic.isShowHitWarning()) {
            drawHitWarning();
        }
        if (gameLogic.getGameState() == GameState.GAME_OVER) {
            drawGameOver();
        }
        if (gameLogic.getGameState() == GameState.MENU){
            drawMenu();
        }
    }

    private void drawGameOver() {
        // Draw a rectangle with a picture
        Rectangle rectangle = new Rectangle(0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        Texture gameOverTexture = new Texture(GAME_OVER_IMG);

        // Debug information
        System.out.println("Rectangle dimensions: " + rectangle.width + "x" + rectangle.height);
        System.out.println("Texture dimensions: " + gameOverTexture.getWidth() + "x" + gameOverTexture.getHeight());

        batch.draw(gameOverTexture, rectangle.x, rectangle.y, rectangle.width, rectangle.height);
    }

    private void drawHitWarning() {
        batch.setColor(1, 0, 0, 0.9f);
        batch.draw(new Texture(HIT_WARNING_IMG), 0, 0, WINDOW_WIDTH, WINDOW_HEIGHT);
        batch.setColor(1, 1, 1, 1);
    }


    private TextureRegion getSpriteFromSheet(Texture spriteSheet, int x, int y, int width, int height) {
        return new TextureRegion(spriteSheet, x, y, width, height);
    }

    private Texture getSpriteSheet(String spriteSheet) {
        return new Texture(Gdx.files.internal(spriteSheet));
    }

    private void drawMenu(){
        
    }

}
