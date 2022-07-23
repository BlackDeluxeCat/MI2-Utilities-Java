package mi2u;

import arc.math.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;

import static mindustry.Vars.*;

/** @Author 工业2*/
public class MI2UFuncs{

    public static void unitRebuildBlocks(){
        if(!state.isGame() || !player.unit().canBuild()) return;
        int p = 0;
        for(BlockPlan block : state.teams.get(player.team()).plans){
            if(world.tile(block.x, block.y) != null && world.tile(block.x, block.y).block().id == block.block) state.teams.get(player.team()).plans.remove(block);
            if(Mathf.len(block.x - player.tileX(), block.y - player.tileY()) >= 200) continue;
            p++;
            if(p > 511) break;
            player.unit().addBuild(new BuildPlan(block.x, block.y, block.rotation, content.block(block.block), block.config));
        }
    }

}
