#include veil:space_helper

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

uniform float Intensity;
uniform float Progress;
uniform float Time;
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
    float strength = clamp(Intensity * Progress, 0.0, 1.4);

    vec2 uv = texCoord;
    vec2 center = CenterUV;
    vec2 delta = uv - center;
    delta.x *= aspect;
    float dist = length(delta);

    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    float depthGate = 1.0;
    if (CenterDepth > 0.0 && CenterDepth < 1.0) {
        depthGate = smoothstep(CenterDepth - 0.14, CenterDepth + 0.05, depthSample);
        depthGate = mix(0.4, 1.0, depthGate);
    }
    strength *= depthGate;

    // Expanding concentric ripple — space flexes, no horizon / accretion
    float reach = 0.22 + 0.18 * Intensity;
    float falloff = 1.0 - smoothstep(0.0, reach, dist);
    float wave = sin(dist * 42.0 - Time * 14.0) * exp(-dist * 6.5);
    float ripple = wave * falloff * strength;

    vec2 dir = dist > 1e-5 ? delta / dist : vec2(0.0, 1.0);
    vec2 offset = dir * ripple * 0.028;
    offset.x /= aspect;

    vec2 sampleUv = clamp(uv + offset, vec2(0.001), vec2(0.999));

    // Soft chromatic fringe on the ripples
    float chroma = 0.004 * strength * falloff;
    vec2 tangent = vec2(-dir.y, dir.x);
    tangent.x /= aspect;
    float rC = texture(DiffuseSampler0, clamp(sampleUv + tangent * chroma, vec2(0.001), vec2(0.999))).r;
    float gC = texture(DiffuseSampler0, sampleUv).g;
    float bC = texture(DiffuseSampler0, clamp(sampleUv - tangent * chroma, vec2(0.001), vec2(0.999))).b;
    vec3 color = vec3(rC, gC, bC);

    // Hairline wave crest highlight (not a black hole)
    float crest = pow(max(0.0, wave), 2.0) * falloff;
    color += vec3(0.55, 0.78, 1.05) * crest * 0.22 * strength;

    color = mix(base.rgb, color, clamp(strength * 1.05, 0.0, 1.0));
    fragColor = vec4(color, 1.0);
}
