package mi2u;

import arc.math.*;
import arc.math.geom.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;

import static mindustry.Vars.*;

/** @Author 工业2*/
public class MI2UFuncs{
    public static Vec2 tmp1 = new Vec2(), tmp2 = new Vec2(), tmp3 = new Vec2(), tmp4 = new Vec2();
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

    //判断两条线段是否有交点，如果有，交点坐标输出到out
    //Author: BlackDeluxeCat
    public static boolean intersection(Vec2 p1, Vec2 p2, Vec2 q1, Vec2 q2, Vec2 out){
        //check
        tmp1.set(p2).sub(p1);
        tmp2.set(q2).sub(q1);
        if(Mathf.zero(tmp1.len(), 0.01f) || Mathf.zero(tmp1.len(), 0.01f)) return false;

        if(tmp1.crs(tmp3.set(q1).sub(p1)) * tmp1.crs(tmp3.set(q1).sub(p2)) > 0f) return false;
        if(tmp2.crs(tmp3.set(p1).sub(q1)) * tmp2.crs(tmp3.set(p1).sub(q2)) > 0f) return false;

        //valid intersection
        float t;
        if(p1.x - p2.x == 0f) t = (p2.x - q2.x) / (q1.x - q2.x);
        else{
            float mul = -(p1.y - p2.y) / (p1.x - p2.x);
            t = (p2.y - q2.y + mul * (p2.x - q2.x)) / (q1.y - q2.y + mul * (q1.x - q2.x));
        }
        out.set(tmp2).scl(-t).add(q2);

        return true;
    }

}
