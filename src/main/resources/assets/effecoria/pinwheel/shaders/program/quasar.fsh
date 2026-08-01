#include veil:space_helper

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

uniform float Intensity;
uniform float Progress;
uniform float Time;
uniform float RadiusUV;
uniform vec2 CenterUV;
uniform float CenterDepth;
uniform float BehindCamera;

in vec2 texCoord;

out vec4 fragColor;

void main() {
    vec4 base = texture(DiffuseSampler0, texCoord);
    if (Progress < 0.001 || BehindCamera > 0.5) {
        fragColor = base;
        return;
    }

    float aspect = getAspectRatio();
    float strength = clamp(Intensity * Progress, 0.0, 1.55);

    vec2 uv = texCoord;
    vec2 center = CenterUV;
    vec2 delta = uv - center;
    delta.x *= aspect;
    float dist = length(delta);

    // Tiny epicenter (~1 block) — never a screen-filling black hole.
    float core = max(0.012, RadiusUV);
    float halo = core * 2.8;

    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    float depthGate = 1.0;
    if (CenterDepth > 0.0 && CenterDepth < 1.0) {
        depthGate = smoothstep(CenterDepth - 0.06, CenterDepth + 0.02, depthSample);
        depthGate = mix(0.35, 1.0, depthGate);
    }
    strength *= depthGate;

    // Soft local heat shimmer only near the core — no radial lens pull.
    float near = 1.0 - smoothstep(0.0, halo, dist);
    float blade = sin(dist * 90.0 - Time * 8.0);
    vec2 dir = dist > 1e-5 ? delta / dist : vec2(0.0, 1.0);
    vec2 tangent = vec2(-dir.y, dir.x);
    tangent.x /= aspect;
    vec2 sampleUv = clamp(uv + tangent * (strength * 0.006 * near * blade), vec2(0.001), vec2(0.999));
    vec3 color = texture(DiffuseSampler0, sampleUv).rgb;

    // Compact violet plasma star (block-sized)
    float plasma = exp(-pow(dist / max(core * 0.55, 1e-4), 2.0));
    float corona = exp(-pow(dist / max(core * 1.35, 1e-4), 2.0));
    float pulse = 0.7 + 0.3 * sin(Time * 7.5);

    color += vec3(0.55, 0.18, 1.25) * plasma * 1.55 * strength * pulse;
    color += vec3(0.85, 0.4, 1.45) * plasma * 0.55 * strength;
    color += vec3(0.35, 0.12, 0.85) * corona * 0.45 * strength;

    // Faint outer warmth (not black, not a horizon)
    float warm = (1.0 - smoothstep(core * 1.2, halo, dist)) * near;
    color += vec3(0.35, 0.12, 0.05) * warm * 0.12 * strength;

    float edge = smoothstep(halo * 0.75, halo * 1.35, dist);
    color = mix(color, base.rgb, edge);

    fragColor = vec4(color, 1.0);
}
