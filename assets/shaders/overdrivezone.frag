#define HIGHP
#define wide 2.0
uniform sampler2D u_texture;
uniform vec2 u_texsize;
uniform vec2 u_invsize;
uniform float u_time;
uniform float u_dp;
uniform vec2 u_offset;
varying vec2 v_texCoords;
/** Author @BlackDeluxeCat */
void main(){
    vec2 T = v_texCoords.xy;
    vec2 coords = (T * u_texsize) + u_offset;
    vec4 color = texture2D(u_texture, T);

	vec2 v = u_invsize;

    vec4 maxed = max(max(max(texture2D(u_texture, T + vec2(0, wide) * v), texture2D(u_texture, T + vec2(0, -wide) * v)), texture2D(u_texture, T + vec2(wide, 0) * v)), texture2D(u_texture, T + vec2(-wide, 0) * v));

	if(texture2D(u_texture, T).a < 0.02 && maxed.a > 0.49){
		gl_FragColor = vec4(maxed.rgb, maxed.a * 100.0);
    }else{
        if(color.a > 0.03){
            color.a = (0.05 + (mod(coords.y / 1.0 + coords.x / 1.8 - u_time / 12.0, 32.0) > 20.0 ? 0.2 * (step(mod((coords.x / 4.0 + coords.y / 16.0 - u_time / 6.0) / 4.0, 2.0), 1.0)) : 0.0));
        }else{
            color.a = 0.0;
        }
        gl_FragColor = color;
    }   
}
