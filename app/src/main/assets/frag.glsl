#extension GL_OES_EGL_image_external : require

precision mediump float;


uniform float imageWHRatio;
uniform int imageType;
uniform samplerExternalOES camTex;
varying vec2 camTexCoordinate;

int shouldSKip(const float _y){

    int value = int(_y * 100.0);
    if(value > 0 && value < 5||
    value > 10 && value < 15||
    value > 20 && value < 25||


    value > 30 && value < 35||
    value > 40 && value < 45 ||
    value > 50 && value < 55 ||
    value >= 60 && value < 65 ||
    value > 70 && value < 75||
    value > 80 && value < 85 ||
    value > 90 && value < 95){
        return 1;
    }
    return 0;
}


void main () {
    vec4 color = texture2D(camTex, camTexCoordinate);
    if (imageType == 0){
        vec2 modCord = vec2(camTexCoordinate.x * imageWHRatio, camTexCoordinate.y);
        vec2 coord = modCord - vec2(0.5 * imageWHRatio, 0.5);//from [0,1] to [-0.5,0.5]
        if (length(coord) > 0.45)//outside of circle radius?
        discard;
        else if (length(coord) <= 0.45 && length(coord) > 0.4475) {
            gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0);
        } else {
            gl_FragColor = color;
        }

    } else {

        const float _ratio = 0.5;
        const float _heightDiscard = (1.0 - _ratio) / 2.0;

        if(camTexCoordinate.x < _heightDiscard ||
        _heightDiscard + _ratio + 0.002 < camTexCoordinate.x){
            discard;
        } else if(!( camTexCoordinate.x - 0.0025 <= _heightDiscard

        || _heightDiscard + _ratio - 0.0025 <= camTexCoordinate.x

        ) &&
        !(camTexCoordinate.y < 0.005 || camTexCoordinate.y > 0.995 )){
            gl_FragColor = color;
        } else if ( shouldSKip(camTexCoordinate.y) == 0 && shouldSKip(camTexCoordinate.x) == 0 ){
            gl_FragColor =  vec4(1.0, 0.0, 0.0, 1.0);
        } else {
            gl_FragColor = color;
        }

    }
}
