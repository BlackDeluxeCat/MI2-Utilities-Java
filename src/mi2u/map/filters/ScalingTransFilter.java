package mi2u.map.filters;

import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;

public class ScalingTransFilter extends MI2UGenerateFilter{
    public float sclX = 1f, sclY = 1f;
    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new FilterOptions.SliderOption("scaleX", () -> sclX, f -> sclX = f, 0.5f, 20f),
                new FilterOptions.SliderOption("scaleY", () -> sclY, f -> sclY = f, 0.5f, 20f)
        };
    }

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
            transeq.addLast(vec2 -> vec2.scl(1f/sclX, 1f/sclY));
        }
    }
}
