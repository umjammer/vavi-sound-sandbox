/*
 * Copyright (c) 2026 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavi.sound.sampled.misc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.function.Predicate;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioFormat.Encoding;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.getLogger;
import static javax.sound.sampled.AudioSystem.NOT_SPECIFIED;


/**
 * A {@link FormatConversionProvider} that wires the other registered providers together.
 * <p>
 * The java sound spi only ever asks a single provider to perform a conversion, so a
 * conversion that no provider can do alone fails, even when a combination of two of them
 * could do it. The classic example is a monaural mp3: {@code mp3spi} decodes mp3 but does
 * not change the channel count, {@code PCM2PCMConversionProvider} changes the channel
 * count but does not decode mp3, hence
 * <pre>
 *  mp3 mono -&gt; stereo pcm ... fails
 *  mp3 mono -(mp3spi)-&gt; pcm mono -(PCM2PCM)-&gt; pcm stereo ... works, but has to be wired by hand
 * </pre>
 * This provider does that wiring automatically, so the first line works as well.
 * <p>
 * How it works: the registered providers and the formats they advertise form a graph whose
 * nodes are {@link AudioFormat}s and whose edges are conversions. A breadth first search
 * over that graph looks for the shortest chain from the source format to the target format,
 * then {@link #getAudioInputStream(AudioFormat, AudioInputStream)} replays that chain by
 * feeding each intermediate format back into {@link AudioSystem}.
 * <p>
 * Two things make the graph awkward to walk and are dealt with in {@link #candidates}:
 * <ul>
 *  <li>the formats returned by {@link #getTargetFormats} are usually wildcards, i.e. their
 *      sample rate / sample size / channel count is {@link AudioSystem#NOT_SPECIFIED}. Such a
 *      format is not usable as an intermediate as is, so its wildcards are materialized from
 *      the source format and from the ultimate target format.</li>
 *  <li>some providers advertise nothing useful at all. Plain pcm intermediates are therefore
 *      always tried, they are the pivot almost every chain goes through anyway.</li>
 * </ul>
 * Advertisement is only used to <em>propose</em> an intermediate format; every edge is then
 * verified with {@link AudioSystem#isConversionSupported(AudioFormat, AudioFormat)}, which is
 * the authoritative answer. It is only as good as the providers are honest though, and they
 * are not always: a provider claiming a conversion it then refuses to perform is a thing that
 * happens. A chain is therefore never longer than it has to be, so that such a provider has
 * as few chances as possible to end up in one.
 * <p>
 * This provider is strictly additive: when the requested conversion is already possible with
 * a single provider, {@link #isConversionSupported(AudioFormat, AudioFormat)} returns false
 * and lets that provider do it. It only ever kicks in where the conversion would otherwise
 * have thrown.
 * <p>
 * Since this provider is registered itself, every call it makes into {@link AudioSystem}
 * would come back to it. A thread local flag blocks that recursion, the same trick
 * {@code org.tritonus.sampled.convert.SmartFormatConversionProvider} uses (it does so with a
 * set of threads, which is the same thing written by hand).
 * <p>
 * System properties:
 * <ul>
 *  <li>{@code vavi.sound.sampled.spi.autowired} ... {@code false} disables this provider
 *      entirely (default {@code true})</li>
 *  <li>{@code vavi.sound.sampled.spi.autowired.max} ... max number of converters in a chain
 *      (default {@code 3}), {@code 1} means no chaining at all</li>
 * </ul>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 260726 nsano initial version <br>
 */
public class AutoWiredFormatConversionProvider extends FormatConversionProvider {

    private static final System.Logger logger = getLogger(AutoWiredFormatConversionProvider.class.getName());

    /** max number of intermediate formats proposed for a single search node */
    private static final int MAX_CANDIDATES = 256;

    /** max number of edges verified during a single search, keeps a hopeless search short */
    private static final int MAX_EDGES = 20000;

    /**
     * Whether the current thread is already inside this provider.
     * <p>
     * The search calls {@link AudioSystem}, which asks every registered provider, this one
     * included. Without this flag that would recurse forever. Every method that is part of
     * the spi returns "not supported" while the flag is set, so from the point of view of
     * the search this provider does not exist.
     */
    private static final ThreadLocal<Boolean> blocked = ThreadLocal.withInitial(() -> false);

    /** the registered providers except for this one, lazily initialized */
    private static volatile List<FormatConversionProvider> providers;

    /** whether this provider is turned on */
    private static boolean enabled() {
        return Boolean.parseBoolean(System.getProperty("vavi.sound.sampled.spi.autowired", "true"));
    }

    /** max number of converters in a chain, 1 means no chaining at all */
    private static int maxChain() {
        return Integer.parseInt(System.getProperty("vavi.sound.sampled.spi.autowired.max", "3"));
    }

    /** whether this provider must keep quiet, either because it is off or to avoid recursion */
    private static boolean inactive() {
        return !enabled() || blocked.get();
    }

    /**
     * All the registered providers except for this one. {@link AudioSystem} does not expose
     * its own list, so it is rebuilt here. Only used to propose intermediate formats, the
     * conversions themselves always go through {@link AudioSystem}.
     */
    private static List<FormatConversionProvider> providers() {
        if (providers == null) {
            List<FormatConversionProvider> ps = new ArrayList<>();
            try {
                for (FormatConversionProvider p : ServiceLoader.load(FormatConversionProvider.class)) {
                    if (!(p instanceof AutoWiredFormatConversionProvider)) {
                        ps.add(p);
                    }
                }
            } catch (ServiceConfigurationError e) {
logger.log(DEBUG, e.toString());
            }
            providers = ps;
        }
        return providers;
    }

    @Override
    public Encoding[] getSourceEncodings() {
        if (inactive()) {
            return new Encoding[0];
        }
        Set<Encoding> result = new LinkedHashSet<>();
        for (FormatConversionProvider provider : providers()) {
            result.addAll(List.of(provider.getSourceEncodings()));
        }
        return result.toArray(Encoding[]::new);
    }

    @Override
    public Encoding[] getTargetEncodings() {
        if (inactive()) {
            return new Encoding[0];
        }
        Set<Encoding> result = new LinkedHashSet<>();
        for (FormatConversionProvider provider : providers()) {
            result.addAll(List.of(provider.getTargetEncodings()));
        }
        return result.toArray(Encoding[]::new);
    }

    @Override
    public Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (inactive()) {
            return new Encoding[0];
        }
        Set<Encoding> result = new LinkedHashSet<>();
        for (AudioFormat format : reachable(sourceFormat)) {
            Encoding encoding = format.getEncoding();
            if (!result.contains(encoding) && !supported(encoding, sourceFormat)) {
                result.add(encoding);
            }
        }
        return result.toArray(Encoding[]::new);
    }

    @Override
    public AudioFormat[] getTargetFormats(Encoding targetEncoding, AudioFormat sourceFormat) {
        if (inactive()) {
            return new AudioFormat[0];
        }
        return reachable(sourceFormat).stream()
                .filter(f -> f.getEncoding().equals(targetEncoding))
                .filter(f -> !supported(f, sourceFormat))
                .toArray(AudioFormat[]::new);
    }

    @Override
    public boolean isConversionSupported(Encoding targetEncoding, AudioFormat sourceFormat) {
        if (inactive()) {
            return false;
        }
        // when a single provider can do it, stay out of the way
        if (supported(targetEncoding, sourceFormat)) {
            return false;
        }
        return search(sourceFormat, null, f -> supported(targetEncoding, f)) != null;
    }

    @Override
    public boolean isConversionSupported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        if (inactive()) {
            return false;
        }
        // when a single provider can do it, stay out of the way
        if (supported(targetFormat, sourceFormat)) {
            return false;
        }
        return wire(sourceFormat, targetFormat) != null;
    }

    @Override
    public AudioInputStream getAudioInputStream(Encoding targetEncoding, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        List<AudioFormat> chain = inactive() ? null : search(sourceFormat, null, f -> supported(targetEncoding, f));
        if (chain == null) {
            throw new IllegalArgumentException("unable to wire " + sourceFormat + " to " + targetEncoding);
        }
logger.log(DEBUG, "auto wired: " + sourceFormat + " -> " + chain + " -> " + targetEncoding);
        boolean prev = blocked.get();
        blocked.set(true);
        try {
            AudioInputStream stream = sourceStream;
            for (AudioFormat format : chain) {
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            return AudioSystem.getAudioInputStream(targetEncoding, stream);
        } finally {
            blocked.set(prev);
        }
    }

    @Override
    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {
        AudioFormat sourceFormat = sourceStream.getFormat();
        List<AudioFormat> chain = inactive() ? null : wire(sourceFormat, targetFormat);
        if (chain == null) {
            throw new IllegalArgumentException("unable to wire " + sourceFormat + " to " + targetFormat);
        }
logger.log(DEBUG, "auto wired: " + sourceFormat + " -> " + chain);
        boolean prev = blocked.get();
        blocked.set(true);
        try {
            AudioInputStream stream = sourceStream;
            for (AudioFormat format : chain) {
                stream = AudioSystem.getAudioInputStream(format, stream);
            }
            return stream;
        } finally {
            blocked.set(prev);
        }
    }

    // search

    /**
     * Looks for a chain of conversions from {@code sourceFormat} to {@code targetFormat}.
     *
     * @return the formats to convert to, in order, the last one being {@code targetFormat};
     *         its size is the number of converters in the chain and is always {@code >= 2}.
     *         null when no chain was found
     */
    static List<AudioFormat> wire(AudioFormat sourceFormat, AudioFormat targetFormat) {
        List<AudioFormat> intermediates = search(sourceFormat, targetFormat, f -> supported(targetFormat, f));
        if (intermediates == null) {
            return null;
        }
        List<AudioFormat> chain = new ArrayList<>(intermediates);
        chain.add(targetFormat);
        return chain;
    }

    /**
     * Breadth first search over the format graph.
     *
     * @param sourceFormat where to start
     * @param targetFormat the ultimate target, only used to materialize wildcard formats.
     *                     null when the target is an {@link Encoding} rather than a format
     * @param goal tells whether the last conversion of the chain is possible from the given
     *             format. that last conversion is not part of the returned list, so a
     *             returned list of n formats means a chain of n + 1 converters
     * @return the intermediate formats to convert to, in order, never empty. null when no
     *         chain was found
     */
    private static List<AudioFormat> search(AudioFormat sourceFormat, AudioFormat targetFormat, Predicate<AudioFormat> goal) {
        return walk(sourceFormat, targetFormat, goal, maxChain() - 1, null);
    }

    /**
     * Every format this provider can produce out of {@code sourceFormat}, i.e. the formats
     * the search can reach within {@link #maxChain()} converters.
     */
    private static Collection<AudioFormat> reachable(AudioFormat sourceFormat) {
        Map<String, AudioFormat> reached = new LinkedHashMap<>();
        walk(sourceFormat, null, f -> false, maxChain(), reached);
        return reached.values();
    }

    /**
     * The breadth first search itself, shared by {@link #search} and {@link #reachable}.
     *
     * @param maxDepth max length of the returned path
     * @param reached when not null, gets filled with every format the walk reached
     * @return the path to the first format from which {@code goal} holds, null when there is none
     */
    private static List<AudioFormat> walk(AudioFormat sourceFormat, AudioFormat targetFormat,
                                          Predicate<AudioFormat> goal, int maxDepth,
                                          Map<String, AudioFormat> reached) {
        if (maxDepth < 1) {
            return null;
        }
        Set<String> seen = new HashSet<>();
        seen.add(key(sourceFormat));
        Deque<List<AudioFormat>> queue = new ArrayDeque<>();
        queue.add(List.of());
        int edges = 0;
        boolean prev = blocked.get();
        blocked.set(true);
        try {
            while (!queue.isEmpty()) {
                List<AudioFormat> path = queue.poll();
                if (path.size() >= maxDepth) {
                    continue;
                }
                AudioFormat from = path.isEmpty() ? sourceFormat : path.get(path.size() - 1);
                for (AudioFormat candidate : candidates(from, targetFormat)) {
                    String key = key(candidate);
                    // only formats already reached are skipped. a format that is not
                    // reachable in one hop may well be reachable in two, so it has to be
                    // proposed again on the next level
                    if (seen.contains(key)) {
                        continue;
                    }
                    if (++edges > MAX_EDGES) {
logger.log(DEBUG, "giving up, too many edges: " + sourceFormat);
                        return null;
                    }
                    if (!supported(candidate, from)) {
                        continue;
                    }
                    seen.add(key);
                    if (reached != null) {
                        reached.put(key(candidate), candidate);
                    }
                    List<AudioFormat> next = new ArrayList<>(path);
                    next.add(candidate);
                    if (goal.test(candidate)) {
                        return next;
                    }
                    queue.add(next);
                }
            }
            return null;
        } finally {
            blocked.set(prev);
        }
    }

    /**
     * The intermediate formats worth trying after {@code from}.
     * <p>
     * These are only proposals, the caller verifies each of them with {@link #supported}.
     *
     * @param targetFormat the ultimate target, used to materialize wildcards. may be null
     */
    private static Collection<AudioFormat> candidates(AudioFormat from, AudioFormat targetFormat) {
        Map<String, AudioFormat> result = new LinkedHashMap<>();

        // plain pcm pivots. almost every chain goes through pcm, and providers that
        // advertise nothing usable would be unreachable otherwise
        for (float rate : choices((float) NOT_SPECIFIED, from.getSampleRate(), rateOf(targetFormat))) {
            for (int bits : choices(NOT_SPECIFIED, 16, from.getSampleSizeInBits(), bitsOf(targetFormat))) {
                for (int channels : choices(NOT_SPECIFIED, from.getChannels(), channelsOf(targetFormat))) {
                    for (boolean bigEndian : new boolean[] {false, true}) {
                        put(result, Encoding.PCM_SIGNED, rate, bits, channels, bigEndian);
                    }
                }
            }
        }

        // what the providers say they can produce out of `from`
        for (FormatConversionProvider provider : providers()) {
            if (result.size() >= MAX_CANDIDATES) {
                break;
            }
            for (Encoding encoding : provider.getTargetEncodings(from)) {
                for (AudioFormat advertised : provider.getTargetFormats(encoding, from)) {
                    materialize(result, advertised, from, targetFormat);
                }
            }
        }
        return result.values();
    }

    /**
     * Expands one advertised format into concrete ones by replacing its
     * {@link AudioSystem#NOT_SPECIFIED} attributes with the values of the source format and
     * of the ultimate target format.
     */
    private static void materialize(Map<String, AudioFormat> result, AudioFormat advertised, AudioFormat from, AudioFormat targetFormat) {
        for (float rate : choices(advertised.getSampleRate(), from.getSampleRate(), rateOf(targetFormat))) {
            for (int bits : choices(advertised.getSampleSizeInBits(), from.getSampleSizeInBits(), bitsOf(targetFormat))) {
                for (int channels : choices(advertised.getChannels(), from.getChannels(), channelsOf(targetFormat))) {
                    // endianness cannot be advertised as "either", so both are tried
                    boolean[] endians = bits > 8 ? new boolean[] {advertised.isBigEndian(), !advertised.isBigEndian()}
                                                 : new boolean[] {advertised.isBigEndian()};
                    for (boolean bigEndian : endians) {
                        put(result, advertised.getEncoding(), rate, bits, channels, bigEndian);
                    }
                }
            }
        }
    }

    /** builds the format and adds it unless an equivalent one is already in */
    private static void put(Map<String, AudioFormat> result, Encoding encoding, float rate, int bits, int channels, boolean bigEndian) {
        if (result.size() >= MAX_CANDIDATES) {
            return;
        }
        boolean pcm = isPcm(encoding);
        // for pcm both are implied by the other attributes, for anything else they are unknown
        int frameSize = pcm && bits != NOT_SPECIFIED && channels != NOT_SPECIFIED ? (bits + 7) / 8 * channels : NOT_SPECIFIED;
        float frameRate = pcm ? rate : NOT_SPECIFIED;
        AudioFormat format = new AudioFormat(encoding, rate, bits, channels, frameSize, frameRate, bigEndian);
        result.putIfAbsent(key(format), format);
    }

    /**
     * The values to try for one attribute of an intermediate format.
     *
     * @param advertised what the provider says, wins when it is not a wildcard
     * @param fallbacks what to try instead, wildcards are dropped
     */
    private static int[] choices(int advertised, int... fallbacks) {
        if (advertised != NOT_SPECIFIED) {
            return new int[] {advertised};
        }
        int[] result = new int[fallbacks.length];
        int n = 0;
        for (int fallback : fallbacks) {
            if (fallback != NOT_SPECIFIED && !contains(result, n, fallback)) {
                result[n++] = fallback;
            }
        }
        return n == 0 ? new int[] {NOT_SPECIFIED} : Arrays.copyOf(result, n);
    }

    /** {@link #choices(int, int...)} for the sample rate */
    private static float[] choices(float advertised, float... fallbacks) {
        if (advertised != NOT_SPECIFIED) {
            return new float[] {advertised};
        }
        float[] result = new float[fallbacks.length];
        int n = 0;
        for (float fallback : fallbacks) {
            if (fallback != NOT_SPECIFIED && !contains(result, n, fallback)) {
                result[n++] = fallback;
            }
        }
        return n == 0 ? new float[] {NOT_SPECIFIED} : Arrays.copyOf(result, n);
    }

    private static boolean contains(int[] array, int length, int value) {
        for (int i = 0; i < length; i++) {
            if (array[i] == value) {
                return true;
            }
        }
        return false;
    }

    private static boolean contains(float[] array, int length, float value) {
        for (int i = 0; i < length; i++) {
            if (array[i] == value) {
                return true;
            }
        }
        return false;
    }

    // helpers

    /** asks {@link AudioSystem} without coming back here */
    private static boolean supported(AudioFormat targetFormat, AudioFormat sourceFormat) {
        boolean prev = blocked.get();
        blocked.set(true);
        try {
            return AudioSystem.isConversionSupported(targetFormat, sourceFormat);
        } finally {
            blocked.set(prev);
        }
    }

    /** asks {@link AudioSystem} without coming back here */
    private static boolean supported(Encoding targetEncoding, AudioFormat sourceFormat) {
        boolean prev = blocked.get();
        blocked.set(true);
        try {
            return AudioSystem.isConversionSupported(targetEncoding, sourceFormat);
        } finally {
            blocked.set(prev);
        }
    }

    /** {@link AudioFormat} has no equals(), so formats are compared through this */
    private static String key(AudioFormat format) {
        return format.getEncoding() + "/" + format.getSampleRate() + "/" + format.getSampleSizeInBits() + "/" +
                format.getChannels() + "/" + format.getFrameSize() + "/" + format.getFrameRate() + "/" + format.isBigEndian();
    }

    private static boolean isPcm(Encoding encoding) {
        return encoding.equals(Encoding.PCM_SIGNED) || encoding.equals(Encoding.PCM_UNSIGNED) || encoding.equals(Encoding.PCM_FLOAT);
    }

    private static float rateOf(AudioFormat format) {
        return format != null ? format.getSampleRate() : NOT_SPECIFIED;
    }

    private static int bitsOf(AudioFormat format) {
        return format != null ? format.getSampleSizeInBits() : NOT_SPECIFIED;
    }

    private static int channelsOf(AudioFormat format) {
        return format != null ? format.getChannels() : NOT_SPECIFIED;
    }
}
