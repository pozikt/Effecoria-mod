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
    float strength = clamp(Intensity * Progress, 0.0, 1.45);

    vec2 uv = texCoord;
    vec2 center = CenterUV;
    vec2 delta = uv - center;
    delta.x *= aspect;
    float dist = length(delta);

    // Area curvature footprint — scales with gameplay radius, never a horizon.
    float reach = max(0.08, RadiusUV);
    float bowl = 1.0 - smoothstep(0.0, reach, dist);

    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    float depthGate = 1.0;
    if (CenterDepth > 0.0 && CenterDepth < 1.0) {
        depthGate = smoothstep(CenterDepth - 0.16, CenterDepth + 0.06, depthSample);
        depthGate = mix(0.35, 1.0, depthGate);
    }
    strength *= depthGate;

    vec2 dir = dist > 1e-5 ? delta / dist : vec2(0.0, 1.0);

    // Soft radial stretch toward the well (space dips), no black fill.
    float pull = strength * 0.055 * bowl * bowl;
    float swirl = strength * 0.12 * bowl * sin(Time * 1.6 + dist * 18.0);
    float sa = sin(swirl);
    float ca = cos(swirl);
    vec2 spun = vec2(dir.x * ca - dir.y * sa, dir.x * sa + dir.y * ca);

    // Contour-line ripples across the warped area.
    float contours = sin(dist * (22.0 / max(reach, 0.05)) - Time * 3.2) * 0.5 + 0.5;
    float crest = pow(contours, 4.0) * bowl;

    vec2 offset = spun * (-pull + crest * 0.012 * strength);
    offset.x /= aspect;
    vec2 sampleUv = clamp(uv + offset, vec2(0.001), vec2(0.999));

    float chroma = 0.005 * strength * bowl;
    vec2 tangent = vec2(-spun.y, spun.x);
    tangent.x /= aspect;
    float rC = texture(DiffuseSampler0, clamp(sampleUv + tangent * chroma, vec2(0.001), vec2(0.999))).r;
    float gC = texture(DiffuseSampler0, sampleUv).g;
    float bC = texture(DiffuseSampler0, clamp(sampleUv - tangent * chroma, vec2(0.001), vec2(0.999))).b;
    vec3 color = vec3(rC, gC, bC);

    // Cool spacetime contour highlights — not accretion / photon sphere.
    color += vec3(0.42, 0.72, 1.05) * crest * 0.28 * strength;
    color += vec3(0.25, 0.45, 0.75) * bowl * 0.06 * strength;

    float edge = smoothstep(reach * 0.72, reach * 1.25, dist);
    color = mix(color, base.rgb, edge);
    fragColor = vec4(color, 1.0);
}
