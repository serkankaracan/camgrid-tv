plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("app/src/**/*.kt")
        ktfmt("0.63").kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts", "app/*.gradle.kts")
        ktfmt("0.63").kotlinlangStyle()
        trimTrailingWhitespace()
        endWithNewline()
    }
    format("projectFiles") {
        target(
            "*.md",
            "docs/**/*.md",
            ".gitignore",
            ".gitattributes",
            "*.properties",
            "gradle/**/*.toml",
            ".github/**/*.yml",
            ".github/**/*.yaml",
            "scripts/**/*.ps1",
            "app/src/**/*.xml",
        )
        trimTrailingWhitespace()
        endWithNewline()
    }
}
