package mi2u.map.filters;

import arc.math.geom.Vec2;
import mindustry.gen.Iconc;

public class PolarTransFilter extends MI2UGenerateFilter{
    public static Vec2 tmp = new Vec2();

    @Override
    public char icon(){
        return Iconc.add;
    }

    @Override
    public void apply(GenerateInput in){
        if(in.x == 0 && in.y == 0){
            if(transConsumer != null){
                transeq.clear();
                transConsumer = null;
            }
            transeq.addLast(vec2 -> {
                tmp.set(vec2);
                vec2.trns(tmp.x, tmp.y);
            });
        }
    }
}
