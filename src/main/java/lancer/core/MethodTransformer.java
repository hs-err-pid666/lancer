package lancer.core;

import lancer.ForeignHandle;
import lancer.ForeignInspector;
import org.kohsuke.MetaInfServices;

import javax.annotation.processing.Filer;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.*;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.classfile.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.instrument.ClassFileTransformer;
import java.lang.reflect.AccessFlag;
import java.nio.file.*;
import java.security.ProtectionDomain;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;

@MetaInfServices
public class MethodTransformer implements ClassFileTransformer {

    private final ExecutableElement method;

    private ForeignHandle handle;

    public MethodTransformer(ExecutableElement method) {
        this.method = method;
    }

    @SuppressWarnings("preview")
    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classfileBuffer) {
        AnnotationProcessor.env.getMessager().printMessage(Diagnostic.Kind.WARNING, "transforming " + className);

        return classfileBuffer;

        /*ClassFile cf = ClassFile.of();
        ClassModel classModel = cf.parse(classfileBuffer);

        return cf.build(classModel.thisClass().asSymbol(),
            classBuilder ->
        {
           for (ClassElement ce : classModel) {
                if (ce instanceof MethodModel mm && mm.flags().has(AccessFlag.NATIVE)) {
                    var mmFlags = mm.flags();
                    int mask = 0;
                    for (var flag : mmFlags.flags() )
                        if (flag != AccessFlag.NATIVE)
                            mask |= flag.mask();

                    classBuilder.withMethod(mm.methodName(), mm.methodType(), mask,
                        methodBuilder ->
                    {
                        for (MethodElement me : mm) {
                            if (me instanceof CodeModel codeModel) {
                                methodBuilder.withCode(codeBuilder -> {
                                    codeBuilder.iconst_0();
                                    codeBuilder.iconst_1();
                                    codeBuilder.iconst_2();
                                });
                            }
                            else
                                methodBuilder.with(me);
                        }
                    });
                }
                else
                    classBuilder.with(ce);
            }
        });*/
    }

    public void transform(Annotation annotation) {
        switch (annotation) {
            case ForeignHandle h -> {
                this.handle = h;
                generateLibraryExecutor();
            }
            case ForeignInspector _ -> throw new UnsupportedOperationException("Not supported.");
            default -> { }
        }
    }

    private void generateLibraryExecutor() {
        // Получаем класс, в котором объявлен метод
        TypeElement enclosingClass = (TypeElement) this.method.getEnclosingElement();

        String methodName = this.method.getSimpleName().toString();
        String className = this.method.getEnclosingElement().asType().toString();

        AnnotationProcessor.env.getMessager().printMessage(Diagnostic.Kind.WARNING, "assume " + className);

        // TODO: изменять класс здеся:
        Filer filer = AnnotationProcessor.env.getFiler();
        try {
            FileObject openglClass = filer.getResource(
                    StandardLocation.CLASS_OUTPUT,
                    "net.tokyolancer.graphics.opengl32", "OpenGL.class"
            );

            JavaFileObject object = AnnotationProcessor.env.getElementUtils().getFileObjectOf(
                this.method.getEnclosingElement()
            );

            AnnotationProcessor.warn("prepare " + openglClass);
            if (openglClass instanceof ForwardingFileObject<?> ffo) {
                final Path pathToClass = Paths.get(ffo.toUri() );

                AnnotationProcessor.warn("ffo: " + pathToClass);

                Path outputDir = pathToClass;
                for (int i = 0; i < className.length() -
                        className.replace(".", "").length(); i++) {
                    // reverse-back
                    outputDir = outputDir.getParent();
                }

                AnnotationProcessor.warn("output_dir: " + outputDir.getParent());

                WatchService watchService = FileSystems.getDefault().newWatchService();
                outputDir.getParent().register(watchService, ENTRY_CREATE);
                Thread.ofPlatform()
//                        .stackSize(256L)
                        .name("Lancer-Clipper")
                        .unstarted(() -> {
                            WatchKey key;

                            outer: while (!Thread.interrupted() ) {
                                key = watchService.poll();

                                while (key == null) {
                                    key = watchService.poll();
                                    Thread.onSpinWait();
                                }

                                for (var event : key.pollEvents() ) {
                                    if (event.kind() == ENTRY_CREATE) {
                                        Path name = (Path) event.context();
                                        if ("OpenGL.class".equals(name.toString() ) )
                                            break outer;
                                    }
                                }
                                if (!key.reset() )
                                    break;
                            }

                            ClassFile cf = ClassFile.of();
                            try {
                                ClassModel cm = cf.parse(pathToClass);
                                Files.write(pathToClass,
                                    cf.build(
                                        cm.thisClass().asSymbol(),
                                        cb -> {
                                            for (ClassElement ce : cm) {
                                                if (ce instanceof MethodModel mm && mm.flags().has(AccessFlag.NATIVE) ) {
                                                    // do nothing
                                                }
                                                else
                                                    cb.with(ce);
                                            }
                                            cb.withField("TRXAT", ConstantDescs.CD_Double, AccessFlag.PUBLIC.mask() );
                                        }
                                    ),
                                    StandardOpenOption.WRITE
                                );
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        });
            }

            JavaFileObject testObject = AnnotationProcessor.env.getFiler()
                    .createClassFile("net.tokyolancer.graphics.opengl32.Abrakadabra");

            ClassFile cf = ClassFile.of();
            byte[] bytes = cf.build(
                ClassDesc.of("net.tokyolancer.graphics.opengl32.Abrakadabra"),
                    cb ->
                            cb.withField("TRXAT", ConstantDescs.CD_Double, AccessFlag.PUBLIC.mask() )
            );

            try (var writer = testObject.openOutputStream() ) {
//                writer.write("DaYN");
                writer.write(bytes);
                writer.flush();
            }

//            AnnotationProcessor.warn("path " + openglClass.toUri().getPath() + ".class");
//
//            ClassFile cf = ClassFile.of();
//            ClassModel cm = cf.parse(Path.of(openglClass.toUri().getPath() + ".class"));
//
//            try (var writer = object.openWriter() ) {
//                writer.write("yeeeesd");
//                writer.flush();
//            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
//        JavaFileObject classFileSource = AnnotationProcessor.env.getElementUtils().getFileObjectOf(enclosingClass);
    }
}
