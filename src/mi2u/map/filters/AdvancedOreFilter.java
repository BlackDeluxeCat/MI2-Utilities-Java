package mi2u.map.filters;

import mindustry.content.Blocks;
import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;
import mindustry.world.Block;

import static mindustry.maps.filters.FilterOption.*;
import static mi2u.map.filters.FilterOptions.*;

public class AdvancedOreFilter extends MI2UGenerateFilter{
    public float sclX = 40, sclY = 40, threshold = 0.5f, octaves = 3f, falloff = 0.5f, offX = 0f, offY = 0f, rotate = 0f;
    public Block ore = Blocks.oreCopper, targetFloor = Blocks.air, targetOre = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new SliderOption("scaleX", () -> sclX, f -> sclX = f, 1f, 500f),
                new SliderOption("scaleY", () -> sclY, f -> sclY = f, 1f, 500f),
                new SliderOption("offsetX", () -> offX, f -> offX = f, -200f, 200f),
                new SliderOption("offsetY", () -> offY, f -> offY = f, -200f, 200f),
                new SliderOption("rotation", () -> rotate, f -> rotate = f, 0f, 360f),
                new SliderOption("threshold", () -> threshold, f -> threshold = f, 0f, 1f),
                new SliderOption("octaves", () -> octaves, f -> octaves = f, 1f, 10f),
                new SliderOption("falloff", () -> falloff, f -> falloff = f, 0f, 1f),
                new BlockOption("ore", () -> ore, b -> ore = b, oresOnly),
                new BlockOption("targetFloor", () -> targetFloor, b -> targetFloor = b, floorsOptional),
                new BlockOption("targetOre", () -> targetOre, b -> targetOre = b, oresOptional)
        };
    }

    @Override
    public void apply(GenerateInput in){
        float noise = noise(in, sclX, sclY, offX, offY, rotate, 1f, octaves, falloff);

        if(noise > threshold && in.overlay != Blocks.spawn && in.floor.asFloor().hasSurface()){
            if((targetFloor == Blocks.air || in.floor == targetFloor) && (targetOre == Blocks.air || in.overlay == targetOre))
            in.overlay = ore;
        }
    }
}
