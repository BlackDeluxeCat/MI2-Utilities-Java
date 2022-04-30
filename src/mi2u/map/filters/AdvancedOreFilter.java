package mi2u.map.filters;

import mi2u.MI2UTmp;
import mindustry.content.Blocks;
import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;
import mindustry.world.Block;

import static mindustry.maps.filters.FilterOption.*;
import static mi2u.map.filters.FilterOptions.*;

public class AdvancedOreFilter extends MI2UGenerateFilter{
    public float threshold = 0.5f, octaves = 3f, falloff = 0.5f;
    public Block ore = Blocks.oreCopper, targetFloor = Blocks.air, targetOre = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
                new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
                new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
                new BlockOption("ore", () -> ore, b -> ore = b, oresOnly),
                new BlockOption("targetFloor", () -> targetFloor, b -> targetFloor = b, floorsOptional),
                new BlockOption("targetOre", () -> targetOre, b -> targetOre = b, oresOptional)
        };
    }

    @Override
    public char icon(){
        return Iconc.blockLogicDisplay;
    }

    @Override
    public void apply(GenerateInput in){
        preConsume(in);
        if(regionConsumer == this && regionseq.count(r -> r.contains(in.x, in.y)) <= 0) return;
        var v = MI2UTmp.v3.set(in.x, in.y);
        if(transConsumer == this) transeq.each(c -> c.get(v));
        float noise = noise(v.x, v.y, 1f, octaves, falloff);

        if(noise > threshold && in.overlay != Blocks.spawn && in.floor.asFloor().hasSurface()){
            if((targetFloor == Blocks.air || in.floor == targetFloor) && (targetOre == Blocks.air || in.overlay == targetOre))
            in.overlay = ore;
        }
    }
}
