package mi2u.map.filters;

import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Scl;
import arc.util.Scaling;
import mindustry.gen.Iconc;
import mindustry.graphics.Pal;
import mindustry.maps.filters.FilterOption;

public class AppliedRegionFilter extends MI2UGenerateFilter{
    private transient int worldWidth = 1, worldHeight = 1;
    public int fromX = 0, fromY = 0, width = 32, height = 32;

    @Override
    public FilterOption[] options(){
        return new FilterOption[]{
                new FilterOptions.SliderOption("fromX", () -> fromX, f -> fromX = (int) f, 0, 500f, 1),
                new FilterOptions.SliderOption("fromY", () -> fromY, f -> fromY = (int) f, 0, 500f, 1),
                new FilterOptions.SliderOption("width", () -> width, f -> width = (int) f, 1f, 500f, 1),
                new FilterOptions.SliderOption("height", () -> height, f -> height = (int) f, 1f, 500f, 1)
        };
    }

    @Override
    public char icon(){
        return Iconc.cancel;
    }

    @Override
    public void draw(Image image) {
        super.draw(image);
        Vec2 vsize = Scaling.fit.apply(image.getDrawable().getMinWidth(), image.getDrawable().getMinHeight(), image.getWidth(), image.getHeight());

        Lines.stroke(Scl.scl(2f), Pal.remove);
        Lines.rect(fromX / (float)worldWidth * vsize.x + image.x + (image.getWidth() - vsize.x)/2f, fromY / (float)worldHeight * vsize.y + image.y + (image.getHeight() - vsize.y)/2f, width / (float)worldWidth * vsize.x, height / (float)worldHeight * vsize.y);
        Draw.reset();
    }

    @Override
    public void apply(GenerateInput in){
        if(in.x == 0 && in.y == 0){
            worldWidth = in.width;
            worldHeight = in.height;
            if(regionConsumer != null){
                regionseq.clear();
                regionConsumer = null;
            }
            regionseq.add(new Rect(fromX, fromY, width, height));
        }
    }
}