package lancer.core;

import lancer.ForeignDescriptor;
import lancer.ForeignHandle;
import org.kohsuke.MetaInfServices;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementKindVisitor14;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

@MetaInfServices
@SupportedAnnotationTypes({"lancer.ForeignHandle"})
public class AnnotationProcessor implements Processor {

    private static final String METHOD_DESC = "net.tokyolancer.graphics.opengl32.experimental.MethodDescriptor";

    public static ClassLoader loader;
    public static ProcessingEnvironment env;

    static class Visitor extends ElementKindVisitor14<MethodTransformer, Void> {

        @Override
        public MethodTransformer visitExecutableAsMethod(ExecutableElement element, Void v) {
            return new MethodTransformer(element);
        }
    }

    public static void warn(String msg) {
        env.getMessager().printWarning(msg);
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        env = processingEnv;
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Hello, World!");
        loader = processingEnv.getClass().getClassLoader();
    }

    private Stream<Runnable> callers;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment env) {
        if (annotations.isEmpty() )
            return false;

        Set<? extends Element> handles = env.getElementsAnnotatedWith(ForeignHandle.class);

        var items = new HashMap<Element, ForeignHandle>();

        for (var handle : handles) {
            warn(handle.getSimpleName().toString() );

            handle.accept(new Visitor(), null)
                    .transform(handle.getAnnotation(ForeignHandle.class));

            items.put(handle, handle.getAnnotation(ForeignHandle.class) );
        }

        final Path pathToClass = Path.of("E:\\IntelliJ IDEA 2024.1.1\\projects\\OpenGL32\\target\\classes\\net\\tokyolancer\\graphics\\opengl32\\OpenGL.class");

        var list = new ArrayList<CompilationTask>();
        list.add(
            new CompilationTask(null, null, null)
        );
        list.trimToSize();

        warn(this.getClass().getClassLoader().toString() );

        Runtime.getRuntime().addShutdownHook(
            Thread.ofPlatform()
                .stackSize(256L)
                .name("Lancer-Compiler")
                .unstarted(() -> {
                    ClassFile cf = ClassFile.of(ClassFile.LineNumbersOption.PASS_LINE_NUMBERS);

                    System.out.println(new CompilationTask(null, null, null));
                    System.out.println(list);

                    try {
                        ClassModel cm = cf.parse(pathToClass);
                        cf.buildTo(pathToClass,
                            cm.thisClass().asSymbol(),
                            cb -> {
                                for (ClassElement ce : cm) {
                                    if (ce instanceof MethodModel mm && mm.flags().has(AccessFlag.NATIVE) ) {
                                        // do nothing
                                        int mask = 0;
                                        for (var flag : mm.flags().flags() )
                                            if (flag != AccessFlag.NATIVE && flag != AccessFlag.STATIC)
                                                mask |= flag.mask();
                                        final int fmask = mask;

                                        cb.transformMethod(mm, (c, _) -> {
                                            c.withFlags(fmask)
                                                .withCode(code -> {
                                                    // todo: declarations must be retrieved via Handle annotation
                                                    code.ldc(code.constantPool().utf8Entry("glGetString").constantValue() );
                                                    code.ldc(code.constantPool().utf8Entry("PI").constantValue() );
                                                    // calling function
                                                    code.invokestatic(
                                                            ClassDesc.of("net.tokyolancer.graphics.opengl32.experimental.MethodDescriptor"),
                                                            "of",
                                                            MethodTypeDesc.of(
                                                                    ClassDesc.of("net.tokyolancer.graphics.opengl32.experimental.MethodDescriptor"),
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
                                                                            ClassDesc.of("net.tokyolancer.graphics.opengl32.experimental.MethodDescriptor"),
                                                                            ClassDesc.of("java.lang.Object")
                                                                    )
                                                            )
                                                    );
                                                    // returning segment
                                                    code.areturn();
                                                });
                                        });
                                    }
                                    else
                                        cb.with(ce);
                                }
                            }
                        );
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println("Get out.");
                })
        );

        return true;
    }

    public static String handleParameters(ForeignHandle handle) {
        StringBuilder builder = new StringBuilder();
        for (var descriptor : handle.descriptor() ) {
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

    @Override
    public Iterable<? extends Completion> getCompletions(Element element, AnnotationMirror annotation, ExecutableElement member, String userText) {
        return null;
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Set.of();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Set.of(ForeignHandle.class.getName() );
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }
}
