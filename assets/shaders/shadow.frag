#define HIGHP
#define PRECISION 1.0
#define LIGHTH 5.0
#define MAX_LIGHTS 400
uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec2 u_invsize;
uniform vec2 u_offset;
uniform float u_ambientLight;
uniform int u_lightcount;
uniform vec2 u_lights[MAX_LIGHTS];

varying vec2 v_texCoords;
/** Author @BlackDeluxeCat
* You are not allowed to use this fragment in any place except a Mindustry mod.
*/
vec4 unpack(vec2 value){
    vec4 light;
    if(value.x < 0.0){
        light.x = -100000.0;
        return light;
    }
    light.x = mod(value.x, 50000.0) / 5.0 - 100.0;
    light.y = mod(value.y, 50000.0) / 5.0 - 100.0;
    light.z = floor(value.x / 50000.0);     //source size
    light.w = floor(value.y / 50000.0);     //radius
    return light;
}

void main(){
    vec2 T = v_texCoords.xy;
    vec2 worldxy = (T * u_texsize) + u_offset;
    vec4 color = texture2D(u_texture, T);

    float lightness = 0.0;
    float shadowness = 0.0;

    //source light
    for(int i = -1; i < MAX_LIGHTS; i++){
        vec4 light;
        if(i == -1){
            //ambientLight
            light.xy = worldxy + 24.0;
            light.w = 64.0;
        }else{
            light = unpack(u_lights[i]);
        }


        if(light.x < -10000.0) continue;
        float radius = light.w;
        float sourceSize = light.z;

        float dst = distance(worldxy, light.xy);
        if(dst < radius){
            bool isShadow = false;

            for(float j = 0.0; j < min((dst - sourceSize), dst / LIGHTH); j += PRECISION){
                vec2 blockscreenxy = T + normalize(light.xy - worldxy) * j * u_invsize;
                vec4 shadow = texture2D(u_texture, blockscreenxy);
                if(shadow.a > 0.1){
                    shadowness = max(shadowness, 1.0 - dst / radius);
                    isShadow = true;
                    break;
                }
            }

            if(!isShadow){
                lightness = max(lightness, 1.0 - dst / radius);
            }
        }

        if(i >= u_lightcount - 1){
            break;
        }
    }

    vec4 result = vec4(0.0, 0.0, 0.0, max(color.a, clamp(shadowness * (1.0 - lightness) * sqrt(u_ambientLight * 0.75 + 0.25), 0.0, 0.9)));
    gl_FragColor = result;
}