#version 330 core

#define TEXTURE_SIZE 16u

in vec2 TexCoord;
smooth in vec3 position;
in vec4 vLighting;
in vec3 vNormal;
in vec3 faceNormal;
flat in uint blockType;
in vec2 skyUV;

out vec4 FragColor;

uniform uint meshType;

uniform vec3 cameraPosition;
uniform vec2 screenResolution;

uniform mat4 invProjection;
uniform mat4 invView;
uniform sampler2D skyTexture;

uniform vec4 fogColor;
uniform float fogFar;
uniform float fogNear;
uniform float renderDistance;

uniform float time;

uniform sampler2D blockTexture;
uniform float skyIntensity;
uniform vec4 highlightColor;


// ============================================================
// AURORA SETTINGS
// ============================================================

#define AURORA_BOTTOM_COLOR vec3(0.05, 0.35, 0.15)
#define AURORA_TOP_COLOR    vec3(0.15, 0.8, 0.35)

#define AURORA_ALPHA 1.0
#define AURORA_DENSITY 0.5
#define AURORA_SHARPNESS 1.0

#define AURORA_SAMPLES 30

#define AURORA_START_HEIGHT 2.0
#define AURORA_END_HEIGHT   4.0

#define AURORA_FLOW_SCALE    0.15
#define AURORA_FLOW_STRENGTH 1.0
#define AURORA_FLOW_SPEED    0.1
#define AURORA_FLOW_X_SPEED  0.0

#define AURORA_WIGGLE_SCALE    0.5
#define AURORA_WIGGLE_STRENGTH 0.5
#define AURORA_WIGGLE_SPEED    0.2

#define AURORA_OPACITY_PER_SAMPLE 0.08

#define UNDERSPARKLE_PRIMARY   vec3(0.3, 1.0, 0.5)
#define UNDERSPARKLE_SECONDARY vec3(0.8, 1.0, 0.3)
#define UNDERSPARKLE_SCALE 3.0
#define UNDERSPARKLE_SPEED 0.2
#define UNDERSPARKLE_THRESHOLD 0.8
#define UNDERSPARKLE_MAX_HEIGHT 0.5

float hash(vec3 p)
{
    p = fract(p * 0.1031);
    p += dot(p, p.yzx + 33.33);
    return fract((p.x + p.y) * p.z);
}

float noise3D(vec3 p)
{
    vec3 i = floor(p);
    vec3 f = fract(p);

    f = f * f * f * (f * (f * 6.0 - 15.0) + 10.0);

    float n000 = hash(i);
    float n100 = hash(i + vec3(1, 0, 0));
    float n010 = hash(i + vec3(0, 1, 0));
    float n110 = hash(i + vec3(1, 1, 0));
    float n001 = hash(i + vec3(0, 0, 1));
    float n101 = hash(i + vec3(1, 0, 1));
    float n011 = hash(i + vec3(0, 1, 1));
    float n111 = hash(i + vec3(1, 1, 1));

    float x00 = mix(n000, n100, f.x);
    float x10 = mix(n010, n110, f.x);
    float x01 = mix(n001, n101, f.x);
    float x11 = mix(n011, n111, f.x);

    float y0 = mix(x00, x10, f.y);
    float y1 = mix(x01, x11, f.y);

    return mix(y0, y1, f.z);
}


// ============================================================
// AURORA FUNCTIONS
// ============================================================

float makeStripe(float x, float halfSizeNormalized)
{
    float baseValue = fract(x);

    float left = smoothstep(
            0.5 - halfSizeNormalized,
            0.5,
            baseValue
    );

    float right = smoothstep(
            0.5 + halfSizeNormalized,
            0.5,
            baseValue
    );

    return left * right;
}

vec3 getSunDir(float time)
{
    float angle = time * 0.05;

    return normalize(vec3(
            cos(angle),
            sin(angle),
            0.0
    ));
}

float getNightFactor(float time)
{
    vec3 sunDir = getSunDir(time);

    return 1.0 - smoothstep(
            -0.15,
            0.25,
            sunDir.y
    );
}

vec3 getSunColor(float time)
{
    vec3 sunDir = getSunDir(time);

    // 0 at noon, 1 at horizon/night.
    float horizon =
    1.0 - abs(sunDir.y);

    // Warm near sunrise/sunset.
    float sunset =
    smoothstep(
            0.0,
            0.35,
            horizon
    );

    return mix(
            vec3(1.0, 0.95, 0.8),
            vec3(1.0, 0.45, 0.15),
            sunset
    );
}

float getSunDisk(vec3 viewDir, float time)
{
    vec3 sunDir = getSunDir(time);

    float d = dot(viewDir, sunDir);

    // Angular size of the sun.
    float radius = 0.9994;

    return smoothstep(
            radius,
            radius + 0.0005,
            d
    );
}

vec4 aurora(vec3 viewDir)
{
    // Looking at/below the horizon.
    if (viewDir.y < 0.001)
    return vec4(0.0);

    float accumulatedAlpha = 0.0;
    vec3 accumulatedColor = vec3(0.0);

    float flowTime = time * AURORA_FLOW_SPEED;
    float wiggleTime = time * AURORA_WIGGLE_SPEED;

    for (int i = 0; i < AURORA_SAMPLES; i++)
    {
        float heightFactor =
        float(i) / float(AURORA_SAMPLES - 1);

        float height =
        mix(
        AURORA_START_HEIGHT,
                AURORA_END_HEIGHT,
                heightFactor
        );

        // Intersect the view ray with this horizontal layer.
        float t = height / viewDir.y;

        vec3 p = viewDir * t;
        vec2 worldPos = p.xz;


        // ----------------------------------------------------
        // FLOW
        // ----------------------------------------------------

        vec2 flow;

        flow.x = noise3D(vec3(
                worldPos.x * AURORA_FLOW_SCALE,
                worldPos.y * AURORA_FLOW_SCALE,
                flowTime
        ));

        flow.y = noise3D(vec3(
                worldPos.x * AURORA_FLOW_SCALE,
                worldPos.y * AURORA_FLOW_SCALE,
                0.5 + flowTime
        ));

        vec2 flowDir = normalize(flow);


        // ----------------------------------------------------
        // WIGGLE
        // ----------------------------------------------------

        vec2 wigglePos =
        worldPos * AURORA_WIGGLE_SCALE;

        float timeOffset =
        wigglePos.x + wigglePos.y;

        vec2 wiggleNoise;

        wiggleNoise.x = noise3D(vec3(
                wigglePos.x,
                wigglePos.y,
                wiggleTime + timeOffset
        ));

        wiggleNoise.y = noise3D(vec3(
                wigglePos.x,
                wigglePos.y,
                0.5 + wiggleTime + timeOffset
        ));

        vec2 wiggle =
        wiggleNoise * AURORA_WIGGLE_STRENGTH;


        // ----------------------------------------------------
        // WARP
        // ----------------------------------------------------

        vec2 warpedPos =
        worldPos
        + flowDir * AURORA_FLOW_STRENGTH
        + wiggle
        + vec2(
        AURORA_FLOW_X_SPEED * time,
                AURORA_FLOW_X_SPEED * time
        );


        // ----------------------------------------------------
        // BANDS
        // ----------------------------------------------------

        float largeBands =
        makeStripe(
                warpedPos.x * AURORA_DENSITY,
                0.2
        );

        float smallerBands =
        makeStripe(
                warpedPos.x * AURORA_DENSITY * 1.7,
                0.1
        );

        float baseBands =
        pow(
                max(largeBands, smallerBands),
                AURORA_SHARPNESS
        );


        // ----------------------------------------------------
        // VERTICAL FALLOFF
        // ----------------------------------------------------

        float verticalIntensity =
        smoothstep(
                0.0,
                0.15,
                heightFactor
        )
        *
        smoothstep(
                1.0,
                0.5,
                heightFactor
        );


        // ----------------------------------------------------
        // UNDER-SPARKLE
        // ----------------------------------------------------

        float undersparkleIntensity =
        1.0 -
        smoothstep(
                0.0,
                UNDERSPARKLE_MAX_HEIGHT,
                heightFactor
        );

        vec2 sparklePos =
        warpedPos * UNDERSPARKLE_SCALE;

        float sparkleTime =
        time * UNDERSPARKLE_SPEED;

        float sparkleNoise =
        1.0 -
        noise3D(vec3(
                sparklePos.x + sparkleTime,
                sparklePos.y + sparkleTime,
                sparkleTime * 0.3
        ));

        sparkleNoise =
        smoothstep(
        UNDERSPARKLE_THRESHOLD,
                1.0,
                sparkleNoise
        );


        float sparkleColorNoise =
        noise3D(vec3(
                sparklePos.x * 0.3 + 10.0,
                sparklePos.y * 0.3 + 10.0,
                0.0
        ));

        vec3 sparkleColor =
        mix(
        UNDERSPARKLE_PRIMARY,
                UNDERSPARKLE_SECONDARY,
                smoothstep(
                        0.4,
                        1.0,
                        sparkleColorNoise
                )
        );

        float sparkleVisibility =
        smoothstep(
                0.5,
                1.0,
                baseBands
        );

        vec3 sparkle =
        sparkleVisibility
        * undersparkleIntensity
        * sparkleColor
        * sparkleNoise;


        // ----------------------------------------------------
        // CURTAIN
        // ----------------------------------------------------

        float curtain =
        baseBands * verticalIntensity;


        // ----------------------------------------------------
        // ACCUMULATION
        // ----------------------------------------------------

        float sampleAlpha =
        curtain * AURORA_OPACITY_PER_SAMPLE;

        float sampleWeight =
        sampleAlpha * (1.0 - accumulatedAlpha);

        vec3 selectedColor =
        mix(
        AURORA_BOTTOM_COLOR,
                AURORA_TOP_COLOR,
                heightFactor
        );

        accumulatedColor +=
        selectedColor
        * curtain
        * sampleWeight;

        accumulatedColor +=
        sparkle
        * verticalIntensity
        * sampleWeight;

        accumulatedAlpha +=
        sampleAlpha
        * (1.0 - accumulatedAlpha);

        if (accumulatedAlpha > 0.95)
        break;
    }


    float upFactor =
    smoothstep(
            0.05,
            0.7,
            viewDir.y
    );

    // Day/night cycle.
    // Aurora is strongest when the sun is below the horizon.
    float nightFactor = getNightFactor(time);

    // Keep a small amount of aurora around twilight,
    // but strongly suppress it during the day.
    float auroraVisibility =
    smoothstep(
            0.05,
            0.35,
            nightFactor
    );

    float finalAlpha =
    accumulatedAlpha
    * upFactor
    * AURORA_ALPHA
    * auroraVisibility;

    return vec4(
            accumulatedColor
            * upFactor
            * AURORA_ALPHA
            * auroraVisibility,
            finalAlpha
    );
}

vec3 getDaySkyColor(vec3 viewDir)
{
    float h = clamp(viewDir.y * 0.5 + 0.5, 0.0, 1.0);

    vec3 horizon = vec3(0.45, 0.65, 0.9);
    vec3 zenith = vec3(0.05, 0.2, 0.65);

    return mix(horizon, zenith, h);
}

vec3 getNightSkyColor(vec3 viewDir)
{
    float h = clamp(viewDir.y * 0.5 + 0.5, 0.0, 1.0);

    vec3 horizon = vec3(0.015, 0.025, 0.07);
    vec3 zenith = vec3(0.002, 0.005, 0.02);

    return mix(horizon, zenith, h);
}

vec4 skyColorMain()
{
    vec2 ndc = skyUV * 2.0 - 1.0;

    vec4 clip = vec4(ndc, -1.0, 1.0);

    vec4 view = invProjection * clip;

    view = vec4(
            view.xy,
            -1.0,
            0.0
    );

    vec3 dir =
    normalize(
            (invView * view).xyz
    );

    // --------------------------------------------------------
    // DAY / NIGHT
    // --------------------------------------------------------

    vec3 sunDir = getSunDir(time);

    float nightFactor =
    1.0 -
    smoothstep(
            -0.15,
            0.25,
            sunDir.y
    );

    vec3 daySky =
    getDaySkyColor(dir);

    vec3 nightSky =
    getNightSkyColor(dir);

    vec3 sky =
    mix(
            daySky,
            nightSky,
            nightFactor
    );


    // --------------------------------------------------------
    // SUN
    // --------------------------------------------------------

    float sun =
    getSunDisk(
            dir,
            time
    );

    vec3 sunColor =
    getSunColor(time);

    // Strong visible sun disk.
    sky +=
    sunColor
    * sun
    * 5.0;


    // --------------------------------------------------------
    // SUN GLOW
    // --------------------------------------------------------

    float sunDistance =
    max(
            dot(dir, sunDir),
            0.0
    );

    float sunGlow =
    pow(
            sunDistance,
            64.0
    );

    sky +=
    sunColor
    * sunGlow
    * 0.15;

    vec4 auroraColor =
    aurora(dir);

    sky += auroraColor.rgb;


    return vec4(
            sky,
            1.0
    );
}

void main() {
    if (meshType == 0u) {
        vec4 texColor = texture(blockTexture, TexCoord / TEXTURE_SIZE);

        texColor.rgb = pow(texColor.rgb, vec3(2.2));

        FragColor = texColor;
        if (FragColor.a == 0) discard;

        float distance = length(position - cameraPosition);
        float fogFactor = fogColor.a == 1 ? 1 : (fogFar - distance) / (fogFar - fogNear);
        fogFactor = clamp(fogFactor, 0.0, 1.0);
        if (distance > renderDistance) {
            discard;
        }

        vec4 lighting = pow(vLighting, vec4(2.2));
        lighting = 1 - pow(vec4(0.5), lighting);
        vec3 blockLight = lighting.rgb;
        float skyLight = lighting.a;
        float ambient = 0.05;

        vec4 skyColor = texture(skyTexture, gl_FragCoord.xy / screenResolution);

        vec3 sunDir = getSunDir(time);

        float NdotL = max(dot(normalize(faceNormal), sunDir), 0.0);

        float diffuse = mix(0.2, 1.0, NdotL);

        float directionalSky = skyLight * skyIntensity * diffuse;

        vec3 totalLight = blockLight + vec3(directionalSky);

        totalLight = max(totalLight, vec3(ambient));

        totalLight = pow(totalLight, vec3(1.2));

        FragColor.rgb *= totalLight;

        if (blockType == 1u) {
            float fresnel = 1 - abs(dot(vNormal, normalize(faceNormal)));
            FragColor.a = fresnel / 1.5 + 1 - 0.66666;
        }
        if (fogFactor < 1.0) {
            FragColor = mix(fogColor, FragColor, fogFactor);
        }
        FragColor.rgb = pow(FragColor.rgb, vec3(1.0 / 2.2));
        // TODO: Mix with filter color too for water and things.
    } else if (meshType == 1u) {
        FragColor = texture(blockTexture, TexCoord);
    } else if (meshType == 2u) {
        FragColor = skyColorMain();
    } else if (meshType == 3u) {
        FragColor = highlightColor;
    } else if (meshType == 4u) {
        FragColor = texture(skyTexture, skyUV);
    }
}
