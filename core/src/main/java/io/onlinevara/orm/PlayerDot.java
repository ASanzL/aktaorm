package io.onlinevara.orm;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class PlayerDot {
    private float x;
    private float y;

    public PlayerDot(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void create() {
    }

    public void render(SpriteBatch batch, Texture texture) {
        batch.draw(texture, x, y, 30, 30);
    }

}

