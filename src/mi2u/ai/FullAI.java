package mi2u.ai;

import arc.math.geom.*;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustry.content.*;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;

import static mindustry.Vars.*;

public class FullAI extends AIController{
    public Seq<Mode> modes = new Seq<Mode>();
    boolean ctrlBoost = false; boolean ctrlMove = false;
    boolean boost = false;
    @Nullable Position moveTarget = null;
    float moveRadius = 10f;

    public FullAI(){
        super();
        modes.add(new MineMode());
        modes.add(new BaseMineMode(){{
            list.add(Items.copper, Items.lead);
        }});
        modes.add(new AutoBuildMode());
        modes.add(new SelfRepairMode());
    }

    @Override
    public void updateUnit(){
        ctrlBoost = ctrlMove = false;
        modes.each(mode -> mode.act());
        if(ctrlMove) moveTo(moveTarget, moveRadius, 20f);
        //boost control is invaild in servers.
        if(ctrlBoost) player.boosting = boost;
    }

    /**unit actions can be covered by the lasted related mode. Executed after each mode acted.*/
    public void moveAction(Position target, float radius){
        ctrlMove = true;
        moveTarget = target;
        moveRadius = radius;
    }

    public void boostAction(boolean boost){
        ctrlBoost = true;
        this.boost = boost;
    }

    public class Mode{
        public boolean enable = false;
        public String btext;
        public Mode(){
            btext = Iconc.units + "";
        }
        /** override it. enable should be checked first */
        public void act(){}
    }

    public class BaseMineMode extends Mode{
        public Seq<Item> list = new Seq<Item>();
        boolean mining;
        Item targetItem;
        Tile ore;
        
        public BaseMineMode(){
            btext = Iconc.unitMono + "";
        }

        @Override
        public void act(){
            if(!enable) return;
            Building core = unit.closestCore();
            boostAction(true);
            if(!(unit.canMine()) || core == null) return;
            if(unit.mineTile != null && !unit.mineTile.within(unit, unit.type.miningRange)){
                unit.mineTile(null);
            }
            if(mining){
                if(timer.get(timerTarget2, 60 * 4) || targetItem == null){
                    targetItem = list.min(i -> indexer.hasOre(i) && unit.canMine(i), i -> core.items.get(i));
                }
                //core full of the target item, do nothing
                if(targetItem != null && core.acceptStack(targetItem, 1, unit) == 0){
                    unit.clearItem();
                    unit.mineTile = null;
                    return;
                }
                //if inventory is full, drop it off.
                if(unit.stack.amount >= unit.type.itemCapacity || (targetItem != null && !unit.acceptsItem(targetItem))){
                    mining = false;
                }else{
                    if(timer.get(timerTarget3, 60) && targetItem != null){
                        ore = indexer.findClosestOre(unit, targetItem);
                    }
                    if(ore != null){
                        moveAction(ore, 50f);
                        if(ore.block() == Blocks.air && unit.within(ore, unit.type.miningRange)){
                            unit.mineTile = ore;
                        }
                        if(ore.block() != Blocks.air){
                            mining = false;
                        }
                    }
                }
            }else{
                unit.mineTile = null;
                if(unit.stack.amount == 0){
                    mining = true;
                    return;
                }
                if(unit.within(core, itemTransferRange / 1.5f) && timer.get(timerTarget4, 120f)){
                    if(core.acceptStack(unit.stack.item, unit.stack.amount, unit) > 0){
                        Call.transferInventory(unit.getPlayer(), core);
                    }
                    unit.clearItem();
                    mining = true;
                }
                moveAction(core, itemTransferRange / 2f);
            }
        }
    }

    public class MineMode extends BaseMineMode{
        public MineMode(){
            btext = Iconc.unitMono + "+";
        }
        @Override
        public void act(){
            list = content.items();
            super.act();
        }
    }

    public class AutoBuildMode extends Mode{
        public boolean rebuild = false;
        public AutoBuildMode(){
            btext = Iconc.unitPoly + "";
        }
        @Override
        public void act(){
            if(!enable) return;
            if(unit.plans().isEmpty() || !unit.canBuild()) return;
            boostAction(true);
            moveAction(unit.plans().first(), buildingRange / 1.4f);
        }
    }

    public class SelfRepairMode extends Mode{
        public SelfRepairMode(){
            btext = Iconc.blockRepairPoint + "";
        }
        @Override
        public void act(){
            if(!enable) return;
            if(unit.dead || !unit.isValid()) return;
            if(unit.health > unit.maxHealth * 0.6f) return;
            Tile tile = Geometry.findClosest(unit.x, unit.y, indexer.getAllied(unit.team, BlockFlag.repair));
            if(tile == null) return;
            boostAction(true);
            moveAction(tile, 20f);
        }
    }
}
