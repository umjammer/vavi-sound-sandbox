/*
 * Copyright (c) 2024 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package vavix.rococoa.ituneslibrary;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import vavi.util.CharNormalizerJa;
import vavi.util.Debug;
import vavi.util.LevenshteinDistance;
import vavix.util.screenscrape.annotation.HtmlXPathParser;
import vavix.util.screenscrape.annotation.InputHandler;
import vavix.util.screenscrape.annotation.Target;
import vavix.util.screenscrape.annotation.WebScraper;
import vavix.util.selenium.SeleniumUtil;


/**
 * iTunes (Selenium version).
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2018/02/27 nsano initial version <br>
 */
public class iTunes2 {

    private SeleniumUtil seleniumUtil = new SeleniumUtil();
    private WebDriver driver = seleniumUtil.getWebDriver();

    private iTunes2() {
        // authentication?
        driver.navigate().to("http://www2.jasrac.or.jp/eJwid/");
        WebElement button0 = driver.findElements(By.tagName("form")).get(1).findElement(By.name("input"));

        button0.click();
        SeleniumUtil.waitFor(driver);
    }

    private static iTunes2 instance;

    public static iTunes2 getInstance() {
        if (instance == null) {
            instance = new iTunes2();
        }
        return instance;
    }

    /** artist/work name search */
    public static class MyInput implements InputHandler<Reader> {
        /**
         * @param args 0: artist, 1: title
         */
        @Override
        public Reader getInput(String ... args) throws IOException {
try {
            String artist = args[0].toUpperCase();
            String title = args[1].toUpperCase();
//System.err.println("ARGS: " + artist + ", " + title);

            instance.driver.switchTo().window((String) instance.driver.getWindowHandles().toArray()[0]);

            instance.driver.switchTo().frame("frame2");
            SeleniumUtil.waitFor(instance.driver);

            WebElement inputT = instance.driver.findElement(By.name("IN_WORKS_TITLE_NAME1"));
            instance.seleniumUtil.setAttribute(inputT, "value", title);
            // 0:前方一致, 1:後方一致, 2:中間一致 3:完全一致
//            Select selectT = new Select(app.driver.getSelectByName("IN_WORKS_TITLE_OPTION1"));
//            selectT.selectByValue("3");
            WebElement inputA = instance.driver.findElement(By.name("IN_ARTIST_NAME1"));
            instance.seleniumUtil.setAttribute(inputA, "value", artist);
            // 0:前方一致, 1:後方一致, 2:中間一致 3:完全一致
            Select selectA = new Select(instance.driver.findElement(By.name("IN_ARTIST_NAME_OPTION1")));
            selectA.selectByValue("3");
            WebElement button1 = instance.driver.findElement(By.name("CMD_SEARCH"));

            button1.click();
            SeleniumUtil.waitFor(instance.driver);

            // back to page 1
            instance.driver.switchTo().window((String) instance.driver.getWindowHandles().toArray()[0]);

            instance.driver.switchTo().frame("frame3");
//Debug.println("location: " + app.driver.getCurrentUrl());
            return new StringReader(instance.driver.getPageSource());
} catch (Exception e) {
 instance.seleniumUtil.showStats();
 throw e;
}
        }
    }

    @WebScraper(input = MyInput.class,
//                isDebug = true,
                parser = HtmlXPathParser.class)
    public static class TitleUrl {
        @Target(value = "//TABLE//TR/TD[2]/DIV/text()")
        String artist;
        @Target(value = "//TABLE//TR/TD[4]/A/text()")
        String title;
        @Target(value = "//TABLE//TR/TD[4]/A/@href")
        String url;
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(CharNormalizerJa.ToHalfAns2.normalize(artist));
            sb.append(", ");
            sb.append(CharNormalizerJa.ToHalfAns2.normalize(title));
            return sb.toString();
        }
    }

    /** lyrics and composition details (single) */
    public static class MyInput2 implements InputHandler<Reader> {
        /**
         * @param args 0: url
         */
        @Override
        public Reader getInput(String ... args) throws IOException {
            String url = args[0];

            instance.driver.navigate().to("http://www2.jasrac.or.jp/eJwid/" + url);
            SeleniumUtil.waitFor(instance.driver);

            return new StringReader(instance.driver.getPageSource());
        }
    }

    /** lyrics and composition details (one line) */
    @WebScraper(input = MyInput2.class,
//                isDebug = true,
                parser = HtmlXPathParser.class)
    public static class Composer {
        @Target(value = "//TABLE[4]//TR/TD[2]/SPAN/text()")
        String name;
        @Target(value = "//TABLE[4]//TR/TD[3]/DIV/text()")
        String type;
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(type);
            sb.append(", ");
            sb.append(CharNormalizerJa.ToHalfAns2.normalize(name));
            return sb.toString();
        }
    }

    /** search by title*/
    public static class MyInput3 implements InputHandler<Reader> {
        /**
         * @param args 0: title
         */
        @Override
        public Reader getInput(String ... args) throws IOException {
            String title = args[0];
//System.err.println("ARGS: " + artist + ", " + title);

            instance.driver.switchTo().window((String) instance.driver.getWindowHandles().toArray()[0]);

            instance.driver.switchTo().frame("frame2");
            SeleniumUtil.waitFor(instance.driver);

            WebElement inputT = instance.driver.findElement(By.name("IN_WORKS_TITLE_NAME1"));
            instance.seleniumUtil.setAttribute(inputT, "value", title);
            Select selectT = new Select(instance.driver.findElement(By.name("IN_WORKS_TITLE_OPTION1")));
            selectT.selectByValue("3");
            WebElement button1 = instance.driver.findElement(By.name("CMD_SEARCH"));

            button1.click();
            SeleniumUtil.waitFor(instance.driver);

            instance.driver.switchTo().window((String) instance.driver.getWindowHandles().toArray()[instance.driver.getWindowHandles().size() - 1]);

            StringBuilder sb = new StringBuilder(instance.driver.getPageSource());

            try {
                while (true) {
                    WebElement nextAnchor = nextAnchor(instance.driver.findElements(By.tagName("a")));
Debug.println("nextAnchor: " + nextAnchor);
                    nextAnchor.click();
                    sb.append(instance.driver.getPageSource());
                }
            } catch (NoSuchElementException ignored) {
            }

//System.err.println(sb);
            return new StringReader(sb.toString());
        }

        /** */
        WebElement nextAnchor(List<WebElement> anchors) {
            for (WebElement anchor : anchors) {
                if (anchor.getAttribute("title").equals("次ページの結果を表示します")) {
                    return anchor;
                }
            }
            throw new NoSuchElementException();
        }
    }

    /** works with specified work name (multiple) */
    @WebScraper(input = MyInput3.class,
//                isDebug = true,
                parser = HtmlXPathParser.class)
    public static class TitleUrl3 {
        @Target(value = "//TABLE//TR/TD[5]/text()")
        String artist;
        @Target(value = "//TABLE//TR/TD[3]/A/text()")
        String title;
        @Target(value = "//TABLE//TR/TD[3]/A/@href")
        String url;
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(CharNormalizerJa.ToHalfAns2.normalize(artist));
            sb.append(", ");
            sb.append(CharNormalizerJa.ToHalfAns2.normalize(title));
            return sb.toString();
        }
    }

    /** search by artist*/
    public static class MyInput4 implements InputHandler<Reader> {
        /**
         * @param args 0: artist
         */
        @Override
        public Reader getInput(String ... args) throws IOException {
            String artist = args[0];

            instance.driver.switchTo().window((String) instance.driver.getWindowHandles().toArray()[0]);

            instance.driver.switchTo().frame("frame2");
            SeleniumUtil.waitFor(instance.driver);

            WebElement inputA = instance.driver.findElement(By.name("IN_ARTIST_NAME1"));
            instance.seleniumUtil.setAttribute(inputA, "value", artist);
            Select selectA = new Select(instance.driver.findElement(By.name("IN_ARTIST_NAME_OPTION1")));
            selectA.selectByValue("3");
            WebElement button1 = instance.driver.findElement(By.name("CMD_SEARCH"));

            button1.click();
            SeleniumUtil.waitFor(instance.driver);

            instance.driver.switchTo().window((String) instance.driver.getWindowHandles().toArray()[instance.driver.getWindowHandles().size() - 1]);

            StringBuilder sb = new StringBuilder(instance.driver.getPageSource());

            try {
                while (true) {
                    WebElement nextAnchor = nextAnchor(instance.driver.findElements(By.tagName("a")));
                    nextAnchor.click();
                    sb.append(instance.driver.getPageSource());
                }
            } catch (NoSuchElementException ignored) {
            }

            return new StringReader(sb.toString());
        }

        /** */
        WebElement nextAnchor(List<WebElement> anchors) {
            for (WebElement anchor : anchors) {
                if (anchor.getAttribute("title").equals("次ページの結果を表示します")) {
                    return anchor;
                }
            }
            throw new NoSuchElementException();
        }
    }

    /** works specified by the artist (multiple) */
    @WebScraper(input = MyInput4.class,
//                isDebug = true,
                parser = HtmlXPathParser.class)
    public static class TitleUrl4 {
        @Target(value = "//TABLE//TR/TD[1]/DIV/text()")
        String artist;
        @Target(value = "//TABLE//TR/TD[4]/A/text()")
        String title;
        @Target(value = "//TABLE//TR/TD[4]/A/@href")
        String url;
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(CharNormalizerJa.ToHalfAns2.normalize(artist));
            sb.append(", ");
            sb.append(CharNormalizerJa.ToHalfAns2.normalize(title));
            return sb.toString();
        }
    }

    /** Sort by artist name */
    static class MyComparator3 implements Comparator<TitleUrl3> {
        String artist;
        MyComparator3(String artist) {
            this.artist = artist.toUpperCase();
        }
        @Override public int compare(TitleUrl3 o1, TitleUrl3 o2) {
            int d1 = LevenshteinDistance.calculate(artist, CharNormalizerJa.ToHalfAns2.normalize(o1.artist)) - LevenshteinDistance.calculate(artist, CharNormalizerJa.ToHalfAns2.normalize(o2.artist));
            return d1;
        }
    }

    /** Sort by title of work */
    static class MyComparator4 implements Comparator<TitleUrl4> {
        String name;
        MyComparator4(String name) {
            this.name = name.toUpperCase();
        }
        @Override public int compare(TitleUrl4 o1, TitleUrl4 o2) {
            int d1 = LevenshteinDistance.calculate(name, CharNormalizerJa.ToHalfAns2.normalize(o1.title)) - LevenshteinDistance.calculate(name, CharNormalizerJa.ToHalfAns2.normalize(o2.title));
            return d1;
        }
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) {
            return s;
        } else {
            return s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
        }
    }

    private static final Pattern normalizeComposerPattern = Pattern.compile("[\\p{Upper}\\d' _ー\\.\\(\\)-]+");

    /** TODO Mc-, O-, Dr, St, Van, De-, La-, III, II, Jr, Sr, DJ ... and (US1), (GB) ... */
    private static String normalizeComposer(String name) {
        Matcher matcher = normalizeComposerPattern.matcher(name);
        if (!matcher.matches()) {
            return name; // domestic (Japan)
        }
        name = name.replace("ー", "-");
        StringBuilder result = new StringBuilder();
        String[] ns = name.split("\\s");
        if (ns.length > 1) {
            for (int i = 1; i < ns.length; i++) {
                result.append(capitalize(ns[i]));
                result.append(" ");
            }
        }
        result.append(capitalize(ns[0]));
        return result.toString();
    }

    private String getComposer(String url) throws IOException {
//Debug.println("url: " + url);
        List<Composer> cs = WebScraper.Util.scrape(Composer.class, url);
        StringBuilder lyrics_ = new StringBuilder();
        StringBuilder music_ = new StringBuilder();
        for (Composer composer : cs) {
//Debug.println(composer);
//Debug.println(composer.type + ", " + composer.type.indexOf("作詞") + ", " + composer.type.indexOf("作曲"));
            if ((composer.type.contains("作詞") || composer.type.contains("訳詞")) && !composer.name.contains("権利者")) {
                lyrics_.append(normalizeComposer(CharNormalizerJa.ToHalfAns2.normalize(composer.name)));
                lyrics_.append(", ");
            }
            if ((composer.type.contains("作曲") || composer.type.contains("不明")) && !composer.name.contains("権利者")) {
                music_.append(normalizeComposer(CharNormalizerJa.ToHalfAns2.normalize(composer.name)));
                music_.append(", ");
            }
        }
        if (lyrics_.length() > 1) {
            lyrics_.setLength(lyrics_.length() - 2);
        }
        if (music_.length() > 1) {
            music_.setLength(music_.length() - 2);
        }
        String lyrics = lyrics_.toString();
        String music = music_.toString();
        return lyrics.equals(music) || lyrics.isEmpty() ? music : music + " / " + lyrics;
    }

    private int errorCount = 0;

    private static void sleep() {
        try {
            Thread.sleep(300);
        } catch (InterruptedException ignored) {
        }
    }

    static {
        com.sun.jna.NativeLibrary.addSearchPath("rococoa", System.getProperty("java.library.path"));
    }

    public void processITunesLibrary() throws Exception {
        ITLibrary library = ITLibrary.libraryWithAPIVersion("1.1");
        library.getMediaItems().stream()
            .filter(each -> each.mediaKind() == 2)
            .forEach(each -> {
                try {
                    // SPECIAL, exclude speed leaning
                    if ("Speed Learning".equals(each.artist().name())) {
                        return;
                    }

                    if (each.composer() != null && !each.composer().isEmpty()) {
                        return;
                    }

                    getComposerFromJasrac(each.artist().name(), each.title(), each.album().albumArtist()).forEach(System.out::println);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                    System.err.println("error: " + each.artist().name() + " - " + each.title());
                    errorCount++;
                    if (errorCount > 2) {
Debug.println("too many errors: " + errorCount);
                        System.exit(1);
                    }
                }
            });
    }

    // TODO la, un, los
    static final Pattern normalizeArticlePattern = Pattern.compile("(An|A|The) (.*)");

    // TODO "...
    static final Pattern normalizeNamePattern = Pattern.compile("(.*)(feat.*|[~〜].+|[-ー].+|[\\/／].+|[\\(（].+)");

    public static class Result {
        enum Probability {
            RESULT, RESULTa, RESULTn, RESULTp, MAYBEa, MAYBEn, NONE
        }
        Probability probability;
        String artist;
        String title;
        String composer;
        Result(String artist, String title, String composer, Probability probability) {
            this.artist = artist;
            this.title = title;
            this.composer = composer;
            this.probability = probability;
        }
        public String toString() {
            return probability + "\t" + artist + "\t" + title + "\t" + composer;
        }
    }

    static class Result2 extends Result {
        String artist2;
        String title2;
        int index;
        Result2(String artist, String title, String composer, Probability probability, int index, String artist2, String title2) {
            super(artist, title, composer, probability);
            this.artist2 = artist2;
            this.title2 = title2;
            this.index = index;
        }
        public String toString() {
            return probability + String.valueOf(index) + "\t" + artist + "\t" + title + "\t" + "(" + composer + ")" + "\t[" + artist2 + ", " + title2 + "]";
        }
    }

    /**
     * main
     */
    public List<Result> getComposerFromJasrac(String artist, String title, String albumArtist) throws IOException {
        List<Result> result = new ArrayList<>();

//System.err.println(artist + " - " + title);
        // 1. plain artist, name
        List<TitleUrl> urls = WebScraper.Util.scrape(TitleUrl.class, artist, title);
        if (!urls.isEmpty()) {
            sleep();
            result.add(new Result(artist, title, getComposer(urls.get(0).url), Result.Probability.RESULT));
            return result;
        }

        // 2. re-scrape by album artist, name
        String normalizedArtist = artist;
        if (albumArtist != null && !albumArtist.isEmpty()) {
            normalizedArtist = albumArtist;
            sleep();
            List<TitleUrl> urls2 = WebScraper.Util.scrape(TitleUrl.class, normalizedArtist, title);
            if (!urls2.isEmpty()) {
                sleep();
                result.add(new Result(artist, title, getComposer(urls2.get(0).url), Result.Probability.RESULTa));
                return result;
            }
        }

        // 3. re-scrape by album artist, normalized name (cut ~XXX, -XXX, feat. XXX)
        // TODO (...), & -> and, II -> 2
        String normalizedName = title;
        Matcher matcher = normalizeArticlePattern.matcher(title);
        if (matcher.matches()) {
            normalizedName = matcher.group(2);
        }
        matcher = normalizeNamePattern.matcher(normalizedName);
        if (matcher.matches()) {
            normalizedName = matcher.group(1);
        }
        sleep();
        List<TitleUrl> urls3 = WebScraper.Util.scrape(TitleUrl.class, normalizedArtist, normalizedName);
        if (!urls3.isEmpty()) {
            sleep();
            result.add(new Result(artist, title, getComposer(urls3.get(0).url), Result.Probability.RESULTn));
            return result;
        }

        // 4. by artist only
        int ca = 0;
        sleep();
        List<TitleUrl4> url4s = WebScraper.Util.scrape(TitleUrl4.class, normalizedArtist);
        if (!url4s.isEmpty()) {
            url4s.sort(new MyComparator4(normalizedName));
            for (TitleUrl4 url4 : url4s) {
                if (ca == 0 && normalizedName.equalsIgnoreCase(CharNormalizerJa.ToHalfAns2.normalize(url4.title))) {
                    sleep();
                    result.add(new Result(artist, title, getComposer(url4.url), Result.Probability.RESULTp));
                    return result;
                }
                sleep();
                result.add(new Result2(artist, title, getComposer(url4.url), Result.Probability.MAYBEa, ca, CharNormalizerJa.ToHalfAns2.normalize(url4.artist), CharNormalizerJa.ToHalfAns2.normalize(url4.title)));
                ca++;
                if (ca > 2) {
                    break;
                }
            }
        }

        // 5. by name only
        sleep();
        List<TitleUrl3> url3s = WebScraper.Util.scrape(TitleUrl3.class, normalizedName);
        int cn = 0;
        if (!url3s.isEmpty()) {
            matcher = normalizeArticlePattern.matcher(normalizedArtist);
            if (matcher.matches()) {
                normalizedArtist = matcher.group(2);
            }
            url3s.sort(new MyComparator3(normalizedArtist));
            for (TitleUrl3 url3 : url3s) {
                sleep();
                result.add(new Result2(artist, title, getComposer(url3.url), Result.Probability.MAYBEn, cn, CharNormalizerJa.ToHalfAns2.normalize(url3.artist), CharNormalizerJa.ToHalfAns2.normalize(url3.title)));
                cn++;
                if (cn > 2) {
                    break;
                }
            }
            return result;
        }

        // at last
        if (ca == 0) {
            result.add(new Result(artist, title, "", Result.Probability.NONE));
        }

        return result;
    }

    /**
     * @param args 0: artist, 1: title
     */
    public static void main(String[] args) throws Exception {
        iTunes2 app = iTunes2.getInstance();
        app.processITunesLibrary();
    }
}
