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
            if(time.check(id, 10f)) return;
            Draw.drawRange(getLayer(id), 0.001f, () -> renderer.effectBuffer.begin(Color.clear), () -> {
                renderer.effectBuffer.end();
                renderer.effectBuffer.blit(MI2UShaders.turretzone);
            });
        });
    }

    public static float getLayer(int id){
        teams.add(id);
        time.reset(id, 1f);
        return Layer.plans + 1f + id * 0.003f;
    }
}
