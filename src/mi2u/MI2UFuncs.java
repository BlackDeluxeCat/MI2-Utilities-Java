package mi2u;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

public class MI2UFuncs{
    public static void unitRebuildBlocks(){
        if(!state.isGame() || player.unit() == null || !player.unit().canBuild()) return;
        int p = 0;
        for(BlockPlan plan : state.teams.get(player.team()).plans){
            if(world.tile(plan.x, plan.y) != null && world.tile(plan.x, plan.y).block().id == plan.block.id) state.teams.get(player.team()).plans.remove(plan);
            if(Mathf.len(plan.x - player.tileX(), plan.y - player.tileY()) >= 200) continue;
            if(p++ > 511) break;
            player.unit().addBuild(new BuildPlan(plan.x, plan.y, plan.rotation, plan.block, plan.config));
        }
    }

    public static void cleanGhostBlock(){
        if(!state.isGame()) return;
        Call.deletePlans(player, Seq.with(state.teams.get(player.team()).plans).mapInt(plan -> Point2.pack(plan.x, plan.y)).toArray());
    }
}
