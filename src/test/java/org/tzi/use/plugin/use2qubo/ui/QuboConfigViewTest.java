package org.tzi.use.plugin.use2qubo.ui;

import org.junit.jupiter.api.Test;
import org.tzi.use.plugin.use2qubo.qubo.config.QuboConfig;
import org.tzi.use.plugin.use2qubo.testutil.UseFixtures;
import org.tzi.use.uml.mm.MModel;
import org.tzi.use.uml.sys.MSystem;

import javax.swing.JCheckBox;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression coverage for two bugs found in manual testing: Save silently dropped
 * decision_vars (always wrote an empty array regardless of checked associations), and the
 * max-degree spinner's upper bound (6) made the up-arrow permanently dead on any config
 * already at or above that degree (e.g. GarageTrucks' max_degree=7).
 */
class QuboConfigViewTest {

    @Test
    void save_writesDecisionVarsMatchingCheckedAssociations() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        MModel model = system.model();
        File configFile = UseFixtures.garageTrucksConfig();
        File tmpConfig = File.createTempFile("qubo_config", ".json");
        tmpConfig.deleteOnExit();
        Files.write(tmpConfig.toPath(), Files.readAllBytes(configFile.toPath()));

        QuboConfigView view = new QuboConfigView(tmpConfig, model);

        @SuppressWarnings("unchecked")
        Map<String, JCheckBox> checkboxes =
                (Map<String, JCheckBox>) getField(view, "assocCheckboxes");
        assertTrue(checkboxes.get("RouteRoad").isSelected(), "prefill should have checked RouteRoad");
        assertTrue(checkboxes.get("AssignedTo").isSelected(), "prefill should have checked AssignedTo");

        invokeDoSave(view, tmpConfig);

        String raw = new String(Files.readAllBytes(tmpConfig.toPath()), StandardCharsets.UTF_8);
        QuboConfig saved = QuboConfig.parse(raw);

        assertEquals(2, saved.dvEntries.size(), "decision_vars must round-trip, not be dropped: " + raw);
        boolean hasRouteRoad = saved.dvEntries.stream()
                .anyMatch(e -> e[1].equals("RouteRoad") && e[2].equals("Route") && e[3].equals("Road"));
        boolean hasAssignedTo = saved.dvEntries.stream()
                .anyMatch(e -> e[1].equals("AssignedTo") && e[2].equals("Route") && e[3].equals("Truck"));
        assertTrue(hasRouteRoad, "expected RouteRoad(Route,Road) entry, got: " + raw);
        assertTrue(hasAssignedTo, "expected AssignedTo(Route,Truck) entry, got: " + raw);
    }

    @Test
    void maxDegreeSpinner_acceptsDegreeAboveOldSixCap() throws Exception {
        MSystem system = UseFixtures.buildSystem(UseFixtures.garageTrucksUse(), UseFixtures.garageTrucksCmd());
        MModel model = system.model();

        QuboConfigView view = new QuboConfigView(UseFixtures.garageTrucksConfig(), model);

        JSpinner spinner = (JSpinner) getField(view, "maxDegreeSpinner");
        assertEquals(7, spinner.getValue(), "prefill should load max_degree=7 from GarageTrucks config");

        SpinnerNumberModel spinnerModel = (SpinnerNumberModel) spinner.getModel();
        Object next = spinnerModel.getNextValue();
        assertTrue(next != null, "up-arrow must still offer a next value above the prefilled degree");
        assertEquals(8, next);
    }

    private static Object getField(Object target, String name) throws Exception {
        Field f = QuboConfigView.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(target);
    }

    private static void invokeDoSave(QuboConfigView view, File configFile) throws Exception {
        Method m = QuboConfigView.class.getDeclaredMethod("doSave", File.class);
        m.setAccessible(true);
        m.invoke(view, configFile);
    }
}
