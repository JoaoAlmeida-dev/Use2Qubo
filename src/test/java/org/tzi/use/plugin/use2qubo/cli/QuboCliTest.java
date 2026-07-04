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
    // Pipeline integration against checked-in example fixtures
    // -----------------------------------------------------------------

    @Test
    void maxCliqueRunReturnsInexactExitCode(@TempDir Path tempDir) throws Exception {
        File out = tempDir.resolve("maxclique-qubo.json").toFile();

        int exitCode = QuboCli.run(new String[] {
                "--model", UseFixtures.maxCliqueUse().getPath(),
                "--cmd", UseFixtures.maxCliqueCmd().getPath(),
                "--out", out.getPath()
        });

        // cliqueProperty is a boolean pass/fail over many decision variables at once,
        // needing a degree far beyond the default max_degree=3 cap to be exact; QuboEngine
        // still quadratizes the best degree-3 approximation found (10 original + ancillas)
        // rather than falling back to a degree-2 slice.
        assertEquals(3, exitCode);
        assertTrue(out.isFile());
        String json = Files.readString(out.toPath());
        assertTrue(json.contains("\"exact\": false"), json);
        assertTrue(json.contains("\"polyDegree\": 3"), json);
        assertTrue(json.contains("Contains(sol,v1)"), json);
    }

    @Test
    void garageTrucksRunReturnsInexactExitCode(@TempDir Path tempDir) throws Exception {
        File out = tempDir.resolve("garage-qubo.json").toFile();

        int exitCode = QuboCli.run(new String[] {
                "--model", UseFixtures.garageTrucksUse().getPath(),
                "--cmd", UseFixtures.garageTrucksCmd().getPath(),
                "--out", out.getPath()
        });

        // RouteRoad(Route,Road) + AssignedTo(Route,Truck) = 9 decision variables.
        // fuelPenalty() is a hinge max(0, edgeCost()-fuelRange)^2 -- a non-polynomial
        // threshold function, so it stays non-exact at any degree; quadratization only
        // reduces the degree of already-polynomial terms (coveragePenalty/shapePenalty),
        // it cannot make a branching function exact. Same disclosed trade-off as
        // coveragePenalty()/shapePenalty(); see GarbageTruckRouting.use fuelPenalty() comment.
        assertEquals(3, exitCode);
        assertTrue(out.isFile());
        String json = Files.readString(out.toPath());
        assertTrue(json.contains("\"exact\": false"), json);
    }

    // -----------------------------------------------------------------
    // Error paths
    // -----------------------------------------------------------------

    @Test
    void badModelPathFailsWithFileNotFound() {
        FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> QuboCli.run(new String[] {
                "--model", "does-not-exist.use",
                "--cmd", UseFixtures.maxCliqueCmd().getPath()
        }));
        assertTrue(e.getMessage().contains("model"));
    }

    @Test
    void badCmdPathFailsWithFileNotFound() {
        FileNotFoundException e = assertThrows(FileNotFoundException.class, () -> QuboCli.run(new String[] {
                "--model", UseFixtures.maxCliqueUse().getPath(),
                "--cmd", "does-not-exist.cmd"
        }));
        assertTrue(e.getMessage().contains("cmd"));
    }

    @Test
    void missingConfigFailsWithIOException(@TempDir Path tempDir) throws Exception {
        Path model = tempDir.resolve("MaxClique.use");
        Path cmd = tempDir.resolve("MaxClique.cmd");
        Files.copy(UseFixtures.maxCliqueUse().toPath(), model);
        Files.copy(UseFixtures.maxCliqueCmd().toPath(), cmd);
        // deliberately no qubo_config.json alongside the copied model/cmd

        IOException e = assertThrows(IOException.class, () -> QuboCli.run(new String[] {
                "--model", model.toString(),
                "--cmd", cmd.toString()
        }));
        assertTrue(e.getMessage().contains("qubo_config.json"), e.getMessage());
    }

    @Test
    void malformedCmdStatementFailsWithLineNumber(@TempDir Path tempDir) throws Exception {
        MModel model = QuboCli.compileModel(UseFixtures.maxCliqueUse());
        MSystem system = new MSystem(model);

        Path badCmd = tempDir.resolve("bad.cmd");
        Files.writeString(badCmd, "-- comment\n!this is not valid soil syntax $$$\n");

        IOException e = assertThrows(IOException.class,
                () -> QuboCli.runCmdScript(system, badCmd.toFile()));
        assertTrue(e.getMessage().contains("bad.cmd:2"), e.getMessage());
    }
}
