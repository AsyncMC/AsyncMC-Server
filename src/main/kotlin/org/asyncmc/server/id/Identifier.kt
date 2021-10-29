package org.asyncmc.server.id

public interface Identifier {
    public companion object {
        public val VALID_PATTERN: Regex = Regex("""^[a-z]+[0-9]?(_[a-z]+[0-9]?)$""")
    }
}
