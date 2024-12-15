package lancer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import static lancer.ForeignDescriptor.VOID;

/**
 *
 *  Specifies which foreign function the current method subscribes to.
 *
 */
@Target({ ElementType.METHOD })
//@Retention(RetentionPolicy.SOURCE)
public @interface ForeignHandle {

    // by default - empty string, which imitates the same name as subscribed method
    String name() default "";

    // by default - method with no return value and has no args
    ForeignDescriptor[] descriptor() default { VOID };

    // == ForeignLibrary
    @Deprecated
    String executorName() default "";

    // if defined - then function from ForeignLibrary (executor) with provided name
    // will accept a result of native method invocation, and
    // return the reinterpreted result
    @Deprecated
    String interpreterName() default "";
}
