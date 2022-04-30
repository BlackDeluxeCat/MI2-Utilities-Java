package mi2u.map.filters;

import arc.math.Mathf;
import arc.math.geom.Vec2;
import mi2u.MI2UTmp;
import mindustry.content.Blocks;
import mindustry.maps.filters.FilterOption;
import mindustry.maps.filters.GenerateFilter;
import mindustry.world.Block;

import static mindustry.maps.filters.FilterOption.*;
import static mi2u.map.filters.FilterOptions.*;

public class GridFilter extends MI2UGenerateFilter{
    public float width = 40, height = 40, stroke = 3f, offX = 0f, offY = 0f;
    public Block floor = Blocks.air, block = Blocks.air, target = Blocks.air, target2 = Blocks.air;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new SliderOption("width", () -> width, f -> width = f, 1f, 500f),
                new SliderOption("height", () -> height, f -> height = f, 1f, 500f),
                new SliderOption("offsetX", () -> offX, f -> offX = f, -200f, 200f),
                new SliderOption("offsetY", () -> offY, f -> offY = f, -200f, 200f),
                new SliderOption("stroke", () -> stroke, f -> stroke = f, 1f, 100f),
                new BlockOption("targetFloor", () -> target, b -> target = b, floorsOptional),
                new BlockOption("floor", () -> floor, b -> floor = b, floorsOptional),
                new BlockOption("targetWall", () -> target2, b -> target2 = b, wallsOptional),
                new BlockOption("wall", () -> block, b -> block = b, wallsOptional)
        };
    }

    @Override
    public void apply(GenerateInput in){
        preConsume(in);
        if(regionConsumer == this && regionseq.count(r -> r.contains(in.x, in.y)) <= 0) return;
        var v = MI2UTmp.v3.set(in.x, in.y);
        if(transConsumer == this) transeq.each(c -> c.get(v));
        if(Mathf.mod(v.x + offX, width+stroke) < stroke || Mathf.mod(v.y + offY, height+stroke) < stroke){
            if(floor != Blocks.air && (target == Blocks.air || in.floor == target)) in.floor = floor;
            if(block != Blocks.air && in.block == target2) in.block = block;
        }
    }
}
