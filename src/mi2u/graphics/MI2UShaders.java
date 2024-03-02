package mi2u.graphics;

import arc.*;
import arc.files.*;
import arc.graphics.gl.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.world.blocks.defense.*;

public class MI2UShaders{
    public static OverDriverZoneShader odzone;
    public static MenderZoneShader mdzone;
    public static RegenZoneShader rgzone;
    public static TurretZoneShader turretzone;

    public static void load(){
        odzone = new OverDriverZoneShader();
        mdzone = new MenderZoneShader();
        rgzone = new RegenZoneShader();
        turretzone = new TurretZoneShader();
    }

    public static class MI2UShader extends Shader{
        public MI2UShader(String frag, boolean treeFrag, String vert, boolean treeVert){
            super(getShaderFi(vert, treeVert), getShaderFi(frag, treeFrag));
        }
        public MI2UShader(String frag, String vert){
            this(frag, true, vert, false);
        }
    }

    public static Fi getShaderFi(String name, boolean tree){
        if(tree) return Vars.tree.get("shaders/" + name);
        return Core.files.internal("shaders/" + name);
    }

    public static class OverDriverZoneShader extends MI2UShader{
        public OverDriverZoneShader(){
            super("overdrivezone.frag", "screenspace.vert");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Scl.scl(1f));
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_offset",
                    Core.camera.position.x - Core.camera.width / 2,
                    Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_texsize", Core.camera.width, Core.camera.height);
            setUniformf("u_invsize", 1f/Core.camera.width, 1f/Core.camera.height);
        }
    }

    public static class RegenZoneShader extends MI2UShader{
        public RegenZoneShader(){
            super("regenzone.frag", "screenspace.vert");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Scl.scl(1f));
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_offset",
                    Core.camera.position.x - Core.camera.width / 2,
                    Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_texsize", Core.camera.width, Core.camera.height);
            setUniformf("u_invsize", 1f/Core.camera.width, 1f/Core.camera.height);
        }
    }

    public static class MenderZoneShader extends MI2UShader{
        public MenderZoneShader(){
            super("menderzone.frag", "screenspace.vert");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Scl.scl(1f));
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_color", ((MendProjector)Blocks.mendProjector).baseColor);
            setUniformf("u_offset",
                    Core.camera.position.x - Core.camera.width / 2,
                    Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_texsize", Core.camera.width, Core.camera.height);
            setUniformf("u_invsize", 1f/Core.camera.width, 1f/Core.camera.height);
        }
    }

    public static class TurretZoneShader extends MI2UShader{
        public TurretZoneShader(){
            super("turretzone.frag", "screenspace.vert");
        }

        @Override
        public void apply(){
            setUniformf("u_dp", Scl.scl(1f));
            setUniformf("u_time", Time.time / Scl.scl(1f));
            setUniformf("u_offset",
                    Core.camera.position.x - Core.camera.width / 2,
                    Core.camera.position.y - Core.camera.height / 2);
            setUniformf("u_texsize", Core.camera.width, Core.camera.height);
            setUniformf("u_invsize", 1f/Core.camera.width, 1f/Core.camera.height);
        }
    }
}
