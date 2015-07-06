import javafx.scene.media.AudioClip;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by aperte on 06.07.2015.
 */
public class SoundController {
    // TODO: Add sfxvolume, bgvolume, voicevolume
    double volume;
    // TODO: Need better volume structure (messy down there)
    boolean muted;
    HashMap<String, PlayableSound> soundMap = new HashMap<>();
    ArrayList<SoundPlayer> playing = new ArrayList<>();

    SoundController() {
        // TODO: Get volume from config?
        volume = 0.05;
        // TODO: Get sounds from config?
        newSound("bgm", "Cold_Silence.mp3");
        newSoundEffect("beep", "beep.wav");
    }

    void newSoundEffect(String id, String uri) {
        // TODO: Error handling
        putSound(id, new SoundEffect(getClass().getResource(uri).toExternalForm(), volume, this));
    }

    void newSound(String id, String uri) {
        // TODO: Error handling
        putSound(id, new SoundPlayer(getClass().getResource(uri).toExternalForm(), volume, this));
    }

    PlayableSound getSound(String id) {
        // TODO: Error handling
        return soundMap.get(id);
    }

    PlayableSound putSound(String id, PlayableSound sound) {
        // TODO: Error handling
        return soundMap.put(id, sound);
    }

    void playSound(String id) {
        PlayableSound sound = getSound(id);
        sound.play();
        if (sound instanceof SoundPlayer) {
            playing.add((SoundPlayer)sound);
        }
    }

    void mutePlaying() {
        if (muted) {
            for (SoundPlayer sp: playing) {
                sp.mediaPlayer.setVolume(volume);
                muted = false;
            }
        } else {
            for (SoundPlayer sp: playing) {
                sp.mediaPlayer.setVolume(0);
                muted = true;
            }
        }
    }

    void setVolume(Double volume) {
        this.volume = volume;
    }
}

/* Class for playing AudioClips, ment for short sounds with no control - fire and forget. */
class SoundEffect extends PlayableSound {
    AudioClip clip;

    SoundEffect(String uri, double volume, SoundController master) {
        super(master);
        this.clip = new AudioClip(uri);
    }

    void play() {
        if (master.muted) return;
        clip.setVolume(master.volume);
        new Thread(() -> clip.play()).start();
    }

}

/* Class for playing Media sound, ment for longer sounds with control possible. */
class SoundPlayer extends PlayableSound {
    Media media;
    MediaPlayer mediaPlayer;
    Thread th;

    SoundPlayer(String uri, double volume, SoundController master) {
        super(master);
        this.volume = volume;
        this.media = new Media(uri);
    }

    void play() {
        th = new Thread(new Runnable() {
            public void run() {
                mediaPlayer = new MediaPlayer(media);
                mediaPlayer.setVolume(volume);
                mediaPlayer.setOnReady(() -> mediaPlayer.play());
            }
        });
        th.start();
    }
}

/* Abstract class for gathering the playable sounds under one polymorphism */
abstract class PlayableSound {
    SoundController master;
    double volume; // implementation choose themselves how to handle volume

    abstract void play();

    PlayableSound(SoundController master) {
        this.master = master;
    }
}
