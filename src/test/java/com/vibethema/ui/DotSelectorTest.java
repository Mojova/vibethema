package com.vibethema.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.AccessibleAttribute;
import javafx.scene.AccessibleRole;
import javafx.scene.control.Label;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class DotSelectorTest {

    @BeforeAll
    static void initToolkit() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        try {
            Platform.startup(latch::countDown);
        } catch (IllegalStateException e) {
            latch.countDown();
        }
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    void testAccessibilityAttributes() {
        IntegerProperty val = new SimpleIntegerProperty(3);
        Label label = new Label("Strength");
        DotSelector selector = new DotSelector(label, val, 1, 5);
        selector.descriptionProperty().set("Strength");

        // Verify Role
        assertEquals(AccessibleRole.SPINNER, selector.getAccessibleRole());
        assertEquals(
                "dots", selector.queryAccessibleAttribute(AccessibleAttribute.ROLE_DESCRIPTION));

        // Verify Attributes
        assertEquals(
                3.0, (Double) selector.queryAccessibleAttribute(AccessibleAttribute.VALUE), 0.001);
        assertEquals("3", selector.queryAccessibleAttribute(AccessibleAttribute.VALUE_STRING));
        assertEquals(
                1.0,
                (Double) selector.queryAccessibleAttribute(AccessibleAttribute.MIN_VALUE),
                0.001);
        assertEquals(
                5.0,
                (Double) selector.queryAccessibleAttribute(AccessibleAttribute.MAX_VALUE),
                0.01);
        assertEquals("3", selector.queryAccessibleAttribute(AccessibleAttribute.TEXT));
        assertEquals("Strength", selector.queryAccessibleAttribute(AccessibleAttribute.HELP));
        assertEquals("Strength", selector.accessibleTextProperty().get());
    }

    @Test
    void testAttributeUpdatesOnValueChange() {
        IntegerProperty val = new SimpleIntegerProperty(2);
        Label label = new Label("Expertise");
        DotSelector selector = new DotSelector(label, val, 1, 5);
        selector.descriptionProperty().set("Expertise");

        val.set(4);

        assertEquals(
                4.0, (Double) selector.queryAccessibleAttribute(AccessibleAttribute.VALUE), 0.001);
        assertEquals("4", selector.queryAccessibleAttribute(AccessibleAttribute.TEXT));
        assertEquals("4", selector.queryAccessibleAttribute(AccessibleAttribute.VALUE_STRING));
        assertEquals("Expertise", selector.accessibleTextProperty().get());
    }

    @Test
    void testAccessibleTextBinding() {
        IntegerProperty val = new SimpleIntegerProperty(3);
        Label label = new Label("Essence");
        DotSelector selector = new DotSelector(label, val, 1, 5);
        selector.descriptionProperty().set("Essence");

        assertEquals("Essence", selector.accessibleTextProperty().get());

        selector.descriptionProperty().set("Willpower");
        assertEquals("Willpower", selector.accessibleTextProperty().get());
    }
}
