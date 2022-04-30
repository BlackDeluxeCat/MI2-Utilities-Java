package mi2u.map.filters;

import arc.func.Cons;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.struct.Queue;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.noise.Simplex;
import mi2u.MI2UTmp;
import mi2u.MI2Utilities;
import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;
import mindustry.maps.filters.GenerateFilter;

public class MI2UGenerateFilter extends GenerateFilter{
    //transformation stack
    public static Queue<Cons<Vec2>> transeq = new Queue<>();
    //transformation consumer
    @Nullable public static MI2UGenerateFilter transConsumer = null;
    //region stack
    public static Seq<Rect> regionseq = new Seq<>();
    //region consumer
    @Nullable public static MI2UGenerateFilter regionConsumer = null;

    @Override
    public FilterOption[] options() {
        return new FilterOption[0];
    }

    @Override
    public char icon(){
        return Iconc.blockLogicDisplay;
    }

    public void preConsume(GenerateInput in){
        if(in.x == 0 && in.y == 0){
            if(transConsumer == null && !transeq.isEmpty()) {
                transConsumer = this;
            }
            if(regionConsumer == null && !regionseq.isEmpty()) {
                regionConsumer = this;
            }
        }
    }

    public float noise(GenerateInput in, float sclX, float sclY, float offX, float offY, float rotate, float mag, float octaves, float persistence){
        Vec2 vec = MI2UTmp.v1;
        vec.set(in.x, in.y).rotate(rotate);
        return Simplex.noise2d(seed, octaves, persistence, 1f, (vec.x + offX) / sclX, (vec.y + offY) / sclY) * mag;
    }

    public float noise(float x, float y, float mag, float octaves, float persistence){
        return Simplex.noise2d(seed, octaves, persistence, 1f, x, y) * mag;
    }
}
