package mi2u.graphics;

import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.struct.*;
import arc.util.*;
import mindustry.graphics.*;

import static mindustry.Vars.*;

public class TurretZoneDrawer{
    public static IntSet teams = new IntSet();
    public static Interval time = new Interval(256);

    public static void clear(){
        teams.clear();
    }

    public static void applyShader(){
        if(MI2UShaders.turretzone == null) return;
        teams.each(id -> {
            if(time.check(id, Time.delta * 2f)) return;//shut down inactive team shader
            Draw.drawRange(quietGetLayer(id), 0.001f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
                renderer.effectBuffer.end();
                renderer.effectBuffer.blit(MI2UShaders.turretzone);
            });
        });
    }

    /** This method active the shader of this team.*/
    public static float getLayer(int id){
        teams.add(id);
        time.reset(id, 0);
        return quietGetLayer(id);
    }

    public static float quietGetLayer(int id){
        return Layer.plans + 1f + id * 0.003f;
    }
}
