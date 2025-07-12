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
    public int pos = -1;
    public transient @Nullable Building b;
    public transient boolean fetching;

    public BuildingMonitor(){
        super();
    }

    @Override
    public void build(Table table){}

    @Override
    public void buildCfg(Table table){
        table.table(t -> {
            t.defaults().growX();
            t.label(() -> posStr() + (b == null ? "" : (" " + b.block.localizedName)));
            t.button("选点", textbtoggle, () -> fetching = !fetching).checked(b -> fetching).with(funcSetTextb).height(buttonSize);
        }).growX().row();
    }

    @Override
    public void update(){
        if(fetching && !Core.scene.hasMouse() && Core.input.keyTap(Binding.select)){
            fetching = false;
            var tile = world.tileWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY());
            pos = tile == null ? -1 : tile.pos();
        }
        if(pos == -1) return;
        b = world.build(pos);
    }

    public String posStr(){
        return pos == -1 ? "未选中" : (Point2.x(pos) + "," + Point2.y(pos));
    }
}
