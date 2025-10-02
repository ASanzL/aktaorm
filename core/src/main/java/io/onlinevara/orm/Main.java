package io.onlinevara.orm;

import com.badlogic.gdx.ApplicationAdapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Array;

import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture playerTexture;

    private Array<PlayerDot> playerDots;

    private float playerX = 300;
    private float playerY = 300;

    private float playerAngle = 0;

    @Override
    public void create() {
        batch = new SpriteBatch();
        playerDots = new Array<>();
        playerTexture = new Texture(Gdx.files.internal("dot.jpg"));
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        playerDots.add(new PlayerDot(playerX, playerY));

        if (Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
            playerAngle -= 5;
        }

        if (Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
            playerAngle += 5;
        }

        playerX += Math.cos(playerAngle) * 3;
        playerY += Math.sin(playerAngle) * 3;

        batch.begin();
        for (PlayerDot dot: playerDots
             ) {
            dot.render(batch, playerTexture);
        }
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
