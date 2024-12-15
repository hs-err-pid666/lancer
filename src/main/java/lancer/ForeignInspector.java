package lancer;

import static lancer.ForeignMode.NATIVE;

/**
 *
 *  Indicates that the class marked with this annotation has specific settings for foreign methods.
 *
 */
public @interface ForeignInspector {

    @Deprecated
    ForeignMode mode() default NATIVE;

    // prefix before each ForeignHandle
    //  Example:
    //      - prefix: "glfw"
    //      - native func: \glfwInit()          @transcript: [ void.class ]
    //      - java func: \init() or \Init()  -->  transformed to \glfwInit()
    String hprefix() default "";
}
