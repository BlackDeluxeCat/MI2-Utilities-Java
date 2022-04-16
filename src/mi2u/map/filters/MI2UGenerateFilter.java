package mi2u.map.filters;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.util.noise.Simplex;
import mi2u.MI2UTmp;
import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;
import mindustry.maps.filters.GenerateFilter;

public class MI2UGenerateFilter extends GenerateFilter{
    @Override
    public FilterOption[] options() {
        return new FilterOption[0];
    }

    @Override
    public char icon(){
        return Iconc.blockLogicDisplay;
    }

    public float noise(GenerateInput in, float sclX, float sclY, float offX, float offY, float rotate, float mag, float octaves, float persistence){
        Vec2 vec = MI2UTmp.v1;
        vec.set(in.x, in.y).rotate(rotate);
        return Simplex.noise2d(seed, octaves, persistence, 1f, (vec.x + offX) / sclX, (vec.y + offY) / sclY) * mag;
    }
}
