package mi2u.map.filters;

import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;

public class RotateTransFilter extends MI2UGenerateFilter{
    public float rotate = 0f;
    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new FilterOptions.SliderOption("rotation", () -> rotate, f -> rotate = f, 0f, 360f)
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
            transeq.addLast(vec2 -> vec2.rotate(rotate));
        }
    }
}
