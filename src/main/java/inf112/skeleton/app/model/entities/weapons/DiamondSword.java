package inf112.skeleton.app.model.entities.weapons;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.World;
import inf112.skeleton.app.utils.B2DPhysics.PhysicsFactory;

import static inf112.skeleton.app.utils.Constants.*;

public class DiamondSword extends Weapon {
    private boolean isActive = false; // Initially inactive

    public DiamondSword(World world) {
        super(PhysicsFactory.createEntityBody(world,
                new Vector2(0, 0),
                new Vector2(DIAMOND_X_OFFSET, DIAMOND_Y_OFFSET),
                DIAMOND_WIDTH,
                DIAMOND_HEIGHT,
                false),
                DIAMOND_SWORD_IMG,
                DIAMOND_HEIGHT,
                DIAMOND_WIDTH,
                DIAMOND_DMG,
                DIAMOND_KNOCKBACK,
                DIAMOND_COOLDOWN,
                DIAMOND_STUN);
        this.setOffset(new Vector2(DIAMOND_X_OFFSET, DIAMOND_Y_OFFSET));
    }

    // Method to toggle visibility/activation
    public void toggleActive() {
        isActive = !isActive;
    }

    public boolean isActive() {
        return isActive;
    }

    @Override
    public void render(SpriteBatch batch) {
        if (isActive) {
            super.render(batch);
        }
    }
}
