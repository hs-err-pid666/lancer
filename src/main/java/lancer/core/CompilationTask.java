package lancer.core;

import lancer.ForeignDescriptor;
import lancer.ForeignHandle;

import javax.lang.model.element.ExecutableElement;
import java.io.IOException;
import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.util.List;

import static java.lang.classfile.ClassFile.LineNumbersOption.PASS_LINE_NUMBERS;

public record CompilationTask(Path pathToClass,
                              ExecutableElement executable,
                              ForeignHandle handle) implements Runnable {

    private static final String METHOD_DESC = "net.tokyolancer.graphics.opengl32.experimental.MethodDescriptor";

    @Override
    public void run() {
        try {
            resolve();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void resolve() throws IOException {
        ClassFile cf = ClassFile.of(PASS_LINE_NUMBERS);
        ClassModel cm = cf.parse(this.pathToClass);
        cf.buildTo(pathToClass(), cm.thisClass().asSymbol(),
            b -> {
                for (ClassElement ce : cm) {
                    if (!(ce instanceof MethodModel method) )
                         continue;

                    if (!method.flags().has(AccessFlag.NATIVE) )
                        continue;

                    if (!method.methodName().equalsString(
                            executable().getSimpleName().toString() ) )
                        continue;

                    b.transformMethod(method, (builder, _) -> {
                        int mask = 0;
                        for (var flag : method.flags().flags() )
                            if (flag != AccessFlag.NATIVE)
                                mask |= flag.mask();

                        builder.withFlags(mask)
                               .withCode(code -> {
                            // pushing names for the descriptor
                            code.ldc(code.constantPool().utf8Entry(handle().name() ).constantValue() );
                            code.ldc(code.constantPool().utf8Entry(handleParameters() ).constantValue() );

                            // preparing descriptor
                            code.invokestatic(
                                ClassDesc.of(METHOD_DESC),
                                "of",
                                MethodTypeDesc.of(
                                    ClassDesc.of(METHOD_DESC),
                                    List.of(
                                        ClassDesc.of("java.lang.String"),
                                        ClassDesc.of("java.lang.String")
                                    )
                                )
                            );

                            // store return value on stack
                            code.astore(2);

                            // load *this*
                            code.aload(0);

                            // previously returned value load
                            code.aload(2);

                            // provided argument #1 load
                            code.aload(1);

                            // calling function
                            code.invokespecial(
                                ClassDesc.of("net.tokyolancer.graphics.SystemLibrary"),
                                "callAssembly",
                                MethodTypeDesc.of(
                                    ClassDesc.of("java.lang.Object"),
                                    List.of(
                                        ClassDesc.of(METHOD_DESC),
                                        ClassDesc.of("java.lang.Object")
                                    )
                                )
                            );

                            // returning segment
                            code.areturn();
                        });
                    });
                }
            }
        );
    }

    private String handleParameters() {
        StringBuilder builder = new StringBuilder();
        for (var descriptor : handle().descriptor() ) {
            builder.append(enumToString(descriptor) );
        }
        return builder.toString();
    }

    private static String enumToString(ForeignDescriptor descriptor) {
        switch (descriptor) {
            case INT -> {
                return "I";
            }
            case POINTER -> {
                return "P";
            }
            default -> {
                return null;
            }
        }
    }
}
