package org.javarosa.benchmarks;

import static org.javarosa.benchmarks.BenchmarkUtils.dryRun;
import static org.javarosa.benchmarks.BenchmarkUtils.prepareAssets;

import java.io.IOException;
import java.nio.file.Path;
import org.javarosa.core.model.instance.ExternalDataInstance;
import org.javarosa.core.reference.InvalidReferenceException;
import org.javarosa.core.reference.ReferenceManagerTestUtils;
import org.javarosa.xml.util.InvalidStructureException;
import org.javarosa.xml.util.UnfullfilledRequirementsException;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;
import org.xmlpull.v1.XmlPullParserException;

public class ExternalDataInstanceBuildBenchmark {
    public static void main(String[] args) {
        dryRun(FormDefValidateBenchmark.class);
    }

    @State(Scope.Thread)
    public static class ExternalDataInstanceState {
        @Setup(Level.Trial)
        public void initialize() {
            Path assetsDir = prepareAssets("wards.xml");
            ReferenceManagerTestUtils.setUpSimpleReferenceManager("file", assetsDir);
        }
    }

    @Benchmark
    public void benchmark_ExternalDataInstance_build(ExternalDataInstanceState state, Blackhole bh)
        throws IOException, XmlPullParserException, InvalidReferenceException,
        UnfullfilledRequirementsException, InvalidStructureException {
        bh.consume(ExternalDataInstance.build("jr://file/wards.xml", "wards"));
    }

}
