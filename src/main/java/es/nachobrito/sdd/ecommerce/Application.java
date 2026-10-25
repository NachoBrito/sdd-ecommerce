package es.nachobrito.sdd.ecommerce;

import io.micronaut.runtime.Micronaut;

/**
 * Main application entrypoint for SDD E-Commerce REST API.
 */
public final class Application {

    private Application() {
        // Prevent instantiation of utility class
    }

    /**
     * Entry point for running the application.
     *
     * @param args Command line arguments
     */
    public static void main(final String[] args) {
        Micronaut.run(Application.class, args);
    }
}
