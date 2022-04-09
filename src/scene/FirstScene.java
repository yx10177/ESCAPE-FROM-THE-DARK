package scene;

import camera.Camera;
import camera.MapInformation;
import camera.SmallMap;
import controller.AudioResourceController;
import controller.ImageController;
import controller.SceneController;
import gameobj.*;
import map.*;
import menu.Button;
import menu.MouseTriggerImpl;
import menu.Theme;
import utils.*;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import static utils.Global.*;
import static utils.Global.CAMERA_HEIGHT;

public class FirstScene extends Scene {
    private Image backGround;
    private Image wordImg;
    private PlayerIcon playerIcon;
    private DisplayIcon keyIcon;
    private DisplayIcon fireIcon;
    private DisplayIcon lightningIcon;
    private DisplayIcon oilLampIcon;

    private ArrayList<GameObject> floorArr;
    private ArrayList<GameObject> spikeArr;
    private ArrayList<GameObject> wallArr;
    private ArrayList<GameObject> stairArr;
    private ArrayList<GameObject> smallWallArr;
    private Node[][] searchMap;

    private Player player;
    private int playerNum;
    private Delay deadDelay = new Delay(90);

    private Delay doubleAreaDelay = new Delay(60 * 10);
    private Glow glow;
    private Key key;
    private Fire fire;

    private ArrayList<Monster> monsters;

    private Camera camera;
    private Delay shakeCameraDelay;
    private SmallMap smallMap;
    private Set<GameObject> displayArea;
//    private ActiveSpike activeSpike = new ActiveSpike(1984, 1600);

    private int lastCommandCode = 0;

    public FirstScene(int num) {
        this.playerNum = num;
    }

    @Override
    public void sceneBegin() {
        //資訊欄
        backGround = ImageController.getInstance().tryGet("/background_grey.png");
        wordImg = ImageController.getInstance().tryGet("/word.png");
        playerIcon = new PlayerIcon(1100, 40);
        keyIcon = new DisplayIcon("/getkey_img.png", 1248, 138+25);
        fireIcon = new DisplayIcon("/getcandle_img.png", 1133, 253+25);
        lightningIcon = new DisplayIcon("/lockLightning.png", 1133, 138+25);
        oilLampIcon = new DisplayIcon("/lockLamp.png", 1248, 253+25);
        //地圖
        MapInformation.setMapInfo(0, 0, Global.MAP_WIDTH, Global.MAP_HEIGHT);
        floorArr = new ArrayList<>();
        spikeArr = new ArrayList<>();
        wallArr = new ArrayList<>();
        stairArr = new ArrayList<>();
        smallWallArr = new ArrayList<>();
        mapInit();
        //創建地圖資訊 怪物追蹤用
        searchMap = new Node[18][18];
        setSearchMap();

        //角色
        deadDelay.loop();
        player = new Player(playerNum, 2112, 1600);
        glow = new Glow(2112, 1600, player);

        key = new Key(320, 1600);
        fire = new Fire(1216, 960, 1);

        monsters = new ArrayList<>();
        monsters.add(new Monster(0, 1216, 1600, 60));
        monsters.add(new Monster(1, 448, 1600, 45));
        monsters.add(new Monster(3, 1216, 1216, 30));

        //鏡頭
        camera = new Camera.Builder(Global.CAMERA_WIDTH, Global.CAMERA_HEIGHT)
                .setCameraStartLocation(300, 1500)
                .setChaseObj(player, 50, 50).gen();
        shakeCameraDelay = new Delay(75);
        //小地圖
        smallMap = new SmallMap(new Camera.Builder(MAP_WIDTH, MAP_HEIGHT)
                .setCameraWindowLocation(CAMERA_WIDTH + 36, CAMERA_HEIGHT - 330)//小視窗顯示的位置
                .setCameraStartLocation(64, 64).gen(),
                0.13, 0.13);
        displayArea = new HashSet<>();
        //music
        AudioResourceController.getInstance().stop("/fire.wav");
        AudioResourceController.getInstance().play("/background.wav");
    }
    @Override
    public void sceneEnd() {

    }
    @Override
    public CommandSolver.MouseListener mouseListener() {
        return null;
    }
    @Override
    public CommandSolver.KeyListener keyListener() {
        return new CommandSolver.KeyListener() {
            @Override
            public void keyPressed(int commandCode, long trigTime) {
            }

            @Override
            public void keyReleased(int commandCode, long trigTime) {
                if (commandCode == Global.UP || commandCode == Global.DOWN
                        || commandCode == Global.LEFT || commandCode == Global.RIGHT) {
                    playerMove(commandCode);
                    lastCommandCode = commandCode;
                }
                if (commandCode == SPACE) {
                    glow.changeState();
                }
            }

            @Override
            public void keyTyped(char c, long trigTime) {
            }
        };
    }

    @Override
    public void paint(Graphics g) {
        camera.start(g);
//        floorArr.forEach(a -> a.paint(g));
//        spikeArr.forEach(a -> a.paint(g));
//        wallArr.forEach(a -> a.paint(g));
//        stairArr.forEach(a -> a.paint(g));
        closeView(g);
        glow.paint(g);
        if (key != null) {
            key.paint(g);
        }
        fire.paint(g);
//        activeSpike.paint(g);
        monsters.forEach(a -> a.paint(g));
        player.paint(g);
        camera.end(g);

        g.setColor(new Color(153, 153, 153));
        g.fillRect(1070, 0, 400, 900);
        g.drawImage(wordImg, 1140, 400+25, 230, 120, null);

        smallMap.start(g);
        smallWallArr.forEach(a -> a.paint(g));
        displayArea.forEach(a->{if(smallMap.isCollision(a)){
            a.paint(g);
        }});
        printFireRange(g);
        if (smallMap.isCollision(player)) {
            smallMap.paint(g, player, player.getSmallImage(), 128, 128);
        }
        if (key != null && smallMap.isCollision(key)) {
            smallMap.paint(g, key, key.getSmallMapImage(), 128, 128);
        }

        smallMap.end(g);

        g.setColor(new Color(	192,	192,	192,90));
        g.fillRect(1120, 40, 272, 80);
        g.setColor(new Color(204,204,204));
        g.fillRect(1120, 40, 88, 80);
        playerIcon.paint(g, playerNum, deathCount);
        g.drawImage(backGround, 1140, 146+25, 230, 230, null);
        keyIcon.paint(g);
        fireIcon.paint(g);
        lightningIcon.paint(g);
        oilLampIcon.paint(g);

        Font font = new Font("Charlemagne Std", Font.PLAIN, 60);
        g.setFont(font);
        g.setColor(new Color(36, 58, 72));
        g.drawString(String.valueOf(deathCount), 1100+205, 103);
    }

    @Override
    public void update() {
        System.out.println(player.collider().centerX() + "," + player.collider().centerY());
        playerDead();
        shakeCamera();
        wall();
        spike();
        stair();
        key();
        fire();
//        player.setLastPosition(player.collider().centerX(), player.collider().centerY());
        monster();

        if (doubleAreaDelay.count()) {
            glow.resetArea();
        }
        glow.update();
        smallMapDisplayArea();
        camera.update();
    }

    public void mapInit() {
        floorArr = Map.genFloor("/Map01.bmp", "/Map01.txt", "floor0");
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_down"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_down_up"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_left"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_left_down"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_left_up"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_leftx3"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_right"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_right_down"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_right_left"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_right_up"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_rightx3"));
        floorArr.addAll(Map.genFloor("/Map01.bmp", "/Map01.txt", "floor_wall_up"));


        spikeArr.addAll(Map.genSpike("/Map01.bmp", "/Map01.txt", "spike0"));
        stairArr = Map.genStair("/Map01.bmp", "/Map01.txt", "stair0_left");
        wallArr = Map.genWall("/Map01.bmp", "/Map01.txt", "brownwall");
        smallWallArr = Map.genWall("/Map19x19.bmp", "/Map19x19.txt", "wall_smallmap");
    }

    public void printFireRange(Graphics g) {
        wallArr.forEach(a -> {
            if (fire.getState() == Fire.FireState.GOTTEN && fire.inRange(a)) {
                a.paint(g);
            }
        });
        floorArr.forEach(a -> {
            if (fire.getState() == Fire.FireState.GOTTEN && fire.inRange(a)) {
                a.paint(g);
            }
        });
        spikeArr.forEach(a -> {
            if (fire.getState() == Fire.FireState.GOTTEN && fire.inRange(a)) {
                a.paint(g);
            }
        });
    }

    public void closeView(Graphics g) {
        printFireRange(g);
        if (glow.getState() == Glow.GlowState.ON) {
            wallArr.forEach(a -> {
                if (glow.inRange(a)) {
                    a.paint(g);
                }
            });
            floorArr.forEach(a -> {
                if (glow.inRange(a) || player.isCollisionCenter(a)) {
                    a.paint(g);
                }
                for (int i = 0; i < monsters.size(); i++) {
                    if (monsters.get(i).isCollisionCenter(a)
                            && monsters.get(i).getState() == Monster.MonsterState.ALIVE) {
                        a.paint(g);
                    }
                }
            });
            stairArr.forEach(a -> {
                if (glow.inRange(a) || player.isCollisionCenter(a)) {
                    a.paint(g);
                }
            });
            spikeArr.forEach(a -> {
                if (glow.inRange(a) || player.isCollisionCenter(a)) {
                    a.paint(g);
                }
                for (int i = 0; i < monsters.size(); i++) {
                    if (monsters.get(i).isCollisionCenter(a)
                            && monsters.get(i).getState() == Monster.MonsterState.DEAD) {
                        a.paint(g);
                    }
                }
            });
        }
    }

    public void setSearchMap() {
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < 18; j++) {
                int x = j * 128 + 64;
                int y = i * 128 + 64;
                searchMap[i][j] = new Node(x, y);
                searchMap[i][j].isObstacle = true;
                for (int k = 0; k < floorArr.size(); k++) {
                    if (floorArr.get(k).collider().centerX() == x
                            && floorArr.get(k).collider().centerY() == y) {
                        searchMap[i][j].isObstacle = false;
                    }
                }
                for (int k = 0; k < spikeArr.size(); k++) {
                    if (spikeArr.get(k).collider().centerX() == x
                            && spikeArr.get(k).collider().centerY() == y) {
                        searchMap[i][j].isObstacle = false;
                    }
                }
            }
        }
    }

    public void printMap() {
        System.out.println("--------------------地圖資訊--------------------------");
        for (int i = 0; i < searchMap.length; i++) {
            for (int j = 0; j < searchMap[i].length; j++) {
                System.out.println(searchMap[i][j].x + ", " + searchMap[i][j].y + "," + searchMap[i][j].isObstacle);
            }
        }
    }

    public void playerMove(int commandCode) {
        if (player.getState() == Player.PlayerState.ALIVE) {
            if (lastCommandCode != commandCode) {
                player.setDirection(commandCode);
            }
            if (lastCommandCode == commandCode) {
                player.setDirection(commandCode);
                player.update();
            }
        }
    }

    public void playerDead() {
        if (deadDelay.count() && player.getState() != Player.PlayerState.ALIVE) {
            deathCount++;
            SceneController.getInstance().change(new TransitionScene(0, playerNum));
        }
    }


    public void shakeCamera() {
        if (shakeCameraDelay.isPlaying()) {
            camera.translateX(random(-5, 5));
            camera.translateY(random(-5, 5));
        }
        if (shakeCameraDelay.count()) {
            shakeCameraDelay.stop();
        }
    }

    public void floorRandomPosition(GadGetObject obj) {
        int random;
        do {
            random = random(0, floorArr.size() - 1);
        } while (Math.abs(floorArr.get(random).collider().centerX() - player.collider().centerX()) < 256
                || Math.abs(floorArr.get(random).collider().centerY() - player.collider().centerY()) < 256);

        obj.setGivenXY(floorArr.get(random).collider().centerX()
                , floorArr.get(random).collider().centerY());
    }


    public void key() {
        if (key != null) {
            key.update();
            floorRandomPosition(key);
            if(searchMap[(key.collider().centerY()-64)/UNIT][(key.collider().centerX()-64)/UNIT]
                    .isObstacle == true){
                return;
            }
            if (glow.isCollisionCenter(key)) {
                key.changeState(GadGetObject.State.DISCOVERED);
            } else {
                key.changeState(GadGetObject.State.UNDISCOVERED);
            }
            if (player.isCollisionCenter(key)) {
                AudioResourceController.getInstance().shot("/catchkey.wav");
                key = null;
                player.getKey();
                keyIcon.touch();
            }
        }
    }

    public void fire() {
        if (fire.getState() != Fire.FireState.GOTTEN) {
            if (glow.isCollisionCenter(fire)) {
                fire.changeState(Fire.FireState.DISCOVERED);
            }
            if (player.isCollisionCenter(fire)) {
                AudioResourceController.getInstance().play("/fire.wav");
                fire.changeState(Fire.FireState.GOTTEN);
                fireIcon.touch();
            }
        }
    }

    public void wall() {
        wallArr.forEach(g -> {
            if (g.isCollisionCenter(player)) {
                player.unmovable();
            }
        });
    }

    public void spike() {
        spikeArr.forEach(g -> {
            for (int i = 0; i < monsters.size(); i++) {
                if (g.isCollisionCenter(monsters.get(i))
                        && monsters.get(i).getState() != Monster.MonsterState.DEAD) {
                    monsters.get(i).changeState(Monster.MonsterState.DYING);
                    if (monsters.get(i).getState() == Monster.MonsterState.DYING) {
                        AudioResourceController.getInstance().play("/monster-dying.wav");
                        monsters.get(i).changeState(Monster.MonsterState.DEAD);
                        searchMap[monsters.get(i).collider().centerY() / UNIT][monsters.get(i).collider().centerX() / UNIT]
                                .isObstacle = true;
                    }
                }
                if (player.isCollisionCenter(monsters.get(i))
                        && monsters.get(i).getState() == Monster.MonsterState.DEAD) {
                    player.unmovable();
                }
            }
            if (g.isCollisionCenter(player) && player.getState() != Player.PlayerState.DEAD) {
                player.changeState(Player.PlayerState.DYING);
                if (player.getState() == Player.PlayerState.DYING) {
                    AudioResourceController.getInstance().play("/hit-arg.wav");
                    player.changeState(Player.PlayerState.DEAD);
                }
            }
        });

    }

    public void stair() {
        stairArr.forEach(g -> {
            if (g.isCollisionCenter(player) && player.keyCondition()) {
                AudioResourceController.getInstance().play("/dooropen.wav");
                SceneController.getInstance().change(new TowerScene(0, playerNum));
            }
        });
    }

    public void monster() {
        monsters.forEach(g -> {
            if (player.isCollisionCenter(g)) {
                AudioResourceController.getInstance().play("/headchop.wav");
                player.changeState(Player.PlayerState.DAMAGED_BY_MONSTER);
            }
            if (glow.inRange(g)
                    && g.getState() == Monster.MonsterState.SLEEP) {
                shakeCameraDelay.play();
                AudioResourceController.getInstance().play("/monster-moans.wav");
                g.changeState(Monster.MonsterState.ALIVE);
            }
            if (g.getState() == Monster.MonsterState.ALIVE) {
                g.trackingPath(player, searchMap);
            }
            if (shakeCameraDelay.isStop()) {
                g.update();
            }
            if (fire.painter().overlap(g.painter())) {
                g.changeIsInRangeOfFire(true);
            }else{
                g.changeIsInRangeOfFire(false);
            }
        });
    }
    public void smallMapDisplayArea(){
        if(glow.getState() ==Glow.GlowState.ON) {
            floorArr.forEach(a -> {
                if (glow.inRange(a)) {
                    displayArea.add(a);
                }
            });
            spikeArr.forEach(a -> {
                if (glow.inRange(a)) {
                    displayArea.add(a);
                }
            });
            wallArr.forEach(a -> {
                if (glow.inRange(a)) {
                    displayArea.add(a);
                }
            });
        }
    }
}