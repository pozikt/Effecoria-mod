#include veil:space_helper

uniform sampler2D DiffuseSampler0;
uniform sampler2D DiffuseDepthSampler;

uniform float Intensity;
uniform float Progress;
uniform float Time;
uniform vec2 CenterUV;
uniform float CenterDepth;
uniform float BehindCamera;
/** Portal plane stretch in screen space (1 = circle; >1 taller). */
uniform float EllipseY;

in vec2 texCoord;

out vec4 fragColor;

float livingRim(float ang, float time) {
    // Uneven, crawling tear — several harmonics so the rim never sits still.
    return 1.0
        + 0.055 * sin(ang * 5.0 + time * 2.7)
        + 0.035 * sin(ang * 11.0 - time * 3.4)
        + 0.025 * sin(ang * 19.0 + time * 1.6)
        + 0.018 * sin(ang * 29.0 + time * 4.1 + 1.3);
}

void main() {
    vec4 base = texture(DiffuseSampler0, texCoord);
    if (Progress < 0.001 || BehindCamera > 0.5) {
        fragColor = base;
        return;
    }

    float aspect = getAspectRatio();
    float strength = clamp(Intensity * Progress, 0.0, 1.65);
    float ellY = max(EllipseY, 0.55);

    vec2 uv = texCoord;
    vec2 center = CenterUV;
    vec2 delta = uv - center;
    delta.x *= aspect;
    delta.y /= ellY;

    float dist = length(delta);
    float ang = atan(delta.y, delta.x);
    float rimMul = livingRim(ang, Time);
    float influence = (0.20 + 0.14 * strength) * rimMul;
    float softCore = 0.045 * rimMul;

    float depthSample = texture(DiffuseDepthSampler, texCoord).r;
    float depthGate = 1.0;
    if (CenterDepth > 0.0 && CenterDepth < 1.0) {
        depthGate = smoothstep(CenterDepth - 0.10, CenterDepth + 0.03, depthSample);
        depthGate = mix(0.30, 1.0, depthGate);
    }
    strength *= depthGate;

    vec2 dir = dist > 1e-5 ? delta / dist : vec2(0.0, 1.0);

    // Supermassive lens: rays deflect toward the puncture (1/r falloff with soft core).
    float mass = strength * 0.32;
    float deflect = mass / (dist + softCore);
    // Frame-dragging swirl near the tear.
    float drag = strength * 0.55 * (1.0 - smoothstep(softCore, influence, dist)) / (dist + 0.06);
    float swirl = drag * (0.55 + 0.25 * sin(Time * 1.8 + ang * 2.0));
    float sa = sin(swirl);
    float ca = cos(swirl);
    vec2 spun = vec2(dir.x * ca - dir.y * sa, dir.x * sa + dir.y * ca);

    // Sample from farther out as if light bent inward (gravitational refraction).
    float sampleDist = dist + deflect * (1.0 - smoothstep(0.0, influence * 1.35, dist));
    vec2 sampleOff = spun * sampleDist;
    sampleOff.x /= aspect;
    sampleOff.y *= ellY;
    vec2 sampleUv = clamp(center + sampleOff, vec2(0.001), vec2(0.999));

    // Chromatic shear along the living rim.
    float nearRim = 1.0 - smoothstep(influence * 0.55, influence * 1.15, dist);
    float chroma = 0.012 * strength * nearRim;
    vec2 tangent = vec2(-spun.y, spun.x);
    tangent.x /= aspect;
    tangent.y *= ellY;
    float rC = texture(DiffuseSampler0, clamp(sampleUv + tangent * chroma, vec2(0.001), vec2(0.999))).r;
    float gC = texture(DiffuseSampler0, sampleUv).g;
    float bC = texture(DiffuseSampler0, clamp(sampleUv - tangent * chroma, vec2(0.001), vec2(0.999))).b;
    vec3 color = vec3(rC, gC, bC);

    // Subtle photon-ring shimmer on the wobbling edge (not a black-hole fill).
    float ringDist = abs(dist - influence * 0.72);
    float ring = exp(-pow(ringDist / (0.018 + 0.01 * strength), 2.0));
    color += vec3(0.45, 0.72, 1.15) * ring * 0.55 * strength * nearRim;

    // Keep the very center mostly untouched — BER draws the star void.
    float hole = 1.0 - smoothstep(softCore * 0.6, softCore * 1.8, dist);
    color = mix(color, base.rgb, hole * 0.85);

    float edge = smoothstep(influence * 0.85, influence * 1.55, dist);
    color = mix(color, base.rgb, edge);

    fragColor = vec4(color, 1.0);
}
