package lancer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ ElementType.TYPE_USE, ElementType.LOCAL_VARIABLE })
@Retention(RetentionPolicy.RUNTIME)
public @interface HandleContract {

    // @HandleContract("_, _ -> #mask")
    // first '_' is a replacement for return type (it is skipped, so no arrow provided)
    // second '_' is a replacement for first arg by index 0,
    //      and it is replaced by object's, which is representing argument, field with name 'mask'
    //
    // # - DECLARES FIELD WHICH WILL BE USED
    // @ - DECLARES METHOD THAT WILL BE USED
    // ! - DECLARES STATIC MODIFIER

    String value() default "";
}
