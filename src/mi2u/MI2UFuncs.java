package mi2u;

import arc.*;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import mindustry.core.*;
import mindustry.entities.units.*;
import mindustry.game.Teams.*;
import mindustry.gen.*;
import mindustry.input.*;

import static mindustry.Vars.*;

/** @Author 工业2*/
public class MI2UFuncs{
    public static Vec2 tmp1 = new Vec2(), tmp2 = new Vec2(), tmp3 = new Vec2(), tmp4 = new Vec2();
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

    public static int lastTouches = 0;
    /**删除指定区域并转化为待放置的蓝图*/
    public static void deleteToScheme(){
        if(!state.isGame() || player.unit() == null || !player.unit().canBuild() || !control.input.selectPlans.isEmpty()) return;

        boolean touchUp = lastTouches != 0 && ((lastTouches = Core.input.getTouches()) == 0);

        if((control.input instanceof DesktopInput di && Core.input.keyRelease(Binding.breakBlock))){
            int rawCursorX = World.toTile(Core.input.mouseWorld().x), rawCursorY = World.toTile(Core.input.mouseWorld().y);
            control.input.lastSchematic = schematics.create(di.schemX, di.schemY, rawCursorX, rawCursorY);
            control.input.selectPlans.add(schematics.toPlans(control.input.lastSchematic, rawCursorX, rawCursorY));
        }else if(control.input instanceof MobileInput mi && mi.isBreaking() && touchUp){
            control.input.selectPlans.add(schematics.toPlans(schematics.create(mi.lineStartX, mi.lineStartY, mi.lastLineX, mi.lastLineY), player.tileX(), player.tileY()));
        }
    }

    public static void cleanGhostBlock(){
        if(!state.isGame()) return;
        Call.deletePlans(player, Seq.with(state.teams.get(player.team()).plans).mapInt(plan -> Point2.pack(plan.x, plan.y)).toArray());
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
