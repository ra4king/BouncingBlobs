#version 330

struct Circle {
	vec2 center;
	float radius;
};

#define MAX_CIRCLES 100

layout(std140) uniform Circles {
	Circle circles[MAX_CIRCLES];
	int num_circles;
};

uniform bool showCircles = false;

const float thickness = 0.1;

in vec2 coord;

out vec4 fragColor;

void main() {
	fragColor = vec4(0.0, 0.0, 0.0, 1.0);
	
	float sum = 0.0;
	
	for(int i = 0; i < MAX_CIRCLES; i++) {
		Circle c = circles[i];
		vec2 dist = c.center - coord;
		float r = c.radius * c.radius;
		float d = dot(dist, dist);
		float f = r / d; 
		
		float isC = (1.0 - step(float(num_circles), float(i)));
		sum += isC * f;
		fragColor.xyz += int(showCircles) * isC * vec3(1.0, 0.0, 0.0) * (1.0 - step(1.0 + thickness, f)) * step(1.0, f);
	}
	
	fragColor.xyz += vec3(0.0, 1.0, 0.0) * step(1.0, sum);
}
