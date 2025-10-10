package io.onlinevara.orm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.utils.Array;

import space.earlygrey.shapedrawer.JoinType;
import space.earlygrey.shapedrawer.ShapeDrawer;

public class Player extends Actor {
    ShapeDrawer shapeDrawer;
    Vector2 position;
    float angle;
    private final float turnSpeed = 160f;
    private final float movementSpeed = 130f;

    TextureRegion playerTexture;

    private boolean turnLeft = false;
    private boolean turnRight = false;

    Array<Vector2> snakePath = new Array<>();
    Array<Color> snakeColors = new Array<>();
    int snakeColorId = 0;

    public Player(Vector2 position, float angle, SpriteBatch batch) {
        this.position = position;
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
        snakePath.add(new Vector2(position.x, position.y));
    }

    @Override
    public void act(float delta) {
        // Update rotation based and add snake parts
        if (turnRight) {
            angle += turnSpeed * delta;
            addSnakePart();

        } else if (turnLeft) {
            angle -= turnSpeed * delta;
            addSnakePart();
        }

        // Move actor
        position.x += movementSpeed * MathUtils.cosDeg(angle) * delta;
        position.y += movementSpeed * MathUtils.sinDeg(angle) * delta;

        // Set last point to head
        snakePath.get(snakePath.size - 1).x = position.x;
        snakePath.get(snakePath.size - 1).y = position.y;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        shapeDrawer = new ShapeDrawer(batch, new TextureRegion(new Texture(Gdx.files.internal("snake-part.png"))));

//        batch.draw(playerTexture, position.x, position.y, 32, 32, 64, 64, 1f, 1f, angle);

        shapeDrawer.setColor(snakeColors.get(snakeColorId));
        shapeDrawer.path(snakePath, 16, JoinType.SMOOTH, true);
    }
}
