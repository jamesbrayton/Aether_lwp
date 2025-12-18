// Vertex shader for fullscreen quad rendering
// Used by all particle effect fragment shaders
// Maps a simple quad (-1 to 1 in x,y) to cover the entire screen

attribute vec4 a_position;

void main() {
    gl_Position = a_position;
}
