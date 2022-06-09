package mi2u.game;

import arc.Core;
import arc.math.Mathf;
import arc.util.Log;
import arc.util.Time;

public class SpeedController{
    public static float mul = 1f, rawMul = 1f, min = 0.05f;
    public static boolean update = false, auto = true;
    public static float lastAuto = 0f;

    public static void auto(){
        //Log.info(Time.globalTime - lastAuto);
        if(Time.globalTime - lastAuto < 30f) return;
        lastAuto = Time.globalTime;
        rawMul = Core.graphics.getFramesPerSecond()/60f;
        if(Math.abs(rawMul - 1f) < 0.05f) rawMul = 1f;
    }

    public static void update(){
        if(!update) return;
        if(auto) auto();
        mul = Mathf.lerp(mul, rawMul, 0.2f);
        if(Math.abs(rawMul - mul) < 0.01f) mul = rawMul;
        if(mul < min) mul = min;
    }

    public static boolean lowerThanMin(){
        return rawMul <= min;
    }

    public static void reset(){
        mul = rawMul = 1f;
    }

    public static void switchUpdate(){
        switchUpdate(!update);
    }

    public static void switchUpdate(boolean enable){
        update = enable;
        if(!enable) stop();
        else Time.setDeltaProvider(() -> Core.graphics.getDeltaTime()*60f*mul);
    }

    public static void stop(){
        update = false;
        reset();
        Time.setDeltaProvider(() -> Math.min(Core.graphics.getDeltaTime() * 60f, 3f));
    }
}
