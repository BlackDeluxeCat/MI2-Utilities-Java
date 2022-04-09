package mi2u.ai;

import arc.Core;
import arc.math.geom.Geometry;
import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.struct.Seq;
import mi2u.MI2UTmp;
import mi2u.input.DesktopInputExt;
import mi2u.input.InputOverwrite;
import mi2u.input.MobileInputExt;
import mi2u.io.MI2USettings;
import mindustry.content.Blocks;
import mindustry.ctype.ContentType;
import mindustry.entities.Predict;
import mindustry.entities.Units;
import mindustry.entities.units.AIController;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Iconc;
import mindustry.input.Binding;
import mindustry.type.Item;
import mindustry.world.Tile;
import mindustry.world.meta.BlockFlag;

import static mindustry.Vars.*;

public class FullAI extends AIController{
    public Seq<Mode> modes = new Seq<>();

    public FullAI(){
        super();
        modes.add(new MineMode());
        modes.add(new BaseMineMode(){
            @Override
            public void updateList() {
                list.clear();
                list.add(content.getByID(ContentType.item, 0), content.getByID(ContentType.item, 1));
            }
        });
        modes.add(new AutoBuildMode());
        modes.add(new SelfRepairMode());
        modes.add(new AutoTargetMode());
    }

    @Override
    public void updateUnit(){
        if(!(control.input instanceof InputOverwrite) && MI2USettings.getBool("inputReplace", true)) {
            if(mobile){
                MobileInputExt.mobileExt.replaceInput();
            }else{
                DesktopInputExt.desktopExt.replaceInput();
            }
        }
        if(control.input instanceof InputOverwrite inp){
            inp.clear();
            modes.each(Mode::act);
        }
    }

    /**unit actions can be covered by the lasted related mode. Executed after each mode acted.*/
    public void moveAction(Position target, float radius, boolean checkWithin){
        if(control.input instanceof InputOverwrite inp) inp.approach(target, radius, checkWithin);
    }

    public void boostAction(boolean boost){
        if(control.input instanceof InputOverwrite inp) inp.boost(boost);
    }

    public void shootAction(Vec2 point, Boolean shoot){
        if(control.input instanceof InputOverwrite inp) inp.shoot(point, shoot, true);
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
        public Seq<Item> list = new Seq<>();
        boolean mining;
        Item targetItem;
        Tile ore;
        
        public BaseMineMode(){
            btext = Iconc.unitMono + "";
        }

        @Override
        public void act(){
            if(!enable) return;
            updateList();
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
                        moveAction(ore, 50f, false);
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
                moveAction(core, itemTransferRange / 2f, false);
            }
        }

        public void updateList(){
        }
    }

    public class MineMode extends BaseMineMode{
        public MineMode(){
            btext = Iconc.unitMono + "+";
        }
        @Override
        public void updateList(){
            list = content.items();
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
            if(!control.input.isBuilding) return;
            if(unit.plans().isEmpty() || !unit.canBuild()) return;
            boostAction(true);
            moveAction(unit.plans().first(), buildingRange / 1.4f, true);
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
            moveAction(tile, 20f, false);
        }
    }

    public class AutoTargetMode extends Mode{
        public AutoTargetMode(){
            btext = "AT";
        }
        @Override
        public void act(){
            if(!enable) return;
            if(Core.input.keyDown(Binding.select)) return;

            if(timer.get(timerTarget2, 30f)){
                float range = unit.hasWeapons() ? unit.range() : 0f;
                target = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(unit.type.targetAir, unit.type.targetGround), u -> unit.type.targetGround);
                if(unit.type.canHeal && target == null){
                    target = Geometry.findClosest(unit.x, unit.y, indexer.getDamaged(unit.team));
                    if(target != null && !unit.within(target, range)){
                        target = null;
                    }
                }
            }

            if(target != null){
                Vec2 intercept = Predict.intercept(unit, target, unit.hasWeapons() ? unit.type.weapons.first().bullet.speed : 0f);
                shootAction(intercept, true);
            }else{
                shootAction(MI2UTmp.v1.set(player.mouseX, player.mouseY), false);
            }
        }
    }
}
