package mi2u.ui.stats;

import arc.*;
import arc.math.geom.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.gen.*;
import mindustry.input.*;

import static mi2u.MI2UVars.*;
import static mindustry.Vars.world;

public abstract class BuildingMonitor extends Monitor{
    public int source = -1;
    public transient @Nullable Building b;
    public transient boolean fetching;

    public BuildingMonitor(){
        super();
    }

    @Override
    public void buildFetch(Table table){
        table.clear();
        table.button(b -> b.label(this::pos), textbtoggle, () -> fetching = !fetching).growX().update(t -> {
            if(fetching && !Core.scene.hasMouse() && Core.input.keyRelease(Binding.select)){
                var tile = world.tileWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
                source = tile == null ? -1 : tile.pos();
                b = world.build(source);
                fetching = false;
            }
            t.setChecked(fetching);
        }).grow();
    }

    @Override
    public void validate(){
        b = world.build(source);
    }

    @Override
    public String title(){
        return "" + (source == -1 ? Iconc.none : b == null ? Iconc.blockEmpty : b.block.localizedName);
    }

    public String pos(){
        return source == -1 ? "未选中" : (Point2.x(source) + "," + Point2.y(source));
    }
}
