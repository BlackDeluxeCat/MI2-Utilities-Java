package mi2u.map.filters;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.geom.Vec2;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Reflect;
import arc.util.Scaling;
import mi2u.MI2UTmp;
import mindustry.content.Blocks;
import mindustry.graphics.Pal;
import mindustry.maps.filters.FilterOption;
import mindustry.maps.filters.GenerateFilter;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.environment.Prop;
import mindustry.world.blocks.environment.StaticWall;
import mindustry.world.blocks.environment.TreeBlock;

public class CopyPasteFilter extends MI2UGenerateFilter{
    private static TileBuffer[][] buffers;

    private transient int worldWidth = 1, worldHeight = 1;
    public float fromX = 0, fromY = 0, width = 32, height = 32, toX = 64, toY = 64;
    public boolean copyBlock = false, copyOverlay = false;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
            new FilterOptions.SliderOption("fromX", () -> fromX, f -> fromX = f, 0, 500f, 1),
            new FilterOptions.SliderOption("fromY", () -> fromY, f -> fromY = f, 0, 500f, 1),
            new FilterOptions.SliderOption("width", () -> width, f -> width = f, 1f, 500f, 1),
            new FilterOptions.SliderOption("height", () -> height, f -> height = f, 1f, 500f, 1),
            new FilterOptions.SliderOption("toX", () -> toX, f -> toX = f, 0f, 500f, 1),
            new FilterOptions.SliderOption("toY", () -> toY, f -> toY = f, 0f, 500f, 1),
            new FilterOptions.ToggleOption("targetWall", () -> copyBlock, f -> copyBlock = f),
            new FilterOptions.ToggleOption("targetOre", () -> copyOverlay, f -> copyOverlay = f)
        };
    }

    @Override
    public void apply(GenerateInput in){
        //copy to buffers before first tile is processed
        if(in.x == 0 && in.y == 0) copyToBuffers(in);
        if(buffers == null) return;
        preConsume(in);
        if(regionConsumer == this && regionseq.count(r -> r.contains(in.x, in.y)) <= 0) return;
        int bufx = (int)(in.x - toX);
        int bufy = (int)(in.y - toY);
        if(bufx < 0 || bufx >= buffers.length || bufy < 0 || bufy >= buffers[0].length) return;
        TileBuffer buffer = buffers[bufx][bufy];
        if(buffer == null) return;
        in.floor = buffer.floor;
        if(copyBlock) in.block = buffer.wall;
        if(copyOverlay) in.overlay = buffer.overlay;
    }

    @Override
    public void draw(Image image) {
        super.draw(image);
        Vec2 vsize = Scaling.fit.apply(image.getDrawable().getMinWidth(), image.getDrawable().getMinHeight(), image.getWidth(), image.getHeight());

        Lines.stroke(Scl.scl(2f), Pal.accent);
        Lines.rect(fromX / worldWidth * vsize.x + image.x + (image.getWidth() - vsize.x)/2f, fromY / worldHeight * vsize.y + image.y + (image.getHeight() - vsize.y)/2f, width / worldWidth * vsize.x, height / worldHeight * vsize.y);
        Lines.stroke(Scl.scl(2f), Pal.items);
        Lines.rect(toX / worldWidth * vsize.x + image.x + (image.getWidth() - vsize.x)/2f, toY / worldHeight * vsize.y + image.y + (image.getHeight() - vsize.y)/2f, width / worldWidth * vsize.x, height / worldHeight * vsize.y);
        Draw.reset();
    }

    public void copyToBuffers(GenerateInput in){
        buffers = new TileBuffer[(int)width][(int)height];
        worldWidth = in.width;
        worldHeight = in.height;
        GenerateInput.TileProvider provider = Reflect.get(in, "buffer");
        Tile tile;
        for(int x = 0; x < width; x++){
            for(int y = 0; y < height; y++){
                if(x + fromX > in.width - 1 || y + fromY > in.height - 1) continue;
                tile = provider.get(x + (int)fromX, y + (int)fromY);
                buffers[x][y] = new TileBuffer(tile);
            }
        }
    }

    public static class TileBuffer{
        public Block wall, floor, overlay;

        public TileBuffer(Tile tile){
            this.floor = tile.floor();
            this.overlay = tile.overlay();
            this.wall = tile.block() instanceof Prop || tile.block() instanceof TreeBlock ? tile.block() : Blocks.air;
        }
    }
}
