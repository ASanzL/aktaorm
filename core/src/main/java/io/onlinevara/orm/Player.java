package io.onlinevara.orm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
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
    private final static float TURN_SPEED = 120f;
    private final static float MOVEMENT_SPEED = 140f;

    // When this value goes below zero, create gap in snake
    private float distanceUntilGap;
    // When this value goes below zero, stop gap
    private float getDistanceUntilStopGap;
    private boolean makingGap = false;

    private boolean turnLeft = false;
    private boolean turnRight = false;

    Array<Array<Vector2>> snakePaths = new Array<>();
    Array<Vector2> collisionPoints = new Array<>();
    float distanceSinceLastCollisionPoints = 0;

    Array<Color> snakeColors = new Array<>();
    int snakeColorId = 0;

    // Width in pixels
    int snakeSize;

    boolean ready = false;
    boolean isDead = false;
    Main main;

    public Player(Vector2 position, float angle, Main main, int snakeColor, int snakeSize) {
        setPosition(position.x, position.y);
        this.angle = angle;
        this.snakeSize = snakeSize;

        resetPlayer();

        snakeColors.add(Color.RED);
        snakeColors.add(Color.YELLOW);
        snakeColors.add(Color.GREEN);
        snakeColors.add(Color.ORANGE);
        snakeColors.add(Color.CYAN);
        snakeColors.add(Color.NAVY);

        snakeColorId = snakeColor;

        this.main = main;
    }

    public static Vector2 getRandomPosition() {
        return new Vector2(MathUtils.random(200, Main.getGameWidth() - 200),
            MathUtils.random(200, Main.getGameHeight() - 200));
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

    private void addSnakeBody() {
        snakePaths.add(new Array<>());
        addSnakePart();
    }

    private void addSnakePart()
    {
        getLatestSnakeBody().add(new Vector2(getX(), getY()));
    }

    public Vector2 getHeadPosition() {
        return getLatestSnakeBody().get(getLatestSnakeBody().size - 1);
    }

    private Array<Vector2> getLatestSnakeBody() {
        return snakePaths.get(snakePaths.size - 1);
    }

    private void startGap() {
        makingGap = true;
        if (snakePaths.size > 0) {
            addSnakePart();
        }
        addSnakeBody();
    }

    private void stopGap() {
        makingGap = false;
        distanceUntilGap = MathUtils.random(300, 1200);
        getDistanceUntilStopGap = MathUtils.random(30, 80);
        addSnakePart();
        distanceSinceLastCollisionPoints = 0;
    }

    public void resetPlayer() {
        isDead = false;
        ready = false;

        snakePaths = new Array<>();

        startGap();
        stopGap();
        addSnakePart();
        addSnakePart();

        collisionPoints.clear();
    }

    private Color getHeadColor() {
        return ready && !isDead ? Color.WHITE : snakeColors.get(snakeColorId);
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        if (!main.gameStarted() || isDead) {
            return;
        }
        // Update rotation based and add snake parts
        if (turnRight) {
            angle += TURN_SPEED * delta;
            if (!makingGap) {
                addSnakePart();
            }

        } else if (turnLeft) {
            angle -= TURN_SPEED * delta;
            if (!makingGap) {
                addSnakePart();
            }
        }

        float toMoveX = MOVEMENT_SPEED * MathUtils.cosDeg(angle) * delta;
        float toMoveY = MOVEMENT_SPEED * MathUtils.sinDeg(angle) * delta;
        float distanceMovedThisFrame = Vector2.dst(getX(), getY(),
            getX() + toMoveX, getY() + toMoveY);

        // Move actor
        moveBy(toMoveX, toMoveY);

        // Set last point to head
        getHeadPosition().x = getX();
        getHeadPosition().y = getY();

        distanceUntilGap -= distanceMovedThisFrame;
        if (distanceUntilGap <= 0) {
            if (!makingGap) {
                startGap();
            }
            getDistanceUntilStopGap -= distanceMovedThisFrame;
            if (getDistanceUntilStopGap <= 0) {
                stopGap();
            }
        }

        // Place new collision points evenly
        distanceSinceLastCollisionPoints += distanceMovedThisFrame;
        if (distanceSinceLastCollisionPoints > snakeSize && !makingGap) {
            collisionPoints.add(new Vector2(getX(), getY()));
            distanceSinceLastCollisionPoints = 0;
        }
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        shapeDrawerBody = new ShapeDrawer(batch, new TextureRegion(new Texture(Gdx.files.internal("snake-part.png"))));
        shapeDrawerHead = new ShapeDrawer(batch, new TextureRegion(new Texture(Gdx.files.internal("snake-part.png"))));

        shapeDrawerBody.setColor(snakeColors.get(snakeColorId));
        shapeDrawerHead.setColor(getHeadColor());
        for (Array<Vector2> path :
            snakePaths) {
            shapeDrawerBody.path(path, snakeSize, JoinType.SMOOTH, true);
        }
        shapeDrawerHead.filledCircle(getHeadPosition().x, getHeadPosition().y, (float) snakeSize / 2);

        // Uncomment for collision debug
//        for (Vector2 p :
//            collisionPoints) {
//            shapeDrawerBody.circle(p.x, p.y, (float) snakeSize / 2);
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
