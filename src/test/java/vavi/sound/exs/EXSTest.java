/*
 * https://github.com/cldmnky/exsconvert/blob/main/pkg/exs/exs_test.go
 */

package vavi.sound.exs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;


/**
 * EXSTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-04-10 nsano initial version <br>
 */
class EXSTest {

    @ParameterizedTest
    @ValueSource(strings = {
            "/exs/MC-202 bass.exs",
            "/exs/Big News (slow sweeps).exs",
            "/exs/K3 Big.exs",
            "/exs/filter-DFAM-WFM-LP.exs",
            "/exs/Shape-DFAM-PSEQOUT.exs",
            "/exs/Hi Hat 909 Clean.exs"
    })
    void test0(String filename) throws Exception {
        EXS exs = EXS.newFromByteArray(Path.of(EXSTest.class.getResource(filename).toURI()));
    }

    /** .exs files */
    static Stream<Arguments> sources() throws IOException {
        return Files.walk(Path.of("/Library/Application Support/GarageBand/Instrument Library/Sampler/Sampler Instruments"))
                .filter(p -> p.toString().endsWith(".exs"))
                .map(Arguments::arguments);
    }

    @ParameterizedTest
    @MethodSource("sources")
    void test1(Path path) throws Exception {
        EXS exs = EXS.newFromByteArray(path);
    }
}
