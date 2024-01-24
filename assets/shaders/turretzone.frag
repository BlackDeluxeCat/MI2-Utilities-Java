#define HIGHP
#define wide 2.0
uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec2 u_invsize;
uniform float u_time;
uniform float u_dp;
uniform vec2 u_offset;
varying vec2 v_texCoords;
/** Author @BlackDeluxeCat
* You are not allowed to use this fragment shader in any place except any Mindustry mod.
*/
/*
float distanceRad(float a1, float a2){
    float max = max(a1, a2), min = min(a1, a2);
    if(min + 180.0 > max) return max - min;
    return min + 360.0 - max;
}
*/
void main(){
    vec2 T = v_texCoords.xy;
    vec2 coords = (T * u_texsize) + u_offset;
    vec4 color = texture2D(u_texture, T);

	vec2 v = u_invsize;

    vec4 mined = min(min(min(texture2D(u_texture, T + vec2(0, wide) * v), texture2D(u_texture, T + vec2(0, -wide) * v)), texture2D(u_texture, T + vec2(wide, 0) * v)), texture2D(u_texture, T + vec2(-wide, 0) * v));

    if(length(mined.rgb) < 0.0001 && length(color.rgb) > 0.01){
        gl_FragColor = vec4(color.rgb, mod(coords.y / 2.0 + coords.x / 4.0 - u_time / 4.0, 32.0) / 24.0 + 0.5);
    }else{
        if(color.a >= 0.2 && color.a < 0.9){
            color.a = 0.2;
        }else if(color.a >= 0.9){
            color.a = 0.4;
        }
        gl_FragColor = color;
    }
}