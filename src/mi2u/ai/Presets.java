package mi2u.ai;

import arc.struct.*;

public class Presets{
    public static Seq<MAI> all = new Seq<>();

    public final static MAI unitspawner = new MAI("unitspawner", "setrate 5\n" +
                                                                     "jump 25 strictEqual init 1\n" +
                                                                     "set init 1\n" +
                                                                     "set type @flare\n" +
                                                                     "print \"UI.clear()\"\n" +
                                                                     "print \"UI.info(type)\"\n" +
                                                                     "print \"UI.choose(type)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "set strx \"x: \"\n" +
                                                                     "print \"UI.info(strx)\"\n" +
                                                                     "print \"UI.info(x)\"\n" +
                                                                     "print \"UI.field(x)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "set stry \"y: \"\n" +
                                                                     "print \"UI.info(stry)\"\n" +
                                                                     "print \"UI.info(y)\"\n" +
                                                                     "print \"UI.field(y)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "set strrot \"rot\"\n" +
                                                                     "print \"UI.info(strrot)\"\n" +
                                                                     "print \"UI.info(rot)\"\n" +
                                                                     "print \"UI.field(rot)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "print \"UI.info(run)\"\n" +
                                                                     "print \"UI.button(run)\"\n" +
                                                                     "jump 27 strictEqual run 1\n" +
                                                                     "end\n" +
                                                                     "set run 0\n" +
                                                                     "effect placeBlock x y 2 %ffaaff \n" +
                                                                     "spawn type x y rot @crux result\n" +
                                                                     "status false unmoving unit 20\n");

    public final static MAI mapeditor = new MAI("mapeditor", "jump 26 strictEqual init 2\n" +
                                                                     "set brush.size 2\n" +
                                                                     "set floor @air\n" +
                                                                     "set ore @air\n" +
                                                                     "set block @air\n" +
                                                                     "set title \"TerraEditor\"\n" +
                                                                     "set text.ipt \"Ipt\"\n" +
                                                                     "set ipt 50\n" +
                                                                     "print \"UI.info(title)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "print \"UI.info(text.ipt)\"\n" +
                                                                     "print \"UI.field(ipt)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "print \"UI.choose(floor)\"\n" +
                                                                     "print \"UI.info(floor)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "print \"UI.choose(ore)\"\n" +
                                                                     "print \"UI.info(ore)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "print \"UI.info(brush.name)\"\n" +
                                                                     "print \"UI.button(brush.type)\"\n" +
                                                                     "print \"UI.row()\"\n" +
                                                                     "print \"UI.info(brush.size.name)\"\n" +
                                                                     "print \"UI.field(brush.size)\"\n" +
                                                                     "set init 2\n" +
                                                                     "set brush.size.name \"Radius\"\n" +
                                                                     "sensor en @unit @shooting\n" +
                                                                     "set brush.name \"suqare\"\n" +
                                                                     "jump 30 equal brush.type 0\n" +
                                                                     "set brush.name \"circle\"\n" +
                                                                     "sensor tx @unit @shootX\n" +
                                                                     "op add tx tx 0.5\n" +
                                                                     "op idiv tx tx 1\n" +
                                                                     "sensor ty @unit @shootY\n" +
                                                                     "op add ty ty 0.5\n" +
                                                                     "op idiv ty ty 1\n" +
                                                                     "op sub x.min tx brush.size\n" +
                                                                     "op idiv x.min x.min 1\n" +
                                                                     "op add x.max x.min brush.size\n" +
                                                                     "op add x.max x.max brush.size\n" +
                                                                     "op sub y.min ty brush.size\n" +
                                                                     "op idiv y.min y.min 1\n" +
                                                                     "op add y.max y.min brush.size\n" +
                                                                     "op add y.max y.max brush.size\n" +
                                                                     "set x x.min\n" +
                                                                     "op add x x 1\n" +
                                                                     "set y y.min\n" +
                                                                     "op add y y 1\n" +
                                                                     "jump 53 equal brush.type 0\n" +
                                                                     "op sub dx x tx\n" +
                                                                     "op sub dy y ty\n" +
                                                                     "op len d dx dy\n" +
                                                                     "jump 57 greaterThan d brush.size\n" +
                                                                     "effect lightBlock x y 0.5 %ffbd530f \n" +
                                                                     "jump 57 notEqual en 1\n" +
                                                                     "setblock floor floor x y @derelict 0\n" +
                                                                     "setblock ore ore x y @derelict 0\n" +
                                                                     "jump 47 lessThan y y.max\n" +
                                                                     "setrate ipt\n" +
                                                                     "jump 45 lessThan x x.max\n");

    public static final MAI switchteam = new MAI("switchteam", "setrate 1\n" +
                                                                   "jump 6 strictEqual init 1\n" +
                                                                   "set init 1\n" +
                                                                   "print \"UI.clear()\"\n" +
                                                                   "print \"UI.field(team)\"\n" +
                                                                   "print \"UI.button(switch)\"\n" +
                                                                   "jump 8 strictEqual switch 1\n" +
                                                                   "end\n" +
                                                                   "set switch 0\n" +
                                                                   "setprop @team @unit team\n" +
                                                                   "wait 0.2\n");

    public static class MAI{
        String name;
        String value;
        public MAI(String name, String value){
            this.name = name;
            this.value = value;
            all.add(this);
        }
    }
}
