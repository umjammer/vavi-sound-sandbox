/*
 * https://github.com/cldmnky/exsconvert/blob/main/pkg/exs/exs_test.go
 */

package vavi.sound.exs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import vavi.util.Debug;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;


/**
 * EXSTest.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2025-04-10 nsano initial version <br>
 * @see "https://github.com/git-moss/ConvertWithMoss"
 */
class EXSTest {

    static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

    // real samples are in "/Library/Application Support/GarageBand/Instrument Library/Sampler/Sampler Files"

    /** .exs files */
    static Stream<Arguments> sources() throws IOException {
        return Files.walk(Path.of("/Library/Application Support/GarageBand/Instrument Library/Sampler/Sampler Instruments"))
                .filter(p -> p.toString().endsWith(".exs"))
                .map(Arguments::arguments);
    }

    @ParameterizedTest
    @EnabledIfSystemProperty(named = "vavi.test", matches = "ide")
    @MethodSource("sources")
    void test1(Path path) throws Exception {
        EXS exs = EXS.newFromByteArray(path);
String json = gson.toJson(exs);
        Debug.println(json);
    }

    @Test
    void test2() throws Exception {
        EXS exs = EXS.newFromByteArray(Path.of(EXSTest.class.getResource("/exs/MC-202 bass.exs").toURI()));
        String json = gson.toJson(exs);
Debug.println(json);
    }
}
