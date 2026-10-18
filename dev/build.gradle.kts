
plugins {
    id("com.gtnewhorizons.gtnhconvention")
}

// JUnit 5 for the plain Java units (fixes/hosting)
tasks.test { useJUnitPlatform() }
