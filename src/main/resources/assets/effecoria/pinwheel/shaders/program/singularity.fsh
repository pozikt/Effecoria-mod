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
    float strength = clamp(Intensity * Progress, 0.0, 1.75);

    vec2 uv = texCoord;
    vec2 center = CenterUV;
    vec2 delta = uv - center;
    delta.x *= aspect;
    float dist = length(delta);

    // Horizon / photon sphere in aspect-corrected UV space
    float horizon = 0.11 * (0.55 + 0.65 * strength);
    float photon = horizon * 1.55;
    float influence = 0.48 + 0.28 * strength;

    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    // Soften lensing for geometry clearly in front of the singularity
    float depthGate = 1.0;
    if (CenterDepth > 0.0 && CenterDepth < 1.0) {
        depthGate = smoothstep(CenterDepth - 0.08, CenterDepth + 0.02, depthSample);
        depthGate = mix(0.25, 1.0, depthGate);
    }
    strength *= depthGate;

    vec2 dir = dist > 1e-5 ? delta / dist : vec2(0.0, 1.0);

    float soft = 0.028;
    float pull = strength * 0.24 * (1.0 - smoothstep(0.0, influence, dist));
    float deflect = pull / (dist + soft);

    float drag = strength * 0.9 * (1.0 - smoothstep(horizon, influence, dist)) / (dist + 0.055);
    float ang = drag * (0.6 + 0.22 * sin(Time * 2.4));
    float sa = sin(ang);
    float ca = cos(ang);
    vec2 spun = vec2(dir.x * ca - dir.y * sa, dir.x * sa + dir.y * ca);

    float sampleDist = dist + deflect;
    vec2 sampleOff = spun * sampleDist;
    sampleOff.x /= aspect;
    vec2 sampleUv = clamp(center + sampleOff, vec2(0.001), vec2(0.999));

    float chroma = 0.014 * strength * (1.0 - smoothstep(photon, influence, dist));
    vec2 tangent = vec2(-spun.y, spun.x);
    tangent.x /= aspect;
    float rC = texture(DiffuseSampler0, clamp(sampleUv + tangent * chroma, vec2(0.001), vec2(0.999))).r;
    float gC = texture(DiffuseSampler0, sampleUv).g;
    float bC = texture(DiffuseSampler0, clamp(sampleUv - tangent * chroma, vec2(0.001), vec2(0.999))).b;
    vec3 color = vec3(rC, gC, bC);

    float ring = exp(-pow((dist - photon) / (0.02 + 0.012 * strength), 2.0));
    color += vec3(0.4, 0.78, 1.2) * ring * 1.05 * strength;

    float accretion = exp(-pow((dist - horizon * 1.12) / 0.032, 2.0));
    color += vec3(1.1, 0.48, 0.2) * accretion * 0.65 * strength;

    float inside = 1.0 - smoothstep(horizon * 0.72, horizon * 1.06, dist);
    color = mix(color, vec3(0.0), inside * 0.995);

    float edge = smoothstep(influence * 0.7, influence * 1.4, dist);
    color = mix(color, base.rgb, edge);

    fragColor = vec4(color, 1.0);
}
