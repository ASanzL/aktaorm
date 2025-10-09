package io.onlinevara.orm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.DelaunayTriangulator;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;

public class Player extends Actor {
    Vector2 position;
    float angle;
    private final float turnSpeed = 160f;
    private final float movementSpeed = 130f;

    TextureRegion playerTexture;

    private boolean turnLeft = false;
    private boolean turnRight = false;


    public Player(Vector2 position, float angle) {
        this.position = position;
        playerTexture = new TextureRegion(new Texture(Gdx.files.internal("snake-head.png")));

        this.angle = angle;
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

    @Override
    public void act(float delta) {
        if (turnRight) {
            angle += turnSpeed * delta;
        } else if (turnLeft) {
            angle -= turnSpeed * delta;
        }
        position.x += movementSpeed * MathUtils.cosDeg(angle) * delta;
        position.y += movementSpeed * MathUtils.sinDeg(angle) * delta;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.draw(playerTexture, position.x, position.y, 32, 32, 64, 64, 1f, 1f, angle);
    }
}
