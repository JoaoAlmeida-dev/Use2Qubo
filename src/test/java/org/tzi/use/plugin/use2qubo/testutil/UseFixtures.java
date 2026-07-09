package org.tzi.use.plugin.use2qubo.testutil;

import org.tzi.use.plugin.use2qubo.cli.QuboCli;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MSystem;

import java.io.File;
import java.io.IOException;

/**
 * Test-only helper that mirrors QuboCli's compile-model + run-cmd-script sequence,
 * so qubo/*Test classes can build a real MSystem from the synthetic fixtures under
 * {@code src/test/resources/fixtures/} without duplicating that logic or reaching
 * across package visibility.
 *
 * <p>These fixtures are deliberately generic and self-contained: they have no relation
 * to (and are not derived from) the paper's demo scenarios under
 * {@code articles/qmod_2026/examples/}, so editing those examples for the paper can
 * never break this test suite.
 *
 * <p>{@code Selection}: 1 Picker, 3 Options -- {@code Chosen(Picker,Option)} is the sole
 * decision variable (nVars=3). Its {@code exactlyOneChosen} invariant is a boolean
 * indicator over all 3 variables at once (not degree-2-representable), but since any
 * pseudo-Boolean function of n bits has an exact multilinear polynomial of degree &le; n,
 * it is guaranteed exact once sampling escalates to degree 3, forcing quadratization
 * ancilla(s). A second fixed association, {@code Marked(Picker,Tag)}, exercises
 * {@code ctx.fixedLinks}.
 *
 * <p>{@code AllOrNothing}: 1 Picker, 4 Options -- {@code Chosen(Picker,Option)}, nVars=4.
 * Its {@code allChosen} invariant is true only when all 4 are selected, i.e. its exact
 * representation is the single top-degree monomial x1*x2*x3*x4 with every lower-degree
 * coefficient exactly zero; with {@code max_degree=3 < n=4} the derived QUBO is
 * guaranteed to remain inexact at the cap, deterministically.
 */
public final class UseFixtures {

    private static final String FIXTURES_ROOT = "src/test/resources/fixtures";

    private UseFixtures() {}

    public static MSystem buildSystem(File useFile, File cmdFile) throws IOException {
        MModel model = QuboCli.compileModel(useFile);
        MSystem system = new MSystem(model);
        QuboCli.runCmdScript(system, cmdFile);
        return system;
    }

    public static File selectionUse() {
        return new File(FIXTURES_ROOT + "/Selection/Selection.use");
    }

    public static File selectionCmd() {
        return new File(FIXTURES_ROOT + "/Selection/Selection.cmd");
    }

    public static File selectionConfig() {
        return new File(FIXTURES_ROOT + "/Selection/qubo_config.json");
    }

    /** Same model as {@link #selectionConfig()}, but declares two decision-var associations
     *  ({@code Chosen}, {@code Marked}) for UI round-trip tests that don't invoke the engine. */
    public static File selectionUiConfig() {
        return new File(FIXTURES_ROOT + "/Selection/qubo_config_ui.json");
    }

    public static File allOrNothingUse() {
        return new File(FIXTURES_ROOT + "/AllOrNothing/AllOrNothing.use");
    }

    public static File allOrNothingCmd() {
        return new File(FIXTURES_ROOT + "/AllOrNothing/AllOrNothing.cmd");
    }

    public static File allOrNothingConfig() {
        return new File(FIXTURES_ROOT + "/AllOrNothing/qubo_config.json");
    }
}
