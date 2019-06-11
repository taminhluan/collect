package org.javarosa.benchmarks;

import static org.javarosa.test.utils.ResourcePathHelper.r;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.stream.Stream;
import org.javarosa.core.model.QuestionDef;
import org.javarosa.core.model.data.IAnswerData;
import org.javarosa.core.model.data.LongData;
import org.javarosa.core.model.data.StringData;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

public class BenchmarkUtils {
    public static Path prepareAssets(String... filenames) {
        try {
            Path assetsDir = Files.createTempDirectory("javarosa_benchmarks_");
            for (String filename : filenames) {
                String realPath = BenchmarkUtils.class
                    .getResource(filename.startsWith("/") ? filename : "/" + filename)
                    .toURI().toString();
                Files.copy(
                    realPath.contains("!") ? getPathInJar(realPath) : r(filename),
                    assetsDir.resolve(filename)
                );
            }
            return assetsDir;
        } catch (IOException | URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private static Path getPathInJar(String realPath) {
        Path sourcePath;
        try {
            String[] parts = realPath.split("!");
            String jarPart = parts[0];
            String filePart = parts[1];
            sourcePath = getFileSystem(URI.create(jarPart)).getPath(filePart);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sourcePath;
    }

    private static FileSystem getFileSystem(URI jarUri) throws IOException {
        FileSystem fileSystem;
        try {
            fileSystem = FileSystems.getFileSystem(jarUri);
        } catch (FileSystemNotFoundException e) {
            fileSystem = FileSystems.newFileSystem(jarUri, new HashMap<String, String>());
        }
        return fileSystem;
    }

    private static Blackhole getBlackhole() {
        return new Blackhole("Today's password is swordfish. I understand instantiating Blackholes directly is dangerous.");
    }

    /**
     * This method will run all methods annotated with @Benchmark declared in the provided class.
     * <p>
     * This method uses reflection to provide all the required params.
     */
    @SuppressWarnings("unchecked")
    public static void dryRun(Class clazz) {
        Stream<Method> methodsWithAnnotation = getMethodsWithAnnotation(clazz, Benchmark.class);
        methodsWithAnnotation.forEach(method -> {
            try {
                Object[] paramValues = new Object[method.getParameterCount()];
                int i = 0;
                for (Class paramType : method.getParameterTypes())
                    if (paramType.equals(Blackhole.class))
                        paramValues[i++] = getBlackhole();
                    else {
                        Object stateInstance = paramType.getConstructor().newInstance();
                        if (hasAnnotation(paramType, State.class)) {
                            getMethodsWithAnnotation(paramType, Setup.class)
                                .findFirst()
                                .orElseThrow(RuntimeException::new)
                                .invoke(stateInstance);
                        }
                        paramValues[i++] = stateInstance;
                    }
                Object instance = clazz.newInstance();
                method.invoke(instance, paramValues);
            } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        });

    }

    private static boolean hasAnnotation(Class<?> paramType, Class<? extends Annotation> annotationType) {
        return Stream.of(paramType.getDeclaredAnnotations()).anyMatch(a -> a.annotationType().equals(annotationType));
    }

    private static Stream<Method> getMethodsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotationClass) {
        return Stream.of(clazz.getDeclaredMethods())
            .filter(paramTypeMethod -> paramTypeMethod.isAnnotationPresent(annotationClass));
    }

    public static IAnswerData getStubAnswer(QuestionDef question) {
        switch (question.getLabelInnerText()) {
            case "State":
                return new StringData("7b0ded95031647702b8bed17dce7698a"); // Abia
            case "LGA":
                return new StringData("6fa741c46485b9c618f14b79edf50e88"); // Aba North
            case "Ward":
                return new StringData("90fa443787485709a5b11c5f7925fb71"); // Ariaria
            case "Comments":
                return new StringData("No Comment");
            case "What population do you want to search for?":
                return new LongData(699967);
            default:
                return new StringData("");
        }
    }
}
