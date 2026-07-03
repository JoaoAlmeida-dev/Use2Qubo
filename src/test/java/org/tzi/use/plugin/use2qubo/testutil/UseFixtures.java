package org.tzi.use.plugin.use2qubo.testutil;

import org.tzi.use.plugin.use2qubo.cli.QuboCli;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MSystem;

import java.io.File;
import java.io.IOException;

/**
 * Test-only helper that mirrors QuboCli's compile-model + run-cmd-script sequence,
 * so qubo/*Test classes can build a real MSystem from the checked-in examples/
 * fixtures without duplicating that logic or reaching across package visibility.
 */
public final class UseFixtures {

    private UseFixtures() {}

    public static MSystem buildSystem(File useFile, File cmdFile) throws IOException {
        MModel model = QuboCli.compileModel(useFile);
        MSystem system = new MSystem(model);
        QuboCli.runCmdScript(system, cmdFile);
        return system;
    }

    public static File maxCliqueUse() {
        return new File("examples/MaxClique/MaxClique.use");
    }

    public static File maxCliqueCmd() {
        return new File("examples/MaxClique/MaxClique.cmd");
    }

    public static File maxCliqueConfig() {
        return new File("examples/MaxClique/qubo_config.json");
    }

    public static File garageTrucksUse() {
        return new File("examples/GarageTrucks/GarbageTruckRouting.use");
    }

    public static File garageTrucksCmd() {
        return new File("examples/GarageTrucks/GarbageTruckRouting.cmd");
    }

    public static File garageTrucksConfig() {
        return new File("examples/GarageTrucks/qubo_config.json");
    }
}
