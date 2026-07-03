package org.tzi.use.plugin.use2qubo.cli;

import org.tzi.use.parser.shell.ShellCommandCompiler;
import org.tzi.use.parser.use.USECompiler;
import org.tzi.use.plugin.use2qubo.qubo.QuboContext;
import org.tzi.use.plugin.use2qubo.qubo.QuboContextBuilder;
import org.tzi.use.plugin.use2qubo.qubo.QuboEngine;
import org.tzi.use.plugin.use2qubo.qubo.QuboResult;
import org.tzi.use.plugin.use2qubo.qubo.QuboResultExporter;
import org.tzi.use.plugin.use2qubo.util.PluginLog;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.mm.ModelFactory;
import org.tzi.use.uml.sys.MSystem;
import org.tzi.use.uml.sys.soil.MStatement;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Headless entry point for the derive-QUBO pipeline: compiles a {@code .use} model,
 * populates its state from a {@code .cmd} script, and derives {@code qubo.json} without
 * launching USE's Swing UI. See tickets/JAVA-013-headless-cli.md.
 */
public final class QuboCli {

    private QuboCli() {}

    public static void main(String[] args) {
        try {
            System.exit(run(args));
        } catch (UsageException e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println(usage());
            System.exit(2);
        } catch (Exception e) {
            System.err.println("use2qubo-cli: " + e.getMessage());
            System.exit(1);
        }
    }

    /** Runs the full pipeline and returns the process exit code (0 = exact, 3 = derived but inexact). Never calls System.exit. */
    static int run(String[] args) throws Exception {
        Args parsed = Args.parse(args);

        PluginLog.init(new PrintWriter(System.err, true));

        File modelFile = requireFile(parsed.model, "model");
        File cmdFile = requireFile(parsed.cmd, "cmd script");

        MModel model = compileModel(modelFile);
        MSystem system = new MSystem(model);
        runCmdScript(system, cmdFile);

        Path configPath = parsed.config != null
                ? Path.of(parsed.config)
                : new File(modelFile.getAbsoluteFile().getParentFile(), "qubo_config.json").toPath();
        if (!configPath.toFile().isFile()) {
            throw new IOException("qubo_config.json not found at " + configPath);
        }

        File outFile = parsed.out != null
                ? new File(parsed.out)
                : new File(modelFile.getAbsoluteFile().getParentFile(), "qubo.json");

        QuboContext ctx = QuboContextBuilder.build(system, configPath);
        long t0 = System.nanoTime();
        QuboResult result = QuboEngine.derive(ctx, msg -> System.err.println("[use2qubo-cli] " + msg));
        long ms = (System.nanoTime() - t0) / 1_000_000;
        result = result.withDerivationMs(ms);

        QuboResultExporter.write(result, outFile);

        System.out.println(String.format(
                "nVars=%d exact=%s derivationMs=%d out=%s",
                result.nVars, result.exact ? "PASS" : "FAIL", ms, outFile.getAbsolutePath()));

        return result.exact ? 0 : 3;
    }

    public static MModel compileModel(File modelFile) throws IOException {
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(modelFile))) {
            MModel model = USECompiler.compileSpecification(
                    in, modelFile.getAbsolutePath(), modelFile.toURI(),
                    new PrintWriter(System.err), new ModelFactory());
            if (model == null) {
                throw new IOException("Failed to compile model: " + modelFile);
            }
            return model;
        }
    }

    /** Executes each `!`-prefixed SOIL statement line of a .cmd script against the freshly built system. */
    public static void runCmdScript(MSystem system, File cmdFile) throws IOException {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(cmdFile), StandardCharsets.UTF_8))) {
            String rawLine;
            int lineNo = 0;
            while ((rawLine = reader.readLine()) != null) {
                lineNo++;
                String line = rawLine.trim();
                if (line.isEmpty() || line.startsWith("--") || line.startsWith("//")) continue;
                if (!line.startsWith("!")) continue; // only SOIL statements populate state; other shell commands unsupported here
                String soil = line.startsWith("!!") ? line.substring(2).trim() : line.substring(1).trim();
                if (soil.isEmpty()) continue;

                MStatement statement = ShellCommandCompiler.compileShellCommand(
                        system.model(), system.state(), system.getVariableEnvironment(),
                        soil, cmdFile.getName() + ":" + lineNo, new PrintWriter(System.err), false);
                if (statement == null) {
                    throw new IOException("Failed to parse .cmd statement at "
                            + cmdFile.getName() + ":" + lineNo + ": " + soil);
                }
                try {
                    system.execute(statement);
                } catch (Exception e) {
                    throw new IOException("Failed to execute .cmd statement at "
                            + cmdFile.getName() + ":" + lineNo + ": " + e.getMessage(), e);
                }
            }
        }
    }

    static File requireFile(String path, String label) throws FileNotFoundException {
        if (path == null) throw new UsageException("Missing required --" + label.replace(" script", "") + " argument");
        File f = new File(path);
        if (!f.isFile()) throw new FileNotFoundException(label + " file not found: " + path);
        return f;
    }

    private static String usage() {
        return "usage: use2qubo-cli --model <model.use> --cmd <script.cmd> "
                + "[--config <qubo_config.json>] [--out <qubo.json>]";
    }

    static final class Args {
        String model;
        String cmd;
        String config;
        String out;

        static Args parse(String[] args) {
            Args a = new Args();
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("--model")) {
                    a.model = value(args, ++i, arg);
                } else if (arg.equals("--cmd")) {
                    a.cmd = value(args, ++i, arg);
                } else if (arg.equals("--config")) {
                    a.config = value(args, ++i, arg);
                } else if (arg.equals("--out")) {
                    a.out = value(args, ++i, arg);
                } else {
                    throw new UsageException("Unknown argument: " + arg);
                }
            }
            if (a.model == null) throw new UsageException("Missing required --model argument");
            if (a.cmd == null) throw new UsageException("Missing required --cmd argument");
            return a;
        }

        static String value(String[] args, int i, String flag) {
            if (i >= args.length) throw new UsageException("Missing value for " + flag);
            return args[i];
        }
    }

    static final class UsageException extends RuntimeException {
        UsageException(String message) { super(message); }
    }
}
