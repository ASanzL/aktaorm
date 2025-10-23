package io.onlinevara.orm;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputMultiplexer;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.utils.Array;
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
import java.util.ArrayList;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Main extends ApplicationAdapter implements InputProcessor {
    private SpriteBatch batch;
    private Stage stage;

    private final int snakeSize = 16;
    Skin uiSkin;
    TextButton readyButton;
    int readyButtonStageId = -1;

    private boolean gameIsWon = false;

    private boolean isServer = false;

    private int playerId = -1;

    private Server server;
    private Client client;

    private final Color BACKGROUND_COLOR = Color.BLACK;

    private final static int VIEWPORT_WIDTH = 1920;
    private final static int VIEWPORT_HEIGHT = 1080;

    @Override
    public void create() {
        batch = new SpriteBatch();
        stage = new Stage(new FitViewport(VIEWPORT_WIDTH, VIEWPORT_HEIGHT));
        Texture backgroundImage = new Texture(Gdx.files.internal("background.png"));
        stage.addActor(new Image(backgroundImage));
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
        readyButtonStageId = stage.getActors().size - 1;
        readyButton.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
                readyButton.setVisible(false);
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
                        Vector2 startingPosition = Player.getRandomPosition();
                        message.x = startingPosition.x;
                        message.y = startingPosition.y;
                        message.angle = MathUtils.random(360);
                        newPlayer(new Vector2(message.x, message.y), message.angle);
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

            // Create the servers player
            Player p = new Player(Player.getRandomPosition(), MathUtils.random(360), this, getPlayers().size(), snakeSize);
            stage.addActor(p);
            playerId = stage.getActors().size - 1;
            try {
                server.bind(54555,54777);
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

                    // Set player dead message
                    if (object instanceof NetSetPlayerDead) {
                        NetSetPlayerDead response = (NetSetPlayerDead)object;
                        if (getPlayerByColorId(response.snakeColorId) != null) {
                            getPlayerByColorId(response.snakeColorId).isDead = true;
                            getPlayerByColorId(response.snakeColorId).getHeadPosition().x = response.finalPosition.x;
                            getPlayerByColorId(response.snakeColorId).getHeadPosition().y = response.finalPosition.y;
                        }
                    }

                    // Reset player message
                    if (object instanceof NetResetPlayer) {
                        NetResetPlayer response = (NetResetPlayer)object;
                        getPlayerByColorId(response.snakeColorId).setX(response.playerPosition.x);
                        getPlayerByColorId(response.snakeColorId).setY(response.playerPosition.y);
                        getPlayerByColorId(response.snakeColorId).resetPlayer();
                        stage.getActors().get(readyButtonStageId).setVisible(true);
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
        kryo.register(Vector2.class);
        kryo.register(Array.class);
        kryo.register(Object.class);
        kryo.register(NetNewPlayer.class);
        kryo.register(NetSetPlayerId.class);
        kryo.register(NetTurn.class);
        kryo.register(NetSetReady.class);
        kryo.register(NetSetPlayerDead.class);
        kryo.register(NetResetPlayer.class);
    }

    public void newPlayer(Vector2 startPosition, float angle) {
        Player p = new Player(startPosition, angle, this, getPlayers().size(), snakeSize);
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
        ScreenUtils.clear(BACKGROUND_COLOR);

        if (isServer && gameStarted() && !checkForWin()) {
            handleCollision();
        } else if (isServer && checkForWin() && !gameIsWon) {
            gameIsWon = true;
            Timer.schedule(new Timer.Task() {
                @Override
                public void run() {
                    handleWin();
                    gameIsWon = false;
                }
            }, 5);
        }

        stage.act(delta);
        stage.draw();
    }

    /**
     * Handle any collision for a player
     */
    public void handleCollision() {
        Player collidedPlayer = checkForCollision();
        if (collidedPlayer != null) {
            collidedPlayer.isDead = true;
            NetSetPlayerDead netSetPlayerDeadMessage = new NetSetPlayerDead();
            netSetPlayerDeadMessage.snakeColorId = collidedPlayer.snakeColorId;
            netSetPlayerDeadMessage.finalPosition = collidedPlayer.getHeadPosition();
            server.sendToAllTCP(netSetPlayerDeadMessage);
        }
    }

    /**
     * Checks for the player id of if any new collisions
     * @return the player who has collided
     */
    public Player checkForCollision() {
        // Check all players against all other players collision points
        for (Player playerToCheck:
            getPlayers()) {
            if (playerToCheck.getX() < (float) snakeSize / 2 || playerToCheck.getX() > VIEWPORT_WIDTH - (float) snakeSize / 2 ||
                playerToCheck.getY() < (float) snakeSize / 2 || playerToCheck.getY() > VIEWPORT_HEIGHT - (float) snakeSize / 2) {
                return playerToCheck;
            }
            for (Player player:
                getPlayers()) {
                // No need to check a player who already has collided
                if (playerToCheck.isDead) {
                    break;
                }
                for (int i = 0; i < player.collisionPoints.size; i++) {
                    /* If a player is checking against it self -
                     dont check the last few points - so the player dont immediately collided with itself */
                    if (playerToCheck == player && i > player.collisionPoints.size - 8) {
                        break;
                    }
                    /* If distance between the players head and a collision point is less then the width of a snake
                     it has collided */
                    if (Vector2.dst(playerToCheck.getHeadPosition().x,
                        playerToCheck.getHeadPosition().y,
                        player.collisionPoints.get(i).x,
                        player.collisionPoints.get(i).y)
                        < playerToCheck.snakeSize) {
                        return playerToCheck;
                    }
                }
            }
        }
        return null;
    }

    public boolean checkForWin() {
        int numberOfDeaths = 0;
        ArrayList<Player> players = getPlayers();
        for (Player p :
            players) {
            if (p.isDead) {
                numberOfDeaths++;
            }
        }
        // If one player, end game when they die. If more players end when only one player remaining
        if (players.size() == 1 && numberOfDeaths == 1) {
            return true;
        } else if (players.size() > 1 ) {
            return numberOfDeaths >= players.size() - 1;
        }
        return false;
    }

    public void handleWin() {
        Array<Vector2> newPlayerPositions = new Array<>();
        for (Player p :
            getPlayers()) {
            Vector2 newPosition = Player.getRandomPosition();
            newPlayerPositions.add(newPosition);
            p.setX(newPosition.x);
            p.setY(newPosition.y);
            p.resetPlayer();
        }

        stage.getActors().get(readyButtonStageId).setVisible(true);

        for (Player p :
            getPlayers()) {
            NetResetPlayer netResetPlayerMessage = new NetResetPlayer();
            netResetPlayerMessage.playerPosition = p.getHeadPosition();
            netResetPlayerMessage.snakeColorId = p.snakeColorId;;
            server.sendToAllTCP(netResetPlayerMessage);
        }
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

    public Player getPlayerByColorId(int colorId) {
        for (Player p :
            getPlayers()) {
            if (p.snakeColorId == colorId) {
                return p;
            }
        }
        return null;
    }

    public static int getGameWidth() {
        return VIEWPORT_WIDTH;
    }
    public static int getGameHeight() {
        return VIEWPORT_HEIGHT;
    }

    public ArrayList<Player> getPlayers() {
        ArrayList<Player> players = new ArrayList<>();
        for (Actor a :
            stage.getActors()) {
            if (a instanceof Player) {
                players.add((Player)a);
            }
        }
        return players;
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
            readyButton.setVisible(false);
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
