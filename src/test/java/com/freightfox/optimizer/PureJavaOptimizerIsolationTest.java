package com.freightfox.optimizer;

import com.freightfox.optimizer.engine.AssignmentOptimizer;
import com.freightfox.optimizer.model.Lane;
import com.freightfox.optimizer.model.LaneAssignment;
import com.freightfox.optimizer.model.LaneQuote;
import com.freightfox.optimizer.model.OptimizationRequest;
import com.freightfox.optimizer.model.OptimizationResult;
import com.freightfox.optimizer.model.Transporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PureJavaOptimizerIsolationTest {

    @Test
    @DisplayName("REQ-09: Verify pure Java optimizer has zero dependencies on Spring, JPA, HTTP, or Database classes")
    void testPureJavaIsolation() {
        List<Class<?>> optimizerClasses = List.of(
                AssignmentOptimizer.class,
                Lane.class,
                Transporter.class,
                LaneQuote.class,
                LaneAssignment.class,
                OptimizationRequest.class,
                OptimizationResult.class
        );

        for (Class<?> clazz : optimizerClasses) {
            String packageName = clazz.getPackageName();
            assertThat(packageName).startsWith("com.freightfox.optimizer");

            for (Method method : clazz.getDeclaredMethods()) {
                assertNonInfrastructureType(method.getReturnType(), clazz.getSimpleName() + "#" + method.getName() + " return");
                for (Class<?> param : method.getParameterTypes()) {
                    assertNonInfrastructureType(param, clazz.getSimpleName() + "#" + method.getName() + " param");
                }
            }

            for (Field field : clazz.getDeclaredFields()) {
                assertNonInfrastructureType(field.getType(), clazz.getSimpleName() + "#" + field.getName());
            }
        }
    }

    private void assertNonInfrastructureType(Class<?> type, String context) {
        String name = type.getName();
        assertThat(name)
                .as("Type %s in context %s must not depend on Spring", name, context)
                .doesNotContain("org.springframework")
                .as("Type %s in context %s must not depend on JPA / Hibernate", name, context)
                .doesNotContain("jakarta.persistence")
                .doesNotContain("org.hibernate")
                .as("Type %s in context %s must not depend on SQL / JDBC", name, context)
                .doesNotContain("java.sql")
                .as("Type %s in context %s must not depend on HTTP Servlet", name, context)
                .doesNotContain("jakarta.servlet");
    }
}
