package io.onlinevara.orm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class Player extends Actor {
    ShapeDrawer shapeDrawerBody;
    ShapeDrawer shapeDrawerHead;
    float angle;
    private final float turnSpeed = 160f;
    private final float movementSpeed = 130f;

    TextureRegion playerTexture;

    private boolean turnLeft = false;
    private boolean turnRight = false;

    Array<Vector2> snakePath = new Array<>();
    Array<Vector2> collisionPoints = new Array<>();
    float distanceSinceLastCollisionPoints = 0;

    Array<Color> snakeColors = new Array<>();
    int snakeColorId = 0;

    // Width in pixels
    int snakeSize = 16;

    boolean ready = false;
    boolean isDead = false;
    Main main;

    public Player(Vector2 position, float angle, SpriteBatch batch, Main main, int snakeColor) {
        setPosition(position.x, position.y);
        playerTexture = new TextureRegion(new Texture(Gdx.files.internal("snake-head.png")));

        this.angle = angle;

        addSnakePart();
        addSnakePart();

        snakeColors.add(Color.RED);
        snakeColors.add(Color.YELLOW);
        snakeColors.add(Color.GREEN);
        snakeColors.add(Color.ORANGE);
        snakeColors.add(Color.CYAN);
        snakeColors.add(Color.NAVY);

        snakeColorId = snakeColor;

        this.main = main;
    }

    public void turnLeft() {
        turnLeft = true;
        turnRight = false;
    }
    public void turnRight() {
        turnLeft = false;
        turnRight = true;
    }

    public void stopTurning() {
        turnLeft = false;
        turnRight = false;
    }

    private void addSnakePart()
    {
        snakePath.add(new Vector2(getX(), getY()));
    }

    public Vector2 getHeadPosition() {
        return snakePath.get(snakePath.size - 1);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!main.gameStarted() || isDead) {
            return;
        }
        // Update rotation based and add snake parts
        if (turnRight) {
            angle += turnSpeed * delta;
            addSnakePart();

        } else if (turnLeft) {
            angle -= turnSpeed * delta;
            addSnakePart();
        }

        // Move actor
        moveBy(
            movementSpeed * MathUtils.cosDeg(angle) * delta,
            movementSpeed * MathUtils.sinDeg(angle) * delta
        );

        // Set last point to head
        getHeadPosition().x = getX();
        getHeadPosition().y = getY();


        // Place new collision points evenly
        distanceSinceLastCollisionPoints += Math.abs(movementSpeed * MathUtils.cosDeg(angle) * delta);
        distanceSinceLastCollisionPoints += Math.abs(movementSpeed * MathUtils.sinDeg(angle) * delta);
        if (distanceSinceLastCollisionPoints > snakeSize) {
            collisionPoints.add(new Vector2(getX(), getY()));
            distanceSinceLastCollisionPoints -= snakeSize;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        shapeDrawerBody = new ShapeDrawer(batch, new TextureRegion(new Texture(Gdx.files.internal("snake-part.png"))));
        shapeDrawerHead = new ShapeDrawer(batch, new TextureRegion(new Texture(Gdx.files.internal("snake-part.png"))));

        shapeDrawerBody.setColor(snakeColors.get(snakeColorId));
        shapeDrawerHead.setColor(ready ? Color.WHITE : snakeColors.get(snakeColorId));
        shapeDrawerBody.path(snakePath, snakeSize, JoinType.SMOOTH, true);
        shapeDrawerHead.filledCircle(getHeadPosition().x, getHeadPosition().y, snakeSize / 2);

//        for (Vector2 p :
//            collisionPoints) {
//            shapeDrawerBody.circle(p.x, p.y, snakeSize / 2);
//        }
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Player)) {
            return false;
        }

        Player p = (Player) obj;
        return this.snakeColorId == p.snakeColorId;
    }
}
