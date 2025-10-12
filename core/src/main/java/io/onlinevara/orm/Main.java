package io.onlinevara.orm;

import com.badlogic.gdx.ApplicationAdapter;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import java.io.IOException;
import java.net.InetAddress;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter implements InputProcessor {
    private SpriteBatch batch;
    private Stage stage;

    Skin uiSkin;
    TextButton readyButton;

    private boolean isServer = false;

    private int playerId = -1;

    private Server server;
    private Client client;

    private Color bgColor = new Color(0.2f, 0.2f, 0.2f, 1f);

    @Override
    public void create() {
        batch = new SpriteBatch();
        stage = new Stage(new FitViewport(1920, 1080));
        InputMultiplexer multiplexer = new InputMultiplexer();

        // Setup input
        multiplexer.addProcessor(stage);
        multiplexer.addProcessor(this);
        Gdx.input.setInputProcessor(multiplexer);

        // Setup UI
        uiSkin = new Skin(Gdx.files.internal("ui/uiskin.json"));
        readyButton = new TextButton("Ready", uiSkin);
        readyButton.setSize(500, 200);
        readyButton.setPosition(50,100);
        readyButton.getLabel().setFontScale(5);
        stage.addActor(readyButton);
        readyButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                setPlayerReadyNet();
                return true;
            }
        });

        // Initiate server or client
        client = new Client();

        Kryo kryo = client.getKryo();

        registerKryoClasses(kryo);

        InetAddress address = client.discoverHost(54777, 5000);
        if (address == null) {
            isServer = true;
            server = new Server();
            Kryo kryoS = server.getKryo();
            registerKryoClasses(kryoS);

            // Handle server traffic
            server.addListener(new Listener() {
                @Override
                public void received(Connection connection, Object object) {
                    if (object.toString().equals("JOINED")) {
                        // Send all player actors to joining player
                        for (Actor a:stage.getActors()
                             ) {
                            if (a instanceof Player) {
                                Player player = (Player)a;

                                NetNewPlayer newPlayerMessage = new NetNewPlayer();
                                newPlayerMessage.x = player.getX();
                                newPlayerMessage.y = player.getY();
                                newPlayerMessage.angle = player.angle;
                                server.sendToTCP(connection.getID(), newPlayerMessage);
                            }
                        }

                        // Add joining player
                        NetNewPlayer message = new NetNewPlayer();
                        message.x = MathUtils.random(500, 500);
                        message.y = MathUtils.random(900, 900);
                        newPlayer(new Vector2(message.x, message.y), 0);
                        server.sendToAllTCP(message);

                        // Set player id of new player
                        NetSetPlayerId playerIdMsg = new NetSetPlayerId();
                        playerIdMsg.playerId = stage.getActors().size - 1;
                        server.sendToTCP(connection.getID(), playerIdMsg);
                    }
                    // Handle client turn message
                    if (object instanceof NetTurn) {
                        NetTurn response = (NetTurn) object;
                        getPlayer(response.playerId).angle = response.realAngle;
                        if (response.direction.equals("left")) {
                            getPlayer(response.playerId).turnLeft();
                        } else if (response.direction.equals("right")) {
                            getPlayer(response.playerId).turnRight();
                        } else {
                            getPlayer(response.playerId).stopTurning();
                        }
                        response.realX = getPlayer(response.playerId).getX();
                        response.realY = getPlayer(response.playerId).getY();

                        server.sendToAllTCP(response);
                    }

                    if (object instanceof NetSetReady) {
                        NetSetReady response = (NetSetReady)object;

                        server.sendToAllTCP(response);
                        setPlayerReady(response.playerId);
                    }
                }
            });
            server.start();

            Player p = new Player(new Vector2(500, 500), 0, batch, this);
            stage.addActor(p);
            playerId = stage.getActors().size - 1;
            try {
                server.bind(54555, 54777);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            client.addListener(new Listener() {
                @Override
                public void received(Connection connection, Object object) {
                    // New player message
                    if (object instanceof NetNewPlayer) {
                        NetNewPlayer response = (NetNewPlayer) object;
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                newPlayer(new Vector2(response.x, response.y), response.angle);
                            }
                        }, 0);
                    }

                    // Set player id message
                    if (object instanceof NetSetPlayerId) {
                        NetSetPlayerId response = (NetSetPlayerId) object;
                        Timer.schedule(new Timer.Task() {
                            @Override
                            public void run() {
                                playerId = response.playerId;
                            }
                        }, 0);
                    }

                    // Turn message
                    if (object instanceof NetTurn) {
                        NetTurn response = (NetTurn) object;
                        if (response.playerId != playerId) {
                            getPlayer(response.playerId).angle = response.realAngle;

                            // Ignore if distance is too big
                            if (Math.abs(getPlayer(response.playerId).getX() - response.realY) < 10 &&
                                Math.abs(getPlayer(response.playerId).getY() - response.realY) < 10) {
                                getPlayer(response.playerId).setX(response.realX);
                                getPlayer(response.playerId).setY(response.realY);
                            }

                            if (response.direction.equals("left")) {
                                getPlayer(response.playerId).turnLeft();
                            } else if (response.direction.equals("right")) {
                                getPlayer(response.playerId).turnRight();
                            } else {
                                getPlayer(response.playerId).stopTurning();
                            }
                        }
                    }

                    // Set ready message
                    if (object instanceof NetSetReady) {
                        NetSetReady response = (NetSetReady)object;
                        setPlayerReady(response.playerId);
                    }
                }

                @Override
                public void connected(Connection connection) {
                    client.sendTCP("JOINED");
                }
            });
            client.start();
            try {
                client.connect(5000, address, 54555, 54777);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    public void setPlayerReadyNet() {
        setPlayerReady(playerId);
        if (isServer) {
            NetSetReady setPlayerReady = new NetSetReady();
            setPlayerReady.playerId = playerId;
            server.sendToAllTCP(setPlayerReady);
        } else {
            NetSetReady setPlayerReady = new NetSetReady();
            setPlayerReady.playerId = playerId;
            client.sendTCP(setPlayerReady);
        }
    }

    public void setPlayerReady(int playerId) {
        readyButton.setVisible(false);
        getPlayer(playerId).ready = true;
    }

    public boolean gameStarted() {
        for (Actor a:
             stage.getActors()) {
            if ((a instanceof Player)) {
                Player p = (Player) a;
                if (!p.ready) {
                    return false;
                }
            }
        }
        return true;
    }

    private void registerKryoClasses(Kryo kryo) {
        kryo.register(NetNewPlayer.class);
        kryo.register(NetSetPlayerId.class);
        kryo.register(NetTurn.class);
        kryo.register(NetSetReady.class);
    }

    public void newPlayer(Vector2 startPosition, float angle) {
        Player p = new Player(startPosition, angle, batch, this);
        stage.addActor(p);
    }

    public Player getPlayer() {
        return (Player)stage.getActors().get(playerId);
    }

    public Player getPlayer(int playerId) {
        return (Player)stage.getActors().get(playerId);
    }

    @Override
    public void render() {
        float delta = Gdx.graphics.getDeltaTime();
        ScreenUtils.clear(bgColor);

        stage.act(delta);
        stage.draw();
    }

    public void turnPlayer(String direction) {
        Player player = (Player)stage.getActors().get(playerId);
        if (direction.equals("left")) {
            player.turnLeft();
        } else {
            player.turnRight();
        }

        NetTurn turnMsg = new NetTurn();
        turnMsg.playerId = playerId;
        turnMsg.realAngle = getPlayer().angle;
        turnMsg.direction = direction;

        if (isServer) {
            turnMsg.realX = getPlayer().getX();
            turnMsg.realY = getPlayer().getY();

            server.sendToAllTCP(turnMsg);
        } else {
            client.sendTCP(turnMsg);
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public boolean keyDown(int keycode) {
        if (keycode == Input.Keys.LEFT) {
            turnPlayer("left");
            return true;
        }
        if (keycode == Input.Keys.RIGHT) {
            turnPlayer("right");
            return true;
        }
        if (keycode == Input.Keys.CENTER) {
            setPlayerReadyNet();
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }


    @Override public boolean touchDown (int screenX, int screenY, int pointer, int button) {
        if (screenX > Gdx.graphics.getWidth() / 2) {
            turnPlayer("left");
        } else {
            turnPlayer("right");
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        Player player = (Player)stage.getActors().get(playerId);
        player.stopTurning();

        NetTurn turnMsg = new NetTurn();
        turnMsg.playerId = playerId;
        turnMsg.realAngle = getPlayer().angle;
        turnMsg.direction = "stop";

        if (isServer) {
            turnMsg.realX = getPlayer(turnMsg.playerId).getX();
            turnMsg.realY = getPlayer(turnMsg.playerId).getY();
            server.sendToAllTCP(turnMsg);
        } else {
            client.sendTCP(turnMsg);
        }
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }


    @Override
    public void dispose() {
        batch.dispose();
    }
}
