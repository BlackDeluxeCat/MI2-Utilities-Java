package mi2u.ui.stats;

import arc.func.*;
import arc.struct.*;

public class Monitors{
    public static Seq<MonitorMeta> all = new Seq<>();

    public static MonitorMeta health = new MonitorMeta("health", HealthBM::new, Type.building),
    itemChart = new MonitorMeta("itemchart", ItemChartBM::new, Type.building),
    logicProcessor = new MonitorMeta("logicprocessor", ProcessorMonitor::new, Type.building);

    public static class MonitorMeta{
        public String name;
        public Prov<Monitor> prov;
        public Type type;

        public MonitorMeta(String name, Prov<Monitor> prov, Type type){
            this.name = "monitor." + name;
            this.prov = prov;
            this.type = type;
            all.add(this);
        }
    }

    public enum Type{
        building, teamdata, none;
    }
}
