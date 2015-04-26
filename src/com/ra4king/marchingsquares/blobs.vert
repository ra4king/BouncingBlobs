#version 330

layout(location = 0) in vec4 pos;

out vec2 coord;

void main() {
	coord = pos.xy;
	gl_Position = pos;
}
