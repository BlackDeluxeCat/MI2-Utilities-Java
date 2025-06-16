package mi2u.game;

import arc.*;
import arc.math.*;
import arc.util.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.maxDeltaClient;

public class FpsController{
    public static float scl = 1f, ratio = 1f, tgtFps = 120f, min = 3f;
    public static boolean update = false;
    public static float lastAuto = 0f;

    public static void update(){
        if(!update) return;
        if(!(Time.globalTime - lastAuto < 5f)){
            lastAuto = Time.globalTime;
            ratio = Core.graphics.getFramesPerSecond() / tgtFps;
            if(Mathf.zero(ratio - 1f, 0.05f)) ratio = 1f;
            scl = Mathf.lerp(scl, ratio, 0.4f);
            if(Mathf.zero(ratio - scl, 0.01f)) scl = ratio;
            tgtFps = Core.settings.getInt("fpscap", 120);
            min = mi2ui.settings.getInt("speedctrl.cutoff") / tgtFps;
            scl = Math.max(scl, min);
        }
    }

    public static void reset(){
        scl = ratio = 1f;
    }

    public static void toggle(){
        update = !update;
        if(update){
            Time.setDeltaProvider(() -> Core.graphics.getDeltaTime() * Math.min(tgtFps, 60) * scl);
        }else{
            reset();
            Time.setDeltaProvider(() -> {
                float result = Core.graphics.getDeltaTime() * 60f;
                return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, maxDeltaClient);
            });
        }
    }
}
