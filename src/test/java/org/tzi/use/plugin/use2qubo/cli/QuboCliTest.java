package org.tzi.use.plugin.use2qubo.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuboCliTest {

    // -----------------------------------------------------------------
    // Pure argument parsing (no USE, no filesystem)
    // -----------------------------------------------------------------

    @Test
    void parsesAllArguments() {
        QuboCli.Args args = QuboCli.Args.parse(new String[] {
                "--model", "m.use", "--cmd", "c.cmd", "--config", "cfg.json", "--out", "o.json"
        });

        assertEquals("m.use", args.model);
        assertEquals("c.cmd", args.cmd);
        assertEquals("cfg.json", args.config);
        assertEquals("o.json", args.out);
    }

    @Test
    void configAndOutAreOptional() {
        QuboCli.Args args = QuboCli.Args.parse(new String[] { "--model", "m.use", "--cmd", "c.cmd" });

        assertEquals("m.use", args.model);
        assertEquals("c.cmd", args.cmd);
        assertEquals(null, args.config);
        assertEquals(null, args.out);
    }

    @Test
    void missingModelThrowsUsageException() {
        QuboCli.UsageException e = assertThrows(QuboCli.UsageException.class,
                () -> QuboCli.Args.parse(new String[] { "--cmd", "c.cmd" }));
        assertTrue(e.getMessage().contains("--model"));
    }

    @Test
    void missingCmdThrowsUsageException() {
        QuboCli.UsageException e = assertThrows(QuboCli.UsageException.class,
                () -> QuboCli.Args.parse(new String[] { "--model", "m.use" }));
        assertTrue(e.getMessage().contains("--cmd"));
    }

    @Test
    void unknownFlagThrowsUsageException() {
        assertThrows(QuboCli.UsageException.class,
                () -> QuboCli.Args.parse(new String[] { "--model", "m.use", "--cmd", "c.cmd", "--bogus" }));
    }

    @Test
    void missingValueForFlagThrowsUsageException() {
        assertThrows(QuboCli.UsageException.class,
                () -> QuboCli.Args.parse(new String[] { "--model", "m.use", "--cmd" }));
    }

    // -----------------------------------------------------------------
    // Pipeline integration against synthetic fixtures
    // -----------------------------------------------------------------

    @Test
    void run_inexactCapReturnsExitCode3(@TempDir Path tempDir) throws Exception {
        File out = tempDir.resolve("allornothing-qubo.json").toFile();

        int exitCode = QuboCli.run(new String[] {
                "--model", UseFixtures.allOrNothingUse().getPath(),
                "--cmd", UseFixtures.allOrNothingCmd().getPath(),
                "--out", out.getPath()
        });

        // allChosen's exact representation is the pure top-degree monomial x1*x2*x3*x4; capped
        // at max_degree=3 (< n=4), it can never be captured, so the run stays best-effort inexact.
        assertEquals(3, exitCode);
        assertTrue(out.isFile());
        String json = Files.readString(out.toPath());
        assertTrue(json.contains("\"exact\": false"), json);
        assertTrue(json.contains("\"polyDegree\": 3"), json);
        assertTrue(json.contains("Chosen(p1,o1)"), json);
    }

    @Test
    void run_exactEscalationReturnsExitCode0(@TempDir Path tempDir) throws Exception {
        File out = tempDir.resolve("selection-qubo.json").toFile();

        int exitCode = QuboCli.run(new String[] {
                "--model", UseFixtures.selectionUse().getPath(),
                "--cmd", UseFixtures.selectionCmd().getPath(),
                "--out", out.getPath()
        });

        // exactlyOneChosen is a boolean indicator over all 3 decision variables; any
        // pseudo-Boolean function of n bits has an exact multilinear polynomial of degree <= n,
        // so escalating to degree 3 (n=3, matching qubo_config.json's max_degree=3) is guaranteed
        // to derive an exact QUBO.
        assertEquals(0, exitCode);
        assertTrue(out.isFile());
        String json = Files.readString(out.toPath());
        assertTrue(json.contains("\"exact\": true"), json);
    }

    // -----------------------------------------------------------------
    // Error paths
    // -----------------------------------------------------------------

    @Test
    void badModelPathFailsWithFileNotFound() {
        FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> QuboCli.run(new String[] {
                "--model", "does-not-exist.use",
                "--cmd", UseFixtures.allOrNothingCmd().getPath()
        }));
        assertTrue(e.getMessage().contains("model"));
    }

    @Test
    void badCmdPathFailsWithFileNotFound() {
        FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> QuboCli.run(new String[] {
                "--model", UseFixtures.allOrNothingUse().getPath(),
                "--cmd", "does-not-exist.cmd"
        }));
        assertTrue(e.getMessage().contains("cmd"));
    }

    @Test
    void missingConfigFailsWithIOException(@TempDir Path tempDir) throws Exception {
        Path model = tempDir.resolve("AllOrNothing.use");
        Path cmd = tempDir.resolve("AllOrNothing.cmd");
        Files.copy(UseFixtures.allOrNothingUse().toPath(), model);
        Files.copy(UseFixtures.allOrNothingCmd().toPath(), cmd);
        // deliberately no qubo_config.json alongside the copied model/cmd

        IOException e = assertThrows(IOException.class, () -> QuboCli.run(new String[] {
                "--model", model.toString(),
                "--cmd", cmd.toString()
        }));
        assertTrue(e.getMessage().contains("qubo_config.json"), e.getMessage());
    }

    @Test
    void malformedCmdStatementFailsWithLineNumber(@TempDir Path tempDir) throws Exception {
        MModel model = QuboCli.compileModel(UseFixtures.allOrNothingUse());
        MSystem system = new MSystem(model);

        Path badCmd = tempDir.resolve("bad.cmd");
        Files.writeString(badCmd, "-- comment\n!this is not valid soil syntax $$$\n");

        IOException e = assertThrows(IOException.class,
                () -> QuboCli.runCmdScript(system, badCmd.toFile()));
        assertTrue(e.getMessage().contains("bad.cmd:2"), e.getMessage());
    }
}
