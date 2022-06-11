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
void main(){
    vec2 T = v_texCoords.xy;
    vec2 coords = (T * u_texsize) + u_offset;
    vec4 color = texture2D(u_texture, T);

	vec2 v = u_invsize;

    vec4 maxed = max(max(max(texture2D(u_texture, T + vec2(0, wide) * v), texture2D(u_texture, T + vec2(0, -wide) * v)), texture2D(u_texture, T + vec2(wide, 0) * v)), texture2D(u_texture, T + vec2(-wide, 0) * v));

	if(texture2D(u_texture, T).a < 0.02 && maxed.a > 0.02){
		gl_FragColor = vec4(maxed.rgb, 0.3);
    }else{
        if(color.a < 0.1){
            color.a = 0.1;
        }
        gl_FragColor = color;
    }   
}
