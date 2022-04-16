package mi2u.map.filters;

import arc.util.Reflect;
import arc.util.noise.Simplex;
import mindustry.content.Blocks;
import mindustry.gen.Iconc;
import mindustry.maps.filters.FilterOption;
import mindustry.maps.filters.GenerateFilter;
import mindustry.world.Block;

import static mi2u.map.filters.FilterOptions.*;

public class AdvancedNoiseFilter extends MI2UGenerateFilter{
    public float sclX = 40, sclY = 40, threshold = 0.5f, octaves = 3f, falloff = 0.5f, offX = 0f, offY = 0f, rotate = 0f;
    public Block floor = Blocks.air, block = Blocks.air, target = Blocks.air, target2 = Blocks.air;

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
                new BlockOption("targetFloor", () -> target, b -> target = b, FilterOption.floorsOptional),
                new BlockOption("floor", () -> floor, b -> floor = b, FilterOption.floorsOptional),
                new BlockOption("targetWall", () -> target2, b -> target2 = b, FilterOption.wallsOptional),
                new BlockOption("wall", () -> block, b -> block = b, FilterOption.wallsOptional)
        };
    }

    @Override
    public void apply(GenerateFilter.GenerateInput in){
        float noise = noise(in, sclX, sclY, offX, offY, rotate, 1f, octaves, falloff);

        if(noise > threshold){
            if(floor != Blocks.air && in.floor == target) in.floor = floor;
            if(block != Blocks.air && in.block == target2) in.block = block;
        }
    }
}
