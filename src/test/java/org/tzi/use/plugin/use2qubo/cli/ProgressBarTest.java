package org.tzi.use.plugin.use2qubo.cli;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.qubo.engine.ProgressEvent;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressBarTest {

    private static ByteArrayOutputStream buffer;
    private static PrintStream stream() throws UnsupportedEncodingException {
        buffer = new ByteArrayOutputStream();
        return new PrintStream(buffer, true, "UTF-8");
    }

    private static String captured() throws UnsupportedEncodingException {
        return buffer.toString("UTF-8");
    }

    // A "bare" \r is one used for live carriage-return overwriting, as opposed to the \r that is
    // part of println's platform line separator on Windows (\r\n) — must not conflate the two.
    private static final Pattern BARE_CR = Pattern.compile("\r(?!\n)");

    private static long bareCarriageReturnCount(String s) {
        return BARE_CR.matcher(s).results().count();
    }

    @Test
    void interactiveModeRendersCarriageReturnBar() throws Exception {
        ProgressBar bar = new ProgressBar(stream(), true);

        bar.accept(ProgressEvent.phase("Sampling cost degree 2"));
        bar.accept(new ProgressEvent("Sampling cost degree 2", "cost", 2, 1, 4));
        bar.accept(new ProgressEvent("Sampling cost degree 2", "cost", 2, 4, 4));

        String out = captured();
        assertTrue(out.contains("Sampling cost degree 2"), out);
        assertTrue(bareCarriageReturnCount(out) > 0, out);
        assertTrue(out.contains("100%"), out);
    }

    @Test
    void plainModeNeverEmitsCarriageReturn() throws Exception {
        ProgressBar bar = new ProgressBar(stream(), false);

        bar.accept(ProgressEvent.phase("Sampling cost degree 2"));
        for (int i = 1; i <= 10; i++) {
            bar.accept(new ProgressEvent("Sampling cost degree 2", "cost", 2, i, 10));
        }

        String out = captured();
        assertEquals(0, bareCarriageReturnCount(out), out);
        assertTrue(out.contains("Sampling cost degree 2"), out);
    }

    @Test
    void plainModeThrottlesDuplicatePercentEvents() throws Exception {
        ProgressBar bar = new ProgressBar(stream(), false);
        bar.accept(ProgressEvent.phase("Sampling cost degree 3"));

        // 1000 events all landing in the same 0% bucket (current/total ~ 0.0x%) should render
        // at most once beyond the phase line, not 1000 times.
        for (int i = 1; i <= 1000; i++) {
            bar.accept(new ProgressEvent("Sampling cost degree 3", "cost", 3, i, 1_000_000));
        }

        String out = captured();
        long renderedProgressLines = out.lines().filter(l -> l.contains("%")).count();
        assertTrue(renderedProgressLines <= 2, "expected throttling, got " + renderedProgressLines + " lines:\n" + out);
    }

    @Test
    void interactiveModeThrottlesDuplicatePercentEvents() throws Exception {
        ProgressBar bar = new ProgressBar(stream(), true);
        bar.accept(ProgressEvent.phase("Sampling cost degree 3"));

        for (int i = 1; i <= 1000; i++) {
            bar.accept(new ProgressEvent("Sampling cost degree 3", "cost", 3, i, 1_000_000));
        }

        String out = captured();
        long carriageReturns = bareCarriageReturnCount(out);
        assertTrue(carriageReturns <= 2, "expected throttling, got " + carriageReturns + " \\r renders:\n" + out);
    }

    @Test
    void neverContainsCliSummaryFormat() throws Exception {
        ProgressBar bar = new ProgressBar(stream(), true);
        bar.accept(ProgressEvent.phase("Building variable index"));
        bar.accept(new ProgressEvent("Sampling cost degree 2", "cost", 2, 1, 4));

        String out = captured();
        assertFalse(out.matches("(?s).*nVars=\\d+ exact=(PASS|FAIL) derivationMs=\\d+ out=.*"), out);
    }

    @Test
    void neverWritesToSystemOut() throws Exception {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream stdoutCapture = new ByteArrayOutputStream();
        System.setOut(new PrintStream(stdoutCapture, true, "UTF-8"));
        try {
            ProgressBar bar = new ProgressBar(stream(), true);
            bar.accept(ProgressEvent.phase("Building variable index"));
            bar.accept(new ProgressEvent("Sampling cost degree 2", "cost", 2, 1, 4));
            bar.accept(new ProgressEvent("Sampling cost degree 2", "cost", 2, 4, 4));
        } finally {
            System.setOut(originalOut);
        }
        assertEquals("", stdoutCapture.toString("UTF-8"));
    }

    @Test
    void disabledBarRendersNothing() throws Exception {
        ProgressBar bar = ProgressBar.disabled();
        bar.accept(ProgressEvent.phase("Building variable index"));
        bar.accept(new ProgressEvent("Sampling cost degree 2", "cost", 2, 1, 4));
        // no exception, no output stream to inspect (out == null) — reaching here is the assertion
    }

    @Test
    void isInteractiveStderrHonoursSystemPropertyOverride() {
        String prior = System.getProperty("use2qubo.progress");
        try {
            System.setProperty("use2qubo.progress", "bar");
            assertTrue(ProgressBar.isInteractiveStderr());

            System.setProperty("use2qubo.progress", "plain");
            assertFalse(ProgressBar.isInteractiveStderr());

            System.setProperty("use2qubo.progress", "off");
            assertFalse(ProgressBar.isInteractiveStderr());
        } finally {
            if (prior == null) System.clearProperty("use2qubo.progress");
            else System.setProperty("use2qubo.progress", prior);
        }
    }
}
