package com.rewrite;

import org.junit.jupiter.api.Test;
import org.openrewrite.test.RecipeSpec;
import org.openrewrite.test.RewriteTest;

import static org.openrewrite.java.Assertions.java;

class VehicleToCarRecipeTest implements RewriteTest {

    // Configure the recipe to test in all cases
    @Override
    public void defaults(RecipeSpec spec) {
        spec.recipe(new VehicleToCarRecipe());
        // Make sure to enable type validation
//        spec.expectedCyclesThatMakeChanges(1);
    }

    @Test
    void classExtendingVehicleIsUpdated() {
        rewriteRun(
                java(
                        """
                                package com.example;
                                
                                public class Vehicle {}
                                public class Car {}
                                public class MyVehicle extends Vehicle {}
                                """,
                        """
                                package com.example;
                                
                                public class Vehicle {}
                                public class Car {}
                                public class MyVehicle extends Car {}
                                """
                )
        );
    }

    @Test
    void classExtendingGenericVehicleWithUndeclaredXIsUpdated() {
        rewriteRun(
                java(
                        """
                                package com.example;
                                
                                class X {}
                                
                                public class Vehicle<T> {}
                                public class Car<T> {}
                                public class MyVehicle extends Vehicle<X> {}
                                """,
                        """
                                package com.example;
                                
                                class X {}
                                
                                public class Vehicle<T> {}
                                public class Car<T> {}
                                public class MyVehicle extends Car<X> {}
                                """
                )
        );
    }


    @Test
    void classNotExtendingVehicleUnchanged() {
        rewriteRun(
                java(
                        """
                                package com.example;
                                public class NoVehicle {}
                                """
                )
        );
    }

}
