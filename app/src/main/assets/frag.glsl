#extension GL_OES_EGL_image_external : require

precision mediump float;


uniform float imageWHRatio;
uniform int imageType;
uniform samplerExternalOES camTex;
varying vec2 camTexCoordinate;

void main () {
    vec4 color = texture2D(camTex, camTexCoordinate);
    if(imageType == 0){
        vec2 modCord = vec2(camTexCoordinate.x * imageWHRatio, camTexCoordinate.y);
        vec2 coord = modCord - vec2(0.5 * imageWHRatio, 0.5 );  //from [0,1] to [-0.5,0.5]
        if(length(coord) > 0.45)                  //outside of circle radius?
        discard;
    } else {

    }



    gl_FragColor = color;
}
