#version 330 core

in vec2 vUV;
out vec4 FragColor;

uniform mat4 invProjection;
uniform mat4 invView;
uniform samplerCube skybox;
uniform float time;

vec3 skyColor(vec3 dir, vec3 sunDir) {
    float t = max(dir.y * 0.5 + 0.5, 0.0);

    float sunHeight = sunDir.y;

    vec3 daySky = vec3(0.2, 0.5, 0.9);
    vec3 sunsetSky = vec3(1.0, 0.3, 0.05);
    vec3 nightSky = vec3(0.02, 0.02, 0.05);

    float dayFactor = smoothstep(0.0, 0.3, sunHeight);
    float nightFactor = smoothstep(-0.3, 0.0, sunHeight);

    vec3 skyBase = mix(nightSky, sunsetSky, nightFactor);
    skyBase = mix(skyBase, daySky, dayFactor);

    return mix(nightSky, skyBase, t);
}

vec3 applySunset(vec3 dir, vec3 sunDir) {
    float horizon = 1.0 - abs(dir.y);
    float alignment = max(dot(dir, sunDir), 0.0);

    float glow = pow(alignment, 4.0) * horizon;

    vec3 sunsetColor = vec3(1.0, 0.4, 0.1);

    return sunsetColor * glow * 2.0;
}

vec3 getSunDir(float time) {
    float angle = time * 0.05;

    return normalize(vec3(
    cos(angle),
    sin(angle),
    0.0
    ));
}

vec3 applySun(vec3 dir, vec3 sunDir) {
    float sunDot = max(dot(dir, sunDir), 0.0);

    float disk = pow(sunDot, 1024.0);
    float glow = pow(sunDot, 32.0);

    vec3 sunColor = vec3(1.0, 0.9, 0.6);

    return sunColor * (disk * 15.0 + glow * 0.5);
}

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.1, 0.1, 0.1));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

vec3 getMoonDir(float time) {
    // Opposite the sun, slower rotation
    float angle = time * 0.02 + 3.14159;// π offset for opposite side
    return normalize(vec3(cos(angle), sin(angle)*0.5, sin(angle*0.3)));
}

vec3 applyMoon(vec3 dir, vec3 moonDir) {
    float moonDot = max(dot(dir, moonDir), 0.0);
    float disk = pow(moonDot, 256.0);
    float glow = pow(moonDot, 16.0);

    vec3 moonColor = vec3(0.8, 0.8, 0.85);// pale white
    return moonColor * (disk * 2.0 + glow * 0.2);
}

vec3 milkyWayGlow(vec3 dir) {
    // Define the plane of the Milky Way
    vec3 planeNormal = normalize(vec3(0.0, 0.3, 1.0));
    float distance = abs(dot(dir, planeNormal));

    // Band factor: brightest along the plane, falloff away from it
    float bandFactor = smoothstep(0.25, 0.0, distance);// 1 along plane, 0 far away

    // Milky Way color gradient (core: reddish-yellow, edges: bluish)
    vec3 coreColor = vec3(1.0, 0.8, 0.6);
    vec3 edgeColor = vec3(0.6, 0.7, 1.0);

    vec3 color = mix(edgeColor, coreColor, bandFactor);

    // Slight glow
    return color * bandFactor * 0.5;
}

float twinkle(vec3 cell, float t) {
    // Random seed per star cell
    float rnd1 = hash(cell);// controls speed
    float rnd2 = hash(cell * 1.37);// controls phase/offset

    // Speed range: slow to fast
    float speed = mix(0.05, 0.25, rnd1);

    // Phase offset
    float phase = rnd2 * 6.28318;

    // Sin for twinkle
    return sin(t * speed + phase) * 0.5 + 0.5;
}

float starBand(vec3 dir) {
    // Define the plane of the Milky Way (tilted slightly)
    vec3 planeNormal = normalize(vec3(0.0, 0.3, 1.0));

    // Distance from the plane
    float distance = abs(dot(dir, planeNormal));

    // Band factor: brightest at the plane, falls off quickly
    return smoothstep(0.01, 0.15, distance);// 0 near plane, 1 far away
}

vec3 stars(vec3 dir) {
    float bandFactor = (1.0 - starBand(dir)) / 10.0;

    float scale = 300.0;
    vec3 p = normalize(dir) * scale;
    vec3 cell = floor(p);
    vec3 local = fract(p) - 0.5;

    float rnd = hash(cell);

    float starPresent = step(1.0 - (0.995 * bandFactor + 0.01), rnd);
    float d = length(local);
    float intensity = smoothstep(0.5, 0.0, d);

    vec3 starColor = vec3(1.0, 0.95, 0.8);

    float tw = twinkle(cell, time);
    return starPresent * intensity * tw * starColor;
}

float getNightFactor(vec3 sunDir) {
    return smoothstep(0.1, -0.2, sunDir.y);
}

void main() {
    vec2 ndc = vUV * 2.0 - 1.0;

    vec4 clip = vec4(ndc, -1.0, 1.0);

    vec4 view = invProjection * clip;
    view = vec4(view.xy, -1.0, 0.0);

    vec3 dir = normalize((invView * view).xyz);

    vec3 sunDir = getSunDir(time);

    float nightFactor = getNightFactor(sunDir);

    vec3 sky = skyColor(dir, sunDir);
    vec3 sunset = applySunset(dir, sunDir);
    vec3 sun = applySun(dir, sunDir);

    vec3 moonDir = getMoonDir(time);
    vec3 moon = applyMoon(dir, moonDir);

    vec3 mwGlow = milkyWayGlow(dir) * nightFactor;

    vec3 starsField = stars(dir) * nightFactor;

    FragColor = vec4(sky + sunset + sun + moon + mwGlow + starsField, 1.0);
}