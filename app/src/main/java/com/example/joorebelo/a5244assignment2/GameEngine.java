package com.example.joorebelo.a5244assignment2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class GameEngine extends SurfaceView implements Runnable{
    private final String TAG = "joaoR";

    //sounds
    final MediaPlayer laser;
    final MediaPlayer lose;
    final MediaPlayer shieldDown;
    final MediaPlayer enemyDied;
    final MediaPlayer win;

    // game thread variables
    private Thread gameThread = null;
    private volatile boolean gameIsRunning;

    // drawing variables
    private Canvas canvas;
    private Paint paintbrush;
    private SurfaceHolder holder;

    // Screen resolution varaibles
    private int screenWidth;
    private int screenHeight;

    //backgroud parallax
    private Sprite bg1;
    private Sprite bg2;

    // GAME STATS
    boolean gameNotStarted = true;
    int score = 0;
    Sprite player;
    int gameStatus = -1;
    int greenIsMoving = -1;
    int redIsMoving = -1;
    int blueIsMoving = -1;
    boolean moveGreenEnemies = true;
    boolean moveRedEnemies1 = true;
    boolean moveBlueEnemies1 = true;

    //time
    Stopwatch gameTime;

    //CountDownTimer twomd
    private CountDownTimer countDownTimer;
    private long timeLeftInMilliseconds = 60000;
    boolean timerRunning = false;
    Sprite twomd;
    int twomdStatus = 1;
    String twomdcooldown;

    // bullets
    ArrayList<Sprite> bullets = new ArrayList<Sprite>();

    //lives
    int livesRemaining = 3;
    ArrayList<Sprite> lives = new ArrayList<Sprite>();
    boolean shieldUp = true;

    //enemies
    int enemiesRemaining = 40;
    int greenEnemies = 4;
    int redEnemies = 16;
    int blueEnemies = 20;
    String clock = "0";
    int enemyAtackCooldown = 0;

    Map<Integer, Enemies> enemiesMap;
    Map<Integer, Enemies> enemiesInPursuit;

    // VISIBLE GAME PLAY AREA
    int VISIBLE_LEFT;
    int VISIBLE_TOP;
    int VISIBLE_RIGHT;
    int VISIBLE_BOTTOM;

    int posibleClickX;
    int posibleClickY;
    Context context;

    public GameEngine(final Context context, int screenW, int screenH) {
        super(context);
        this.context = context;
        //sounds
        laser = MediaPlayer.create(context, R.raw.sfx_laser2);
        lose = MediaPlayer.create(context, R.raw.sfx_lose);
        shieldDown = MediaPlayer.create(context, R.raw.sfx_shielddown);;
        enemyDied = MediaPlayer.create(context, R.raw.sfx_twotone);;
        win = MediaPlayer.create(context, R.raw.sfx_zap);;

        // set screen height and width
        this.screenWidth = screenW;
        this.screenHeight = screenH;
        this.gameStatus = -1;

        restartGame();

        //region Swipe Listeners
        this.setOnTouchListener(new OnSwipeTouchListener(context) {
            public void onSwipeTop() {
                Log.d(TAG, "Swipe top");
                if (twomdStatus == 1 && gameStatus == 3){
                    final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.bomb);
                    laser2.start();
                    laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        public void onCompletion(MediaPlayer mp) {
                            mp.release();

                        };
                    });
                    weaponOfMassDestruction();
                    startStop();
                }
            }
            public void onSwipeRight() {
                Log.d(TAG, "Swipe right");
                if(gameStatus == 3){
                    changePlayerPosition("right");
                }
            }
            public void onSwipeLeft() {
                Log.d(TAG, "Swipe left");
                if(gameStatus == 3){
                    changePlayerPosition("left");
                }
            }
            public void onSwipeBottom() {
                //Toast.makeText(context, "bottom", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Swipe bottom");
            }
        });
        //endregion Swipe Listeners
    }

    public void restartGame() {
        Log.d(TAG, "----------------REstart------------------");



        this.gameNotStarted = true;
        this.score = 0;

        this.greenIsMoving = -1;
        this.redIsMoving = -1;
        this.blueIsMoving = -1;
        this.moveGreenEnemies = true;
        this.moveRedEnemies1 = true;
        this.moveBlueEnemies1 = true;

        //CountDownTimer twomd
        timerRunning = false;
        this.timeLeftInMilliseconds = 60000;
        this.twomdStatus = 1;


        // bullets
        this.bullets = new ArrayList<Sprite>();

        //lives
        this.livesRemaining = 3;
        this.lives = new ArrayList<Sprite>();
        this.shieldUp = true;

        //enemies
        this.enemiesRemaining = 40;
        this.clock = "0";
        this.enemyAtackCooldown = 0;



        ////////////////////////
        this.gameTime = new Stopwatch();
        // intialize the drawing variables
        this.holder = this.getHolder();
        this.paintbrush = new Paint();



        // setup visible game play area variables
        this.VISIBLE_LEFT = 20;
        this.VISIBLE_TOP = 10;
        this.VISIBLE_RIGHT = this.screenWidth - 20;
        this.VISIBLE_BOTTOM = (int) (this.screenHeight * 0.95);

        //region init sprites
        //background
        this.bg1 = new Sprite(this.getContext(), 0, 0, R.drawable.bg_stars, screenWidth, screenHeight);
        this.bg2 = new Sprite(this.getContext(), 0, screenHeight, R.drawable.bg_stars, screenWidth, screenHeight);


        // initalize player + twomd
        this.player = new Sprite(this.getContext(), screenWidth/2, VISIBLE_BOTTOM-200, R.drawable.player_ship1_red_shield, 100, 100);
        this.twomd = new Sprite(this.getContext(), screenWidth-120, VISIBLE_BOTTOM-90, R.drawable.missile, 70, 100);

        // initalize lives
        this.lives.add(new Sprite(this.getContext(), VISIBLE_LEFT+10, VISIBLE_BOTTOM-50, R.drawable.player_ship1_red, 50,50));
        this.lives.add(new Sprite(this.getContext(), VISIBLE_LEFT+70, VISIBLE_BOTTOM-50, R.drawable.player_ship1_red, 50, 50));
        this.lives.add(new Sprite(this.getContext(), VISIBLE_LEFT+130, VISIBLE_BOTTOM-50, R.drawable.player_ship1_red, 50, 50));
        //endregion init sprites

        Log.d(TAG, "lives: " + lives.size());
        Log.d(TAG, "livesRemaining: " + livesRemaining);
        //region Create Enemies
        enemiesInPursuit = new HashMap<Integer, Enemies>();
        enemiesMap = new HashMap<Integer, Enemies>();
        if (enemiesMap.size() != 40){
            for (int i = 0; i < 40; i++) {
                if (i < greenEnemies){//green
                    enemiesMap.put(i, new Enemies(this.getContext(), 0, 0, R.drawable.enemy_green1, 80, 80, false, "green"));
                }else if(i < (redEnemies+greenEnemies)){//red
                    if (i < 12){
                        enemiesMap.put(i, new Enemies(this.getContext(), 0, 0, R.drawable.enemy_red2, 80, 80, false, "red"));
                    }else {
                        enemiesMap.put(i, new Enemies(this.getContext(), screenWidth, 0, R.drawable.enemy_red2, 80, 80, false, "red"));
                    }
                }else {//blue
                    if (i < 30){
                        enemiesMap.put(i, new Enemies(this.getContext(), 0, 0, R.drawable.enemy_blue3, 80, 80, false, "blue"));
                    }else {
                        enemiesMap.put(i, new Enemies(this.getContext(), screenWidth, 0, R.drawable.enemy_blue3, 80, 80, false, "blue"));
                    }
                }
            }
        }
        //show enemies
        Log.d(TAG, "----------------------------------");
        Log.d(TAG, enemiesMap.size()+"");
        Log.d(TAG, "----------------------------------");
        for ( Map.Entry<Integer, Enemies> entry : enemiesMap.entrySet() ) {
            Integer key = entry.getKey();
            Enemies value = entry.getValue();
            Log.d(TAG, "id: " + key + " - " + value.getColor());

        }
        Log.d(TAG, "----------------------------------");
        //endregion Create Enemies
    }

    private void weaponOfMassDestruction() {
        Log.d(TAG, "Color ");
        Random r = new Random();
        int color = 0;

        boolean foundColor = false;

        Integer[] enemiesSelected = {0, 0, 0};
        for (int i = 0 ; i < enemiesMap.size(); i++){
            if (enemiesMap.get(i).getColor().equals("green") && enemiesMap.get(i).isVisible()){
                enemiesSelected[0] += 1;
            }else if (enemiesMap.get(i).getColor().equals("red") && enemiesMap.get(i).isVisible()){
                enemiesSelected[1] += 1;
            }else if (enemiesMap.get(i).getColor().equals("blue") && enemiesMap.get(i).isVisible()){
                enemiesSelected[2] += 1;
            }
        }

        while (!foundColor){
            color = r.nextInt(3 - 0) + 0;
            Log.d(TAG, "Try Color: " + color);
            if(enemiesSelected[color] > 0){
                foundColor = true;
            }
        }

        Log.d(TAG, "Color Out: " + color);
        switch (color){
            case 0: //green
                for (int i = 0 ; i < 4; i++) {
                    if (enemiesMap.get(i).isVisible()){
                        enemiesMap.get(i).setVisible(false);
                        enemiesRemaining--;
                        this.score = this.score + 150;
                    }
                }
                break;
            case 1: //red
                for (int i = 4 ; i < 20; i++) {
                    if (enemiesMap.get(i).isVisible()){
                        enemiesMap.get(i).setVisible(false);
                        enemiesRemaining--;
                        this.score = this.score + 100;
                    }
                }
                break;
            case 2: //blue
                for (int i = 20 ; i < 40; i++) {
                    if (enemiesMap.get(i).isVisible()){
                        enemiesMap.get(i).setVisible(false);
                        enemiesRemaining--;
                        this.score = this.score + 50;
                    }
                }
                break;
            default:
                for (int i = 0 ; i < 4; i++) {
                    if (enemiesMap.get(i).isVisible()){
                        enemiesMap.get(i).setVisible(false);
                        enemiesRemaining--;
                        this.score = this.score + 150;
                    }

                }
        }
        twomdStatus = 0;
    }

    private void changePlayerPosition(String direction){
        if (direction.equals("right")){
            if (!(this.player.getxPosition() >= VISIBLE_RIGHT-120)) {
                this.player.setxPosition(this.player.getxPosition()+50);
            }
        }else{
            if (!(this.player.getxPosition() < VISIBLE_LEFT+30)) {
                this.player.setxPosition(this.player.getxPosition()-50);
            }

        }
        this.player.updateHitbox();
    }

    @Override
    public void run() {
        // @TODO: Put game loop in here
        while (gameIsRunning == true) {
            updateGame();    // updating positions of stuff
            redrawSprites(); // drawing the stuff
            controlFPS();
        }
    }

///------------------------- Start UpdateGame ---------------------------------------------------------------------------------
    /**
     * //gamestatus
     * -1 = click to start
     * 0 = Place Green enemies
     * 1 = Place Red enemies
     * 2 = Place Blue enemies
     * 3 = Player can play
     * 4 = player Won
     * 5 = Player Lost
     *
     **/
    public void updateGame() {
        parallaxBackground();
        while(gameStatus == -1){
            //refresh background
            parallaxBackground();
            redrawHome();
        }
        //region Place Green Enemies
        if (gameStatus == 0){
            if (greenIsMoving == -1 || greenIsMoving == 0){
                for ( Map.Entry<Integer, Enemies> entry : enemiesMap.entrySet() ) {
                    Integer key = entry.getKey();
                    Enemies enemy = entry.getValue();
                    int posX, posY, posX2, posY2;
                    if (greenIsMoving == -1){
                        posX = screenWidth/2;
                        posY = screenHeight-200;
                        if (enemiesMap.get(0).getyPosition() >= 763){
                            greenIsMoving = 0;
                        }
                    }else{
                        posX = screenWidth/2;
                        posY = 100;
                    }

                    if (key == 0){
                        enemy = distanceBetweenObj(enemy, posX, posY, key, 40);
                    }else if(key >= 4){
                        break;
                    }else{
                        if (canStartMoving(enemy, enemiesMap.get((key-1)).getxPosition(), enemiesMap.get((key-1)).getyPosition())){
                            enemy = distanceBetweenObj(enemy, enemiesMap.get((key-1)).getxPosition(), enemiesMap.get((key-1)).getyPosition(), key, 40);
                        }

                    }
                    enemy.setVisible(true);
                }

                if (enemiesMap.get(0).getyPosition() < 150 && greenIsMoving == 0){
                    gameStatus = 1;
                    int separator = (int) screenWidth/9;
                    int padding = separator*2;

                    for (int i = 0; i < 4; i++) {
                        enemiesMap.get(i).setxPosition(padding + (separator*(i+1)));
                        enemiesMap.get(i).setyPosition(150);
                        enemiesMap.get(i).updateHitbox();
                    }
                }
                moveEnemies();
            }
        }//endregion Place Green Enemies
        //region Place Red Enemies
        else if(gameStatus == 1){

            if (redIsMoving == -1 || redIsMoving == 0){
                for ( Map.Entry<Integer, Enemies> entry : enemiesMap.entrySet() ) {
                    Integer key = entry.getKey();
                    if(key < 4){
                        continue;
                    }
                    Enemies enemy = entry.getValue();
                    int posX, posY, posX2, posY2;
                    if (redIsMoving == -1){
                        posX = screenWidth/2;
                        posY = screenHeight-200;
                        if (enemiesMap.get(4).getyPosition() >= 1000){
                            redIsMoving = 0;
                        }
                    }else{
                        posX = screenWidth/2;
                        posY = 100;
                    }

                    if (key == 4 || key == 12){
                        enemy = distanceBetweenObj(enemy, posX, posY, key, 40);
                    }else if(key >= 20){
                        break;
                    }else{
                        if (canStartMoving(enemy, enemiesMap.get((key-1)).getxPosition(), enemiesMap.get((key-1)).getyPosition())){
                            enemy = distanceBetweenObj(enemy, enemiesMap.get((key-1)).getxPosition(), enemiesMap.get((key-1)).getyPosition(), key, 40);
                        }
                    }
                    enemy.setVisible(true);
                }

                if (enemiesMap.get(4).getyPosition() < 270 && redIsMoving == 0){
                    gameStatus = 2;
                    int separator = (int) screenWidth / 12;
                    int padding = (separator * 2) - 50;

                    int iAuxRow = 0;
                    for (int i = 4; i < 20; i++, iAuxRow++) {
                        if(iAuxRow == 8){iAuxRow = 0;} //restart counter for the secound row
                        if (i <12){
                            enemiesMap.get(i).setyPosition(270);
                            enemiesMap.get(i).setxPosition(padding + (separator*(iAuxRow+1)));
                        }else {
                            enemiesMap.get(i).setyPosition(380);
                            enemiesMap.get(i).setxPosition(padding + (separator*(iAuxRow+1)));
                        }
                        enemiesMap.get(i).updateHitbox();
                    }
                    /*
                    Log.d(TAG, "----------------------------------");
                    Log.d(TAG, enemiesMap.size()+"");
                    Log.d(TAG, "----------------------------------");
                    for ( Map.Entry<Integer, Enemies> entry : enemiesMap.entrySet() ) {
                        Integer key = entry.getKey();
                        Enemies value = entry.getValue();
                        if (key > 3 && key < 20)
                        Log.d(TAG, "id: " + key + " - X: " + value.getxPosition()+ " - Y: " + value.getyPosition());

                    }
                    Log.d(TAG, "----------------------------------");
                    */
                }
                moveEnemies();
            }
        }//endregion Place Red Enemies
        //region Place Blue Enemies
        else if(gameStatus == 2){

            if (blueIsMoving == -1 || blueIsMoving == 0){
                for ( Map.Entry<Integer, Enemies> entry : enemiesMap.entrySet() ) {
                    Integer key = entry.getKey();
                    if(key < 20){
                        continue;
                    }
                    Enemies enemy = entry.getValue();
                    int posX, posY, posX2, posY2;
                    if (blueIsMoving == -1){
                        posX = screenWidth/2;
                        posY = screenHeight-100;
                        if (enemiesMap.get(20).getyPosition() >= 1260){
                            blueIsMoving = 0;
                        }
                    }else{
                        posX = screenWidth/2;
                        posY = 100;
                    }

                    if (key == 20 || key == 30){
                        enemy = distanceBetweenObj(enemy, posX, posY, key, 40);
                    }else if(key >= 40){
                        break;
                    }else{
                        if (canStartMoving(enemy, enemiesMap.get((key-1)).getxPosition(), enemiesMap.get((key-1)).getyPosition())){
                            enemy = distanceBetweenObj(enemy, enemiesMap.get((key-1)).getxPosition(), enemiesMap.get((key-1)).getyPosition(), key, 40);
                        }
                    }
                    enemy.setVisible(true);
                }

                if (enemiesMap.get(20).getyPosition() < 500 && blueIsMoving == 0){
                    gameStatus = 3;
                    int separator = (int) screenWidth / 13;
                    int padding = (separator * 2) - 100;

                    int iAuxRow = 0;
                    for (int i = 20; i < 40; i++, iAuxRow++) {
                        if(iAuxRow == 10){iAuxRow = 0;} //restart counter for the secound row
                        if (i <30){
                            enemiesMap.get(i).setyPosition(500);
                            enemiesMap.get(i).setxPosition(padding + (separator*(iAuxRow+1)));
                        }else {
                            enemiesMap.get(i).setyPosition(620);
                            enemiesMap.get(i).setxPosition(padding + (separator*(iAuxRow+1)));
                        }
                        enemiesMap.get(i).updateHitbox();
                    }
                }
            }
            moveEnemies();
        }//endregion Place Blue Enemies
        //region Player can play
        else if(gameStatus == 3){
            moveEnemies();

            //one time variable to start the game timer
            if(gameNotStarted){gameTime.start(); gameNotStarted = false;}


            //region Bullets
            for (int i = 0 ; i < bullets.size(); i++) {
                this.bullets.get(i).setyPosition((this.bullets.get(i).getyPosition() - 50));
                this.bullets.get(i).updateHitbox();
            }
            ArrayList<Sprite> bulletsAux = this.bullets;

            for(int i = bullets.size()-1 ; i >= 0; i--){
                for ( Map.Entry<Integer, Enemies> entry : enemiesInPursuit.entrySet() ) {
                    Integer index = entry.getKey();
                    Enemies enemy = entry.getValue();
                    if (this.bullets.size()>0){
                        if (enemy.isVisible()) {
                            if (this.bullets.get(i).getHitbox().intersect(enemy.getHitbox())) {
                                enemy.setVisible(false);
                                enemiesInPursuit.remove(index);
                                bulletsAux.remove(bulletsAux.get(i));
                                //bulletHit = true;
                                enemiesRemaining--;
                                // 100- blue / 150 - red / 200 - green
                                int tempScore = (enemy.getColor()=="blue"? 100 :
                                        enemy.getColor()=="red"? 200 :
                                                enemy.getColor()=="green"? 300 : 0);
                                //reduced by time
                                if (gameTime.getElapsedTimeMin() < 1){// < 1 minutos
                                    this.score = this.score + (gameTime.getElapsedTimeSecs() > 10 ? tempScore :
                                            tempScore - (tempScore * ((int) gameTime.getElapsedTimeSecs()/10)));
                                }else { // > 1 minuto
                                    this.score = this.score - 10 + (gameTime.getElapsedTimeSecs() > 10 ? tempScore :
                                            tempScore - (tempScore * ((int) gameTime.getElapsedTimeSecs()/10)));
                                }
                                final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.sfx_twotone);
                                laser2.start();
                                laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.release();

                                    };
                                });

                                break;
                            }
                        }
                    }
                }
                this.bullets = bulletsAux;
            }
            //endregion Bullets

            //region Bullets
            for (int i = 0 ; i < bullets.size(); i++) {
                this.bullets.get(i).setyPosition((this.bullets.get(i).getyPosition() - 50));
                this.bullets.get(i).updateHitbox();
            }

            boolean bulletHit = false;
            for(int i = bullets.size()-1 ; i >= 0; i--){
                for (int g = 0 ; g < enemiesMap.size(); g++) {
                    if (this.bullets.size()>0){
                        if (enemiesMap.get(g).isVisible()) {
                            if (this.bullets.get(i).getHitbox().intersect(this.enemiesMap.get(g).getHitbox())) {
                                this.enemiesMap.get(g).setVisible(false);
                                bulletsAux.remove(bulletsAux.get(i));
                                //bulletHit = true;
                                enemiesRemaining--;
                                // 100- blue / 150 - red / 200 - green
                                int tempScore = (this.enemiesMap.get(g).getColor()=="blue"? 100 :
                                        this.enemiesMap.get(g).getColor()=="red"? 200 :
                                                this.enemiesMap.get(g).getColor()=="green"? 300 : 0);
                                //reduced by time
                                if (gameTime.getElapsedTimeMin() < 1){// < 1 minutos
                                    this.score = this.score + (gameTime.getElapsedTimeSecs() > 10 ? tempScore :
                                            tempScore - (tempScore * ((int) gameTime.getElapsedTimeSecs()/10)));
                                }else { // > 1 minuto
                                    this.score = this.score - 10 + (gameTime.getElapsedTimeSecs() > 10 ? tempScore :
                                            tempScore - (tempScore * ((int) gameTime.getElapsedTimeSecs()/10)));
                                }
                                final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.sfx_twotone);
                                laser2.start();
                                laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.release();

                                    };
                                });
                                break;
                            }
                        }
                    }
                }
                this.bullets = bulletsAux;
            }
            //endregion Bullets

            String currentTime = String.valueOf(gameTime.getElapsedTimeMin() + gameTime.getElapsedTimeSecs());
            //Log.d(TAG, "currentTime: " + currentTime);
            //Log.d(TAG, "clock: " + clock);

            if (Integer.parseInt(clock) != Integer.parseInt(currentTime)){
                //enemyAtackCooldown = "0";
                int c = Integer.parseInt(clock);
                if((c + 1) <= Integer.parseInt(currentTime)){
                    clock = String.valueOf(c+1);
                    enemyAtackCooldown--;
                }if(Integer.parseInt(currentTime) >= (c+10) || Integer.parseInt(currentTime) <= (c+10)){
                    clock = String.valueOf(currentTime);
                }
            }

            //region Suicide Enemy
            Random r = new Random();
            if (enemyAtackCooldown == 0){
                boolean isEnemiesSelected = false;
                while (!isEnemiesSelected){
                    int index = r.nextInt(39 - 0) + 0;
                    //Log.d(TAG, "Try index: " + index);

                    if(enemiesMap.get(index).isVisible()){
                        Enemies e = enemiesMap.get(index);
                        enemiesInPursuit.put(index, new Enemies(this.getContext(), e.getxPosition(), e.getyPosition(),
                                (e.getColor() == "green" ? R.drawable.enemy_green1 :
                                        e.getColor() == "red" ? R.drawable.enemy_red2 :
                                                e.getColor() == "blue" ? R.drawable.enemy_blue3 : R.drawable.enemy_green1),
                                80, 80, true, e.getColor()));
                        isEnemiesSelected = true;
                        enemiesMap.get(index).setVisible(false);
                        //Log.d(TAG, "Index Selected: " + index);
                        break;
                    }
                }

                for ( Map.Entry<Integer, Enemies> entry : enemiesInPursuit.entrySet() ) {
                    Integer index = entry.getKey();
                    Enemies enemy = entry.getValue();

                    // calculate distance between 2 points
                    double a = player.getxPosition() - enemy.getxPosition();//enemy.getxPosition() - player.getxPosition();
                    double b = player.getyPosition() - enemy.getyPosition();

                    // d = sqrt(a^2 + b^2)
                    double d = Math.sqrt((a * a) + (b * b));

                    // calculate xn and yn constants
                    double xn = (a / d);
                    double yn = (b / d);

                    enemy.xn = xn;
                    enemy.yn = yn;
                    enemy.updateHitbox();
                }

                enemyAtackCooldown = 5;

                //Log.d(TAG, "enemyAtackCooldown changed: " + enemyAtackCooldown);
            }else {
                if (enemiesInPursuit.size() > 0){
                    for ( Map.Entry<Integer, Enemies> entry : enemiesInPursuit.entrySet() ) {
                        Integer index = entry.getKey();
                        Enemies enemy = entry.getValue();

                        int newX = enemy.getxPosition() + (int) (enemy.xn * 20);
                        int newY = enemy.getyPosition() + (int) (enemy.yn * 20);
                        enemy.setxPosition(newX);
                        enemy.setyPosition(newY);
                        enemy.updateHitbox();
                        if (newX <= VISIBLE_LEFT || newY >= VISIBLE_BOTTOM || newX >= VISIBLE_RIGHT ){
                            //remove from the game
                            enemiesInPursuit.remove(index);
                            enemiesRemaining--;
                            this.score = this.score + 10;
                            continue;
                        }
                        if (enemy.getHitbox().intersect(player.getHitbox())){
                            enemiesInPursuit.remove(index);
                            enemiesRemaining--;
                            this.score = this.score - 10;
                            //Log.d("TAG", "-----Player was hit");
                            if (shieldUp){
                                shieldUp = false;
                                player.setImage(Bitmap.createScaledBitmap(BitmapFactory.decodeResource(context.getResources(), R.drawable.player_ship1_red), 80, 80, true));
                                player.updateHitbox();
                                final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.sfx_shielddown);
                                laser2.start();
                                laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.release();

                                    };
                                });
                            }else {
                                livesRemaining--;
                                final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.sfx_twotone);
                                laser2.start();
                                laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                                    public void onCompletion(MediaPlayer mp) {
                                        mp.release();

                                    };
                                });
                            }

                            if(livesRemaining < 1){
                                gameStatus = 5;
                                if(timerRunning){
                                    stopTimer();
                                }
                                break;
                            }
                            //Log.d("TAG", "-----gameStatus" + gameStatus);
                            //Log.d("TAG", "-----livesRemaining" + livesRemaining);
                        }
                    }
                }
            }
            //endregion Suicide Enemy

            if (enemiesRemaining <= 0){
                gameStatus = 4;
                if(timerRunning){
                    stopTimer();
                }
            }

        }//endregion Player can play
        //region Player Won
        else if(gameStatus == 4){
            final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.sfx_zap);
            laser2.start();
            laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    mp.release();

                };
            });
            while(gameStatus == 4){
                //refresh background
                parallaxBackground();
                redrawWin();
            }
        }//endregion Player Won
        //region Player lost
        else if(gameStatus == 5){
            final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.sfx_lose);
            laser2.start();
            laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    mp.release();

                };
            });
            while(gameStatus == 5){
                //refresh background
                parallaxBackground();
                redrawLost();
            }
        }
        //endregion Player lost

    }
///------------------------- End Start UpdateGame ---------------------------------------------------------------------------------

    private Enemies distanceBetweenObj(Enemies enemy, int posX, int posY, int i, int speed) {
        double a = posX - enemy.getxPosition();
        double b = posY - enemy.getyPosition();

        // d = sqrt(a^2 + b^2)
        double d = Math.sqrt((a * a) + (b * b));

        // 2. calculate xn and yn constants
        double xn = (a / d);
        double yn = (b / d);

        // 3. calculate new (x,y) coordinates
        int newX = enemy.getxPosition() + (int) (xn * speed);
        int newY = enemy.getyPosition() + (int) (yn * speed);
        enemy.setxPosition(newX);
        enemy.setyPosition(newY);
        // 4. update the bullet hitbox position
        enemy.updateHitbox();
        return enemy;
    }

    private Boolean canStartMoving(Enemies enemy, int posX, int posY) {
        double a = posX - enemy.getxPosition();
        double b = posY - enemy.getyPosition();

        double d = Math.sqrt((a * a) + (b * b));

        if (d > 100){
            return true;
        }else {
            return false;
        }
    }

    private void moveEnemies(){
        int maxEnenmiesMoving = 0;
        if (gameStatus == 1){maxEnenmiesMoving = greenEnemies;}
        else if(gameStatus == 2){maxEnenmiesMoving = greenEnemies + redEnemies;}
        else if(gameStatus == 3){maxEnenmiesMoving = greenEnemies + redEnemies + blueEnemies;}

        if (this.enemiesMap.get(3).getxPosition() >= VISIBLE_RIGHT-150) {
            moveGreenEnemies = false;
        }
        // colliding with left side of screen
        if (this.enemiesMap.get(0).getxPosition() < VISIBLE_LEFT+60 ) {
            moveGreenEnemies = true;
        }

        if (this.enemiesMap.get(19).getxPosition() >= VISIBLE_RIGHT - 150) {
            moveRedEnemies1 = false;
        }
        // colliding with left side of screen
        if (this.enemiesMap.get(4).getxPosition() < VISIBLE_LEFT + 60) {
            moveRedEnemies1 = true;
        }

        if (this.enemiesMap.get(39).getxPosition() >= VISIBLE_RIGHT - 150) {
            moveBlueEnemies1 = false;
        }
        // colliding with left side of screen
        if (this.enemiesMap.get(20).getxPosition() < VISIBLE_LEFT + 60) {
            moveBlueEnemies1 = true;
        }



        for (int i = 0; i < maxEnenmiesMoving; i++) {
            if (i < 4){
                if (moveGreenEnemies == true) {
                    this.enemiesMap.get(i).setxPosition(this.enemiesMap.get(i).getxPosition() + 20);
                }
                else {
                    this.enemiesMap.get(i).setxPosition(this.enemiesMap.get(i).getxPosition() - 20);
                }
            }else if (i < 20){
                if (moveRedEnemies1 == true) {
                    this.enemiesMap.get(i).setxPosition(this.enemiesMap.get(i).getxPosition() + 10);
                } else {
                    this.enemiesMap.get(i).setxPosition(this.enemiesMap.get(i).getxPosition() - 10);
                }
            }else if (i < 40){
                if (moveBlueEnemies1 == true) {
                    this.enemiesMap.get(i).setxPosition(this.enemiesMap.get(i).getxPosition() + 5);
                } else {
                    this.enemiesMap.get(i).setxPosition(this.enemiesMap.get(i).getxPosition() - 5);
                }
            }

            // update the enemies hitbox
            this.enemiesMap.get(i).updateHitbox();

        }
    }

    private void parallaxBackground() {
        this.bg1.setyPosition(this.bg1.getyPosition() + 30);
        this.bg2.setyPosition(this.bg2.getyPosition() + 30);

        if (this.bg1.getyPosition() >= VISIBLE_BOTTOM) {
            this.bg1.setyPosition(screenHeight-(2*screenHeight));
        }
        if (this.bg2.getyPosition() >= VISIBLE_BOTTOM) {
            this.bg2.setyPosition(screenHeight-(2*screenHeight));
        }
    }

    public void redrawSprites() {
        if (holder.getSurface().isValid()) {
            // initialize the canvas
            canvas = holder.lockCanvas();

            //background color
            canvas.drawColor(Color.argb(255,0,0,0));
            //background
            canvas.drawBitmap(this.bg1.getImage(), this.bg1.getxPosition(), this.bg1.getyPosition(), paintbrush);
            canvas.drawBitmap(this.bg2.getImage(), this.bg2.getxPosition(), this.bg2.getyPosition(), paintbrush);

            // --------------------------------------------------------
            //  player + hitbox
            // --------------------------------------------------------
            canvas.drawBitmap(this.player.getImage(), this.player.getxPosition(), this.player.getyPosition(), paintbrush);
            /*Rect r = player.getHitbox();
            paintbrush.setStyle(Paint.Style.STROKE);
            canvas.drawRect(r, paintbrush);*/

            // --------------------------------------------------------
            //  twomd + hitbox
            // --------------------------------------------------------
            if (twomdStatus == 1){
                canvas.drawBitmap(this.twomd.getImage(), this.twomd.getxPosition(), this.twomd.getyPosition(), paintbrush);
                /*Rect rectwomd = twomd.getHitbox();
                paintbrush.setStyle(Paint.Style.STROKE);
                canvas.drawRect(rectwomd, paintbrush);*/
            }else{
                paintbrush.setTextSize(50);
                paintbrush.setStyle(Paint.Style.FILL);
                canvas.drawText(""+ this.twomdcooldown, this.twomd.getxPosition()-20, this.twomd.getyPosition()+70, paintbrush);
            }

            // --------------------------------------------------------
            //  Game timer
            // --------------------------------------------------------
            if(gameStatus == 3){
                paintbrush.setTextSize(50);
                paintbrush.setStyle(Paint.Style.FILL);
                String gametime = "";
                if (this.gameTime.getElapsedTimeMin() < 10) {
                    gametime += "0";

                }gametime += this.gameTime.getElapsedTimeMin();
                gametime += ":";
                if (this.gameTime.getElapsedTimeSecs() < 10) {
                    gametime += "0";

                }gametime += this.gameTime.getElapsedTimeSecs();

                canvas.drawText(gametime, 30, 60, paintbrush);

                if (this.gameTime.getElapsedTimeMin() == 2){
                    if (enemiesRemaining > 0){
                        gameStatus = 5;
                        if(timerRunning){
                            stopTimer();
                        }
                    }else {
                        gameStatus = 4;
                        if(timerRunning){
                            stopTimer();
                        }
                    }
                }
            }

            // --------------------------------------------------------
            //  Score
            // --------------------------------------------------------
            paintbrush.setColor(Color.argb(255, 255, 0, 0));
            paintbrush.setTextSize(50);
            paintbrush.setStyle(Paint.Style.FILL);
            canvas.drawText("High Score", screenWidth/2-100, VISIBLE_TOP+50, paintbrush);
            canvas.drawText(""+this.score, screenWidth/2, VISIBLE_TOP+100, paintbrush);

            if (lives.size() == 3){
                for (int i = 0; i < this.livesRemaining; i++) {
                    canvas.drawBitmap(this.lives.get(i).getImage(), this.lives.get(i).getxPosition(), this.lives.get(i).getyPosition(), paintbrush);
                }
            }



            // --------------------------------------------------------
            //  Enemies
            // --------------------------------------------------------
            for ( Map.Entry<Integer, Enemies> entry : enemiesMap.entrySet() ) {
                Integer index = entry.getKey();
                Enemies enemy = entry.getValue();

                if (enemy.isVisible()){
                    canvas.drawBitmap(enemy.getImage(), enemy.getxPosition(), enemy.getyPosition(), paintbrush);
                    /*Rect re = enemy.getHitbox();
                    paintbrush.setStyle(Paint.Style.STROKE);
                    canvas.drawRect(re, paintbrush);*/
                }
            }

            // --------------------------------------------------------
            //  Enemies In Pursuit
            // --------------------------------------------------------
            for ( Map.Entry<Integer, Enemies> entry : enemiesInPursuit.entrySet() ) {
                Integer index = entry.getKey();
                Enemies enemy = entry.getValue();
                canvas.drawBitmap(enemy.getImage(), enemy.getxPosition(), enemy.getyPosition(), paintbrush);
               /* Rect re = enemy.getHitbox();
                paintbrush.setStyle(Paint.Style.STROKE);
                canvas.drawRect(re, paintbrush);*/
            }

            // --------------------------------------------------------
            //  Bullets
            // --------------------------------------------------------
            for (int i = 0; i < this.bullets.size(); i++) {
                canvas.drawBitmap(this.bullets.get(i).getImage(), this.bullets.get(i).getxPosition(), this.bullets.get(i).getyPosition(), paintbrush);
            }


            // --------------------------------

            // --------------------------------------------------------
            // draw boundaries of the visible space of app
            // --------------------------------------------------------
            paintbrush.setStrokeWidth(8);
            paintbrush.setStyle(Paint.Style.STROKE);
            paintbrush.setColor(Color.argb(255, 0, 128, 0));
            //canvas.drawRect(VISIBLE_LEFT, VISIBLE_TOP, VISIBLE_RIGHT, VISIBLE_BOTTOM, paintbrush);
/*
            //start game
            if (gameStatus == -1){
                paintbrush.setStyle(Paint.Style.FILL);
                paintbrush.setTextSize(50);
                paintbrush.setColor(Color.argb(255, 255, 0, 0));

                String screenInfo3 = "Click to Start the game";
                canvas.drawText(screenInfo3, this.screenWidth/2-250, screenHeight/2, paintbrush);
            }
*/
            /*
             * Game
             * */

            holder.unlockCanvasAndPost(canvas);
        }

    }

    private void redrawHome() {
        if (holder.getSurface().isValid()) {
            // initialize the canvas
            canvas = holder.lockCanvas();

            //background color
            canvas.drawColor(Color.argb(255,0,0,0));

            //background
            canvas.drawBitmap(this.bg1.getImage(), this.bg1.getxPosition(), this.bg1.getyPosition(), paintbrush);
            canvas.drawBitmap(this.bg2.getImage(), this.bg2.getxPosition(), this.bg2.getyPosition(), paintbrush);
            // player
            canvas.drawBitmap(this.player.getImage(), this.player.getxPosition(), this.player.getyPosition(), paintbrush);

            // draw the score
            paintbrush.setColor(Color.argb(255, 255, 0, 0));
            paintbrush.setTextSize(50);
            paintbrush.setStyle(Paint.Style.FILL);
            canvas.drawText("High Score", screenWidth/2-100, VISIBLE_TOP+50, paintbrush);
            canvas.drawText(""+this.score, screenWidth/2, VISIBLE_TOP+100, paintbrush);

            paintbrush.setStyle(Paint.Style.FILL);
            paintbrush.setTextSize(50);
            paintbrush.setColor(Color.argb(255, 255, 0, 0));

            String screenInfo3 = "Click to Start the game";
            canvas.drawText(screenInfo3, this.screenWidth/2-250, screenHeight/2, paintbrush);

            // --------------------------------
            holder.unlockCanvasAndPost(canvas);
        }

    }

    private void redrawLost(){
        if (holder.getSurface().isValid()) {
            // initialize the canvas
            canvas = holder.lockCanvas();

            //background color
            canvas.drawColor(Color.argb(255,0,0,0));

            //background
            canvas.drawBitmap(this.bg1.getImage(), this.bg1.getxPosition(), this.bg1.getyPosition(), paintbrush);
            canvas.drawBitmap(this.bg2.getImage(), this.bg2.getxPosition(), this.bg2.getyPosition(), paintbrush);
            // player
            canvas.drawBitmap(this.player.getImage(), this.player.getxPosition(), this.player.getyPosition(), paintbrush);

            // draw the score
            paintbrush.setColor(Color.argb(255, 255, 0, 0));
            paintbrush.setTextSize(50);
            paintbrush.setStyle(Paint.Style.FILL);
            canvas.drawText("Score", screenWidth/2-50, VISIBLE_TOP+50, paintbrush);
            canvas.drawText(""+this.score, screenWidth/2-10, VISIBLE_TOP+100, paintbrush);

            paintbrush.setStyle(Paint.Style.FILL);
            paintbrush.setTextSize(80);
            paintbrush.setColor(Color.argb(255, 255, 0, 0));

            String screenInfo2 = "You Lost";
            canvas.drawText(screenInfo2, this.screenWidth/2-100, screenHeight/2-400, paintbrush);
            paintbrush.setTextSize(50);
            String screenInfo3 = "Click to Leave";
            canvas.drawText(screenInfo3, this.screenWidth/2-150, screenHeight/2, paintbrush);

            // --------------------------------
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private void redrawWin(){
        if (holder.getSurface().isValid()) {
            // initialize the canvas
            canvas = holder.lockCanvas();

            //background color
            canvas.drawColor(Color.argb(255,0,0,0));

            //background
            canvas.drawBitmap(this.bg1.getImage(), this.bg1.getxPosition(), this.bg1.getyPosition(), paintbrush);
            canvas.drawBitmap(this.bg2.getImage(), this.bg2.getxPosition(), this.bg2.getyPosition(), paintbrush);
            // player
            canvas.drawBitmap(this.player.getImage(), this.player.getxPosition(), this.player.getyPosition(), paintbrush);

            // draw the score
            paintbrush.setColor(Color.argb(255, 255, 0, 0));
            paintbrush.setTextSize(50);
            paintbrush.setStyle(Paint.Style.FILL);
            canvas.drawText("Score", screenWidth/2-50, VISIBLE_TOP+50, paintbrush);
            canvas.drawText(""+this.score, screenWidth/2-10, VISIBLE_TOP+100, paintbrush);

            paintbrush.setStyle(Paint.Style.FILL);
            paintbrush.setTextSize(80);
            paintbrush.setColor(Color.argb(255, 255, 0, 0));

            String screenInfo2 = "You Won";
            canvas.drawText(screenInfo2, this.screenWidth/2-100, screenHeight/2-400, paintbrush);
            paintbrush.setTextSize(50);
            String screenInfo3 = "Click to Leave";
            canvas.drawText(screenInfo3, this.screenWidth/2-150, screenHeight/2, paintbrush);

            // --------------------------------
            holder.unlockCanvasAndPost(canvas);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int userAction = event.getActionMasked();

        if (userAction == MotionEvent.ACTION_DOWN) {
            posibleClickX = (int) event.getRawX();
            posibleClickY = (int) event.getRawY();
            //Log.d(TAG, "X: " + posibleClickX + " - Y: " + posibleClickY);
        }
        else if (userAction == MotionEvent.ACTION_UP) {
            if (this.gameStatus == -1){
                this.gameStatus = 0;
                return true;
            }else if(gameStatus == 4 || gameStatus == 5){
                this.gameStatus = -1;
                restartGame();
                return true;
            }
            event.getRawX();
            event.getRawY();

            double a = (int) event.getRawX() - posibleClickX;
            double b = (int) event.getRawY() - posibleClickY;

            double d = Math.sqrt((a * a) + (b * b));


            if(gameStatus == 3 && d <= 20.0){
                //Log.d(TAG, "clock: " + clock);
                //this.bullets.add(new Square(this.getContext(), this.player.getxPosition(), this.player.getyPosition(), 30));
                this.bullets.add(new Sprite(this.getContext(), this.player.getxPosition()+50, this.player.getyPosition(), R.drawable.laser_red02, 20, 50));


                final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.sfx_laser2);
                laser2.start();
                laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer mp) {
                        mp.release();

                    };
                });
            }
        }
        return true;
    }

    public void pauseGame() {
        gameIsRunning = false;
        try {
            gameThread.join();
        }
        catch (InterruptedException e) {

        }
    }

    public void  resumeGame() {
        gameIsRunning = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    public void controlFPS() {
        try {
            gameThread.sleep(17);
        }
        catch (InterruptedException e) {

        }
    }

    //region countDownTimer
    private void startStop() {
        if(timerRunning){
            stopTimer();
        }else{
            startTimer();
        }
    }

    private void stopTimer() {
        countDownTimer.cancel();
        timerRunning = false;
    }

    private void startTimer() {
        countDownTimer = new CountDownTimer(timeLeftInMilliseconds, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMilliseconds = millisUntilFinished;
                updateTimer();
            }

            @Override
            public void onFinish() {

            }
        }.start();
        timerRunning = true;
    }

    private void updateTimer() {
        int minutes = (int) timeLeftInMilliseconds / 60000;
        int seconds = (int) timeLeftInMilliseconds % 60000 / 1000;

        String timeLeftText;
        timeLeftText = "" + minutes;
        timeLeftText += ":";
        if (seconds < 10) {
            timeLeftText += "0";

        }timeLeftText += seconds;
        twomdcooldown = timeLeftText;
        if (minutes == 0 && seconds == 0){
            twomdStatus = 1;
            stopTimer();
            timeLeftInMilliseconds = 60000;
            final MediaPlayer laser2 = MediaPlayer.create(context, R.raw.powerup);
            laser2.start();
            laser2.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                public void onCompletion(MediaPlayer mp) {
                    mp.release();

                };
            });
        }
    }
    //endregion countDownTimer
}
