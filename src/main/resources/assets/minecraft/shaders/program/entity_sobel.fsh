#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
in vec2 oneTexel;

out vec4 fragColor;

// outlines are normally drawn 1 texel either side of an entity's edge. Entities tagged with the marker colour
// set in EntityMixin#getTeamColor (0xFFFFFE) also get an extra ring this many texels outside their edge.
const float THICK_OFFSET = 2.0;

bool isThick(vec4 c) {
    return c.a > 0.5 && c.r > 0.99 && c.g > 0.99 && abs(c.b - 254.0 / 255.0) < 0.002;
}

void main(){
    vec4 center = texture(DiffuseSampler, texCoord);
    vec4 left = texture(DiffuseSampler, texCoord - vec2(oneTexel.x, 0.0));
    vec4 right = texture(DiffuseSampler, texCoord + vec2(oneTexel.x, 0.0));
    vec4 up = texture(DiffuseSampler, texCoord - vec2(0.0, oneTexel.y));
    vec4 down = texture(DiffuseSampler, texCoord + vec2(0.0, oneTexel.y));
    float leftDiff  = abs(center.a - left.a);
    float rightDiff = abs(center.a - right.a);
    float upDiff    = abs(center.a - up.a);
    float downDiff  = abs(center.a - down.a);
    float total = clamp(leftDiff + rightDiff + upDiff + downDiff, 0.0, 1.0);

    // vanilla blurs this result afterwards, which averages the colour out; with no blur we normalise by
    // the contributing alpha instead so the outline keeps its full colour
    vec3 outColor = center.rgb * center.a + left.rgb * left.a + right.rgb * right.a + up.rgb * up.a + down.rgb * down.a;
    float outAlpha = center.a + left.a + right.a + up.a + down.a;
    vec3 color = outAlpha > 0.0 ? outColor / outAlpha : vec3(0.0);

    // extra outer ring for marked entities
    if (center.a == 0.0) {
        vec2 offset = oneTexel * THICK_OFFSET;
        vec4 left2 = texture(DiffuseSampler, texCoord - vec2(offset.x, 0.0));
        vec4 right2 = texture(DiffuseSampler, texCoord + vec2(offset.x, 0.0));
        vec4 up2 = texture(DiffuseSampler, texCoord - vec2(0.0, offset.y));
        vec4 down2 = texture(DiffuseSampler, texCoord + vec2(0.0, offset.y));
        vec3 thickColor = vec3(0.0);
        float thickCount = 0.0;
        if (isThick(left2))  { thickColor += left2.rgb;  thickCount += 1.0; }
        if (isThick(right2)) { thickColor += right2.rgb; thickCount += 1.0; }
        if (isThick(up2))    { thickColor += up2.rgb;    thickCount += 1.0; }
        if (isThick(down2))  { thickColor += down2.rgb;  thickCount += 1.0; }
        if (thickCount > 0.0) {
            if (total == 0.0) {
                color = thickColor / thickCount;
            }
            total = 1.0;
        }
    }

    fragColor = vec4(color, total);
}
