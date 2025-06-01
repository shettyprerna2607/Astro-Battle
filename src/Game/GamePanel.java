package Game;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.net.URL;
import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

public class GamePanel extends JPanel implements ActionListener {

    // Spaceship position and size
    private int spaceshipX = 370;
    private final int SPACESHIP_WIDTH = 60;
    private final int SPACESHIP_HEIGHT = 40;
    private final int MOVE_DISTANCE = 10;

    // Bullet info
    private int bulletX;
    private int bulletY;
    private boolean bulletFired = false;

    // Enemy data
    private class Enemy {
        int x, y;
        boolean alive = true;
        int direction = new Random().nextBoolean() ? 1 : -1;
        int speed = 1 + new Random().nextInt(3);
    }
    private ArrayList<Enemy> enemies = new ArrayList<>();
    private int enemyDrop = 20;
    private int enemyMoveCounter = 0;
    private final int ENEMY_MOVE_INTERVAL = 20;

    // Explosion effect
    private class Explosion {
        int x, y;
        int frame = 0;
    }
    private ArrayList<Explosion> explosions = new ArrayList<>();

    private int score = 0;
    private boolean gameOver = false;
    private boolean gameWon = false;

    private Timer timer;

    // Game images
    private Image alienImage;
    private Image explosionImage;
    private Image spaceshipImage;
    private Image shootImage;

    // Sounds
    private Clip shootSound, explosionSound;

    // Star background
    private class Star {
        int x, y, size, speed;
        Star(int x, int y, int size, int speed) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
        }
    }
    private ArrayList<Star> stars = new ArrayList<>();
    private final int STAR_COUNT = 100;
    private Random rand = new Random();

    public GamePanel() {
        setPreferredSize(new Dimension(800, 600));
        setFocusable(true);
        requestFocusInWindow();

        loadResources();
        initEnemies();
        initStars();

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                // Restart on 'R' if game ended
                if ((gameOver || gameWon) && e.getKeyCode() == KeyEvent.VK_R) {
                    restartGame();
                    return;
                }

                if (gameOver || gameWon) return;

                switch (e.getKeyCode()) {
                    case KeyEvent.VK_LEFT:
                        spaceshipX = Math.max(spaceshipX - MOVE_DISTANCE, 0);
                        break;
                    case KeyEvent.VK_RIGHT:
                        spaceshipX = Math.min(spaceshipX + MOVE_DISTANCE, getWidth() - SPACESHIP_WIDTH);
                        break;
                    case KeyEvent.VK_SPACE:
                        if (!bulletFired) {
                            bulletFired = true;
                            bulletX = spaceshipX + SPACESHIP_WIDTH / 2 - 5;
                            bulletY = 520;
                            playSound(shootSound);
                        }
                        break;
                }
            }
        });

        timer = new Timer(20, this);
        timer.start();
    }

    // Create stars for background
    private void initStars() {
        stars.clear();
        for (int i = 0; i < STAR_COUNT; i++) {
            int x = rand.nextInt(800);
            int y = rand.nextInt(600);
            int size = 1 + rand.nextInt(3);
            int speed = 1 + rand.nextInt(3);
            stars.add(new Star(x, y, size, speed));
        }
    }

    // Load images and sounds
    private void loadResources() {
        alienImage = loadImage("alien.png").getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        explosionImage = loadImage("explosion.png").getScaledInstance(40, 40, Image.SCALE_SMOOTH);
        spaceshipImage = loadImage("spaceship.png").getScaledInstance(SPACESHIP_WIDTH, SPACESHIP_HEIGHT, Image.SCALE_SMOOTH);
        shootImage = loadImage("shoot.png").getScaledInstance(10, 20, Image.SCALE_SMOOTH);

        shootSound = loadSound("shoot.wav");
        explosionSound = loadSound("explosion.wav");
    }

    // Load image helper
    private Image loadImage(String filename) {
        URL url = getClass().getResource("/" + filename);
        return (url != null) ? new ImageIcon(url).getImage() : null;
    }

    // Load sound helper
    private Clip loadSound(String filename) {
        try {
            URL url = getClass().getResource("/" + filename);
            if (url == null) return null;
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            Clip clip = AudioSystem.getClip();
            clip.open(audioIn);
            return clip;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Play a sound clip
    private void playSound(Clip clip) {
        if (clip != null) {
            if (clip.isRunning()) clip.stop();
            clip.setFramePosition(0);
            clip.start();
        }
    }

    // Setup enemies grid
    private void initEnemies() {
        enemies.clear();
        int startX = 100, startY = 50, gapX = 60, gapY = 50;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                Enemy enemy = new Enemy();
                enemy.x = startX + col * gapX;
                enemy.y = startY + row * gapY;
                enemies.add(enemy);
            }
        }
    }

    // Game loop update
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameOver || gameWon) return;

        // Move bullet up
        if (bulletFired) {
            bulletY -= 7;
            if (bulletY < 0) bulletFired = false;
        }

        // Move enemies on interval
        enemyMoveCounter++;
        if (enemyMoveCounter >= ENEMY_MOVE_INTERVAL) {
            enemyMoveCounter = 0;
            for (Enemy enemy : enemies) {
                if (!enemy.alive) continue;
                enemy.x += enemy.direction * enemy.speed;
                if (enemy.x < 0 || enemy.x > getWidth() - 40) {
                    enemy.direction *= -1;
                    enemy.y += enemyDrop;
                    enemy.x += enemy.direction * enemy.speed;
                    if (enemy.y + 40 >= 520) gameOver = true;
                }
            }
        }

        // Check bullet hits
        if (bulletFired) {
            for (Enemy enemy : enemies) {
                if (enemy.alive &&
                        bulletX + 10 >= enemy.x && bulletX <= enemy.x + 40 &&
                        bulletY <= enemy.y + 40 && bulletY + 20 >= enemy.y) {
                    enemy.alive = false;
                    bulletFired = false;
                    score += 10;

                    Explosion exp = new Explosion();
                    exp.x = enemy.x;
                    exp.y = enemy.y;
                    explosions.add(exp);

                    playSound(explosionSound);
                    break;
                }
            }
        }

        // Update explosions and remove done ones
        Iterator<Explosion> expIterator = explosions.iterator();
        while (expIterator.hasNext()) {
            Explosion exp = expIterator.next();
            exp.frame++;
            if (exp.frame > 10) expIterator.remove();
        }

        // Move stars down and reset
        for (Star star : stars) {
            star.y += star.speed;
            if (star.y > getHeight()) {
                star.y = 0;
                star.x = rand.nextInt(getWidth());
                star.size = 1 + rand.nextInt(3);
                star.speed = 1 + rand.nextInt(3);
            }
        }

        // Check if all enemies are dead
        boolean allDead = true;
        for (Enemy enemy : enemies) {
            if (enemy.alive) {
                allDead = false;
                break;
            }
        }
        if (allDead) gameWon = true;

        repaint();
    }

    // Draw game screen
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // Background
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, getWidth(), getHeight());

        // Draw stars
        g.setColor(Color.WHITE);
        for (Star star : stars) {
            g.fillOval(star.x, star.y, star.size, star.size);
        }

        // Draw spaceship
        g.drawImage(spaceshipImage, spaceshipX, 520, this);

        // Draw bullet
        if (bulletFired && shootImage != null) {
            g.drawImage(shootImage, bulletX, bulletY, this);
        }

        // Draw enemies
        for (Enemy enemy : enemies) {
            if (!enemy.alive) continue;
            g.drawImage(alienImage, enemy.x, enemy.y, this);
        }

        // Draw explosions
        for (Explosion exp : explosions) {
            g.drawImage(explosionImage, exp.x, exp.y, this);
        }

        // Draw score
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 20));
        g.drawString("Score: " + score, 10, 20);

        // Game over or win message
        if (gameOver) {
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.setColor(Color.RED);
            g.drawString("GAME OVER", getWidth() / 2 - 120, getHeight() / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Press 'R' to Restart", getWidth() / 2 - 90, getHeight() / 2 + 40);
        } else if (gameWon) {
            g.setFont(new Font("Arial", Font.BOLD, 36));
            g.setColor(Color.GREEN);
            g.drawString("YOU WIN!", getWidth() / 2 - 100, getHeight() / 2);
            g.setFont(new Font("Arial", Font.PLAIN, 18));
            g.drawString("Press 'R' to Restart", getWidth() / 2 - 90, getHeight() / 2 + 40);
        }
    }

    // Restart the game
    private void restartGame() {
        spaceshipX = 370;
        bulletFired = false;
        score = 0;
        gameOver = false;
        gameWon = false;
        explosions.clear();
        initEnemies();
        initStars();
        repaint();
    }
}

