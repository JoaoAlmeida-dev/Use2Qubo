package org.tzi.use.plugin.use2qubo.util;

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimpleJsonWriterTest {

    @Test
    void keyValueEscapesQuotesBackslashesAndNewlines() {
        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.keyValue("msg", "line1\\line2\n\"quoted\"", false);
        w.objectClose(false);

        String json = w.toString();
        assertTrue(json.contains("\"msg\": \"line1\\\\line2\\n\\\"quoted\\\"\""), json);
    }

    @Test
    void renderValueFormatsWholeNumberDoublesWithOneDecimal() {
        assertEquals("11.0", SimpleJsonWriter.renderValue(11.0));
        assertEquals("-4.0", SimpleJsonWriter.renderValue(-4.0));
    }

    @Test
    void renderValueKeepsFractionalDoublesAsIs() {
        assertEquals("11.5", SimpleJsonWriter.renderValue(11.5));
    }

    @Test
    void renderValueFormatsBooleansAndIntegers() {
        assertEquals("true", SimpleJsonWriter.renderValue(Boolean.TRUE));
        assertEquals("false", SimpleJsonWriter.renderValue(Boolean.FALSE));
        assertEquals("42", SimpleJsonWriter.renderValue(42));
    }

    @Test
    void renderValueQuotesStrings() {
        assertEquals("\"hello\"", SimpleJsonWriter.renderValue("hello"));
    }

    @Test
    void keyValueTrailingCommaControlsComma() {
        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.keyValue("a", 1, true);
        w.keyValue("b", 2, false);
        w.objectClose(false);

        String json = w.toString();
        assertTrue(json.contains("\"a\": 1,\n"), json);
        assertTrue(json.contains("\"b\": 2\n"), json);
    }

    @Test
    void linkArrayEmptyList() {
        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.linkArray("links", Collections.emptyList(), false);
        w.objectClose(false);

        assertTrue(w.toString().contains("\"links\": []"));
    }

    @Test
    void linkArraySingleItemStaysOnOneLine() {
        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.linkArray("links", List.of("a,b"), false);
        w.objectClose(false);

        assertTrue(w.toString().contains("\"links\": [\"a,b\"]"));
    }

    @Test
    void linkArrayMultiItemIsOneItemPerLine() {
        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.linkArray("links", List.of("a,b", "c,d"), false);
        w.objectClose(false);

        String json = w.toString();
        assertTrue(json.contains("\"links\": [\n"), json);
        assertTrue(json.contains("\"a,b\",\n"), json);
        assertTrue(json.contains("\"c,d\"\n"), json);
    }

    @Test
    void objectCloseWritesTrailingCommaWhenRequested() {
        SimpleJsonWriter w = new SimpleJsonWriter();
        w.objectOpen();
        w.objectClose(true);
        assertEquals("{\n},\n", w.toString());
    }
}
