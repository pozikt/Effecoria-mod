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

    float maxR = max(0.12, RadiusUV);
    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    float depthGate = 1.0;
    if (CenterDepth > 0.0 && CenterDepth < 1.0) {
        depthGate = smoothstep(CenterDepth - 0.14, CenterDepth + 0.05, depthSample);
        depthGate = mix(0.4, 1.0, depthGate);
    }
    strength *= depthGate;

    // Expanding gravitational wave fronts driven by Progress.
    float life = clamp(Progress, 0.0, 1.0);
    float front = maxR * (0.15 + 0.95 * life);
    float front2 = maxR * (0.05 + 0.7 * life);
    float front3 = maxR * (0.0 + 0.45 * life);

    float band = 0.018 + 0.01 * strength;
    float w1 = exp(-pow((dist - front) / band, 2.0));
    float w2 = exp(-pow((dist - front2) / (band * 1.15), 2.0)) * 0.7;
    float w3 = exp(-pow((dist - front3) / (band * 1.35), 2.0)) * 0.45;
    float wave = (w1 + w2 + w3) * (1.0 - smoothstep(maxR * 0.95, maxR * 1.2, dist));

    vec2 dir = dist > 1e-5 ? delta / dist : vec2(0.0, 1.0);
    // Shock: push UV outward at the crest.
    vec2 offset = dir * wave * 0.045 * strength;
    offset.x /= aspect;
    vec2 sampleUv = clamp(uv + offset, vec2(0.001), vec2(0.999));

    float chroma = 0.008 * strength * wave;
    vec2 tangent = vec2(-dir.y, dir.x);
    tangent.x /= aspect;
    float rC = texture(DiffuseSampler0, clamp(sampleUv + tangent * chroma, vec2(0.001), vec2(0.999))).r;
    float gC = texture(DiffuseSampler0, sampleUv).g;
    float bC = texture(DiffuseSampler0, clamp(sampleUv - tangent * chroma, vec2(0.001), vec2(0.999))).b;
    vec3 color = vec3(rC, gC, bC);

    color += vec3(0.65, 0.85, 1.15) * wave * 0.55 * strength;
    color += vec3(0.35, 0.55, 0.9) * wave * 0.18 * strength;

    float mixAmt = clamp(strength * (0.35 + wave * 0.9), 0.0, 1.0);
    color = mix(base.rgb, color, mixAmt);
    fragColor = vec4(color, 1.0);
}
