package mi2u.game;

import arc.*;
import arc.math.*;
import arc.util.*;

import static mi2u.MI2UVars.*;

public class SpeedController{
    public static float scl = 1f, rawScl = 1f, reference = 60f, min = 3f;
    public static boolean update = false, auto = true;
    public static float lastAuto = 0f;

    public static void auto(){
        //Log.info(Time.globalTime - lastAuto);
        if(Time.globalTime - lastAuto < 5f) return;
        lastAuto = Time.globalTime;
        rawScl = Core.graphics.getFramesPerSecond() / reference;
        if(Math.abs(rawScl - 1f) < 0.05f) rawScl = 1f;
    }

    public static void update(){
        if(!update) return;
        if(auto) auto();
        scl = Mathf.lerp(scl, rawScl, 0.2f);
        if(Math.abs(rawScl - scl) < 0.01f) scl = rawScl;
        reference = Mathf.clamp(mi2ui.settings.getInt("speedctrl.basefps", 60), 10, 1000);
        min = mi2ui.settings.getInt("speedctrl.cutoff") / reference;
        if(scl < min) scl = min;
    }

    public static boolean lowerThanMin(){
        return rawScl <= min;
    }

    public static void reset(){
        scl = rawScl = 1f;
    }

    public static void switchUpdate(){
        switchUpdate(!update);
    }

    public static void switchUpdate(boolean enable){
        update = enable;
        if(!enable) stop();
        else Time.setDeltaProvider(() -> Core.graphics.getDeltaTime()*reference* scl);
    }

    public static void stop(){
        update = false;
        reset();
        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f, 3f));
    }
}
