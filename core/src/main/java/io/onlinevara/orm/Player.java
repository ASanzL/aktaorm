package io.onlinevara.orm;

import com.badlogic.gdx.ApplicationAdapter;
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
    Array<Color> snakeColors = new Array<>();
    int snakeColorId = 0;

    boolean ready = false;
    Main main;

    public Player(Vector2 position, float angle, SpriteBatch batch, Main main) {
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
        snakeColors.add(Color.WHITE);

        snakeColorId = MathUtils.random(snakeColors.size - 1);

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

    private Vector2 getHeadPosition() {
        return snakePath.get(snakePath.size - 1);
    }

    @Override
    public void act(float delta) {
        if (!main.gameStarted()) {
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
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        shapeDrawerBody = new ShapeDrawer(batch, new TextureRegion(new Texture(Gdx.files.internal("snake-part.png"))));
        shapeDrawerHead = new ShapeDrawer(batch, new TextureRegion(new Texture(Gdx.files.internal("snake-part.png"))));

        shapeDrawerBody.setColor(snakeColors.get(snakeColorId));
        shapeDrawerHead.setColor(Color.WHITE);
        shapeDrawerBody.path(snakePath, 16, JoinType.SMOOTH, true);
        shapeDrawerHead.filledCircle(getHeadPosition().x, getHeadPosition().y, 8);
    }
}
