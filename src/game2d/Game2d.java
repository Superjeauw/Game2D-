package game2d;

import game2d.charmechanics.Player;
import game2d.charmechanics.CharStats;
import game2d.charmechanics.GameItems;
import game2d.charmechanics.Monster;
import game2d.keyboardinteraction.Listener;
import game2d.shapes.Rectangle;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.util.Duration;

import java.util.ArrayList;

@SuppressWarnings("restriction")
public class Game2d {
	private Canvas game_window;
	private Player Actor;
	private SharedDataLists SDL;
	private Settings config;
	private final double fps = 30;
	//private final double fps = 60;
	private final int WINDOW_WIDTH, WINDOW_HEIGHT;

	public Scene scene;
	private Group root;
	private GraphicsContext g;
	private Timeline gameLoop;

	/**Create the application and run it.*/
	public Game2d() {

		config = new Settings("config.properties");
		/*WINDOW_WIDTH example: Picks value from the properties file, if no such value 
		exists "800" is taken as default instead.*/
		WINDOW_WIDTH = Integer.parseInt(config.get("width", "800"));
		WINDOW_HEIGHT = Integer.parseInt(config.get("height", "600"));
		SDL = new SharedDataLists(config, new GameItems());
		SDL.initialize_monsters();
		Actor = new Player(SDL, new CharStats("Playa", 100, 30, 0, 1, true));
		SDL.set_actor_once(Actor);

		root = new Group();
		scene = new Scene(root);

		final Duration frameDuration = Duration.millis(1000/fps);
		final KeyFrame keyFrame = new KeyFrame(frameDuration,
				event -> tick());

		gameLoop = new Timeline(fps, keyFrame);
		gameLoop.setCycleCount(Animation.INDEFINITE);

		// give access to this object from Listener
		Listener.controller = this;

		// keybindings
		Listener.keymap.put(KeyCode.P, () -> Listener.controller.pause());
		// TODO: Fix so muting works in GameWindow/GameMenu not only in Game2d..
		Listener.keymap.put(KeyCode.O, () -> Listener.controller.muteSound());
		Listener.keymap.put(KeyCode.ESCAPE, () -> Listener.controller.exitGame());
		Listener.keymap.put(KeyCode.DIGIT1, () -> Listener.controller.Actor.charStats.equipWeapon('1'));
		Listener.keymap.put(KeyCode.DIGIT2, () -> Listener.controller.Actor.charStats.equipWeapon('2'));
		Listener.keymap.put(KeyCode.DIGIT3, () -> Listener.controller.Actor.charStats.equipWeapon('3'));
		Listener.keymap.put(KeyCode.DIGIT4, () -> Listener.controller.Actor.charStats.equipWeapon('4'));
		Listener.keymap.put(KeyCode.PLUS, () -> SoundController.incrementVolume());
		Listener.keymap.put(KeyCode.MINUS, () -> SoundController.decrementVolume());

		game_window = new Canvas(WINDOW_WIDTH, WINDOW_HEIGHT);
		root.getChildren().add(game_window);
		scene.addEventHandler(KeyEvent.KEY_PRESSED,
				event -> Listener.keyPressed(event));
		scene.addEventHandler(KeyEvent.KEY_RELEASED,
				event -> Listener.keyReleased(event));

		g = game_window.getGraphicsContext2D();

		gameLoop.play();
	}

	private void muteSound() {
		SoundController.mutePlaying();
	}

	private void exitGame() {
         System.exit(0);
	}
	
	private void pause() {
		Listener.keymap.put(KeyCode.P,
				() -> Listener.controller.unpause());
		gameLoop.pause();
	}

	private void unpause() {
		Listener.keymap.put(KeyCode.P,
				() -> Listener.controller.pause());
		gameLoop.play();
	}

	private void reset() {
		//System.out.println("reset");
		g.setFill(Color.WHITE);
		g.fillRect(0, 0, game_window.getWidth(), game_window.getHeight());
	}

	private final int portal_resize = 5;
	
	private void update() {
		for(PaintRectNode PRN : SDL.map_list[SDL.map_index].toPaint){
			switch(PRN.type){
			case 'G':
				g.setFill(Color.GREEN);
				g.fillRect(PRN.rect.x - Actor.xCoord(),
						PRN.rect.y - Actor.yCoord(), PRN.rect.width,
						PRN.rect.height);
				break;
			case 'P':
				g.setFill(Color.YELLOW);
				g.fillOval(PRN.rect.x - Actor.xCoord() - portal_resize,
						PRN.rect.y - Actor.yCoord() - portal_resize,
						PRN.rect.width + (portal_resize*2),
						PRN.rect.height + (portal_resize*2));
				break;
			}
		}
		//
		//painting characters/creatures(not actor)
		g.setFill(Color.MAGENTA);//why's purple called "magneta", who knows..
		for(Monster MOB : SDL.map_list[SDL.map_index].mobs_in_map){
			Rectangle R = MOB.shape;
			g.fillRect(R.x - Actor.xCoord(), R.y - Actor.yCoord(),
					R.width, R.height);
		}
		//
		//painting main character
		if(Actor.charStats.isImmune()) g.setFill(Color.RED);
		else g.setFill(Color.BLACK);
		g.fillRect(Actor.shape.x - Actor.xCoord(),
				Actor.shape.y - Actor.yCoord(), Actor.width, Actor.height);
		//
		//weapon related animations:
		g.setFill(Color.BLUE);
		if(Actor.charStats.getWeapon().getDegreeA() != 0)
			g.setFill(Color.YELLOW);//testing weapon switch
		ArrayList<Integer> seq;
		for (int i = 0; i < SDL.image_sequences.size(); i++) {
			seq = SDL.image_sequences.get(i);
			
			if(Actor.getFacing() == 'd'){//right
				g.fillRect(Actor.shape.x - Actor.xCoord() + Actor.width,
						Actor.shape.y - Actor.yCoord() + Actor.height/2,
						seq.remove(0), 2);

			}else{//left
				int img = seq.remove(0);
				g.fillRect(Actor.shape.x - Actor.xCoord() - img,
						Actor.shape.y - Actor.yCoord() + Actor.height/2,
						img, 2);
			}

			if(seq.isEmpty())
				SDL.image_sequences.remove(i);
		}
	}

	/* Method called once every frame. Performance! */
	private void tick() {
		//Actor actions:
		Actor.movement(Listener.get_moveKey());
		Actor.gravity(Listener.get_otherKey());
		Actor.actions(Listener.get_otherKey());
		Actor.buffDurations();
		//System.out.println("x coords: " + Actor.x_coord);
		//System.out.println("shape x: " + Actor.shape.x);
		//
		//NPCs actions:
		ArrayList<Monster> mobs = SDL.map_list[SDL.map_index].mobs_in_map;
		for (int i = 0; i < mobs.size(); i++) {
			if (mobs.get(i).charStats.isAlive()) {
				mobs.get(i).AI_movement();
				mobs.get(i).AI_gravity();
			} else {
				mobs.remove(i);
			}
		}
		reset();
		update();
	}
}
