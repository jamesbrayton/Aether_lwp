package com.aether.wallpaper.shader

import com.aether.wallpaper.model.ParameterType
import com.aether.wallpaper.model.ShaderDescriptor
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for ShaderMetadataParser.
 * Tests the parsing of JavaDoc-style metadata comments from GLSL shader files.
 */
class ShaderMetadataParserTest {

    private lateinit var parser: ShaderMetadataParser

    @Before
    fun setUp() {
        parser = ShaderMetadataParser()
    }

    @Test
    fun `parse shader with complete metadata`() {
        val shaderSource = """
            /**
             * @shader Test Effect
             * @id test_effect
             * @version 1.0.0
             * @author Test Author
             * @source https://github.com/test/repo
             * @license MIT
             * @description A simple test shader for validation
             * @tags test, validation, simple
             * @minOpenGL 2.0
             *
             * @param u_intensity float 1.0 min=0.0 max=2.0 step=0.1 name="Intensity" desc="Effect intensity"
             * @param u_count int 100 min=10 max=200 step=10 name="Count" desc="Particle count"
             */

            precision mediump float;
            uniform sampler2D u_backgroundTexture;
            uniform float u_time;
            uniform vec2 u_resolution;
            uniform float u_intensity;
            uniform int u_count;

            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        val descriptor = parser.parse(shaderSource, "test.frag")

        assertEquals("test_effect", descriptor.id)
        assertEquals("Test Effect", descriptor.name)
        assertEquals("1.0.0", descriptor.version)
        assertEquals("Test Author", descriptor.author)
        assertEquals("https://github.com/test/repo", descriptor.source)
        assertEquals("MIT", descriptor.license)
        assertEquals("A simple test shader for validation", descriptor.description)
        assertEquals(listOf("test", "validation", "simple"), descriptor.tags)
        assertEquals("2.0", descriptor.minOpenGLVersion)
        assertEquals(2, descriptor.parameters.size)

        val intensityParam = descriptor.parameters[0]
        assertEquals("u_intensity", intensityParam.id)
        assertEquals("Intensity", intensityParam.name)
        assertEquals(ParameterType.FLOAT, intensityParam.type)
        assertEquals(1.0f, intensityParam.defaultValue)
        assertEquals(0.0f, intensityParam.minValue)
        assertEquals(2.0f, intensityParam.maxValue)
        assertEquals(0.1f, intensityParam.step)
        assertEquals("Effect intensity", intensityParam.description)

        val countParam = descriptor.parameters[1]
        assertEquals("u_count", countParam.id)
        assertEquals("Count", countParam.name)
        assertEquals(ParameterType.INT, countParam.type)
        assertEquals(100, countParam.defaultValue)
    }

    @Test
    fun `parse shader with minimal required metadata`() {
        val shaderSource = """
            /**
             * @shader Minimal Effect
             * @id minimal
             * @version 1.0.0
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        val descriptor = parser.parse(shaderSource, "minimal.frag")

        assertEquals("minimal", descriptor.id)
        assertEquals("Minimal Effect", descriptor.name)
        assertEquals("1.0.0", descriptor.version)
        assertNull(descriptor.author)
        assertNull(descriptor.source)
        assertNull(descriptor.license)
        assertNull(descriptor.description)
        assertTrue(descriptor.tags.isEmpty())
        assertEquals("2.0", descriptor.minOpenGLVersion) // default
        assertTrue(descriptor.parameters.isEmpty())
    }

    @Test
    fun `parse boolean parameter`() {
        val shaderSource = """
            /**
             * @shader Bool Test
             * @id bool_test
             * @version 1.0.0
             *
             * @param u_enabled bool true name="Enabled" desc="Enable effect"
             * @param u_disabled bool false name="Disabled"
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        val descriptor = parser.parse(shaderSource, "bool.frag")

        assertEquals(2, descriptor.parameters.size)

        val enabledParam = descriptor.parameters[0]
        assertEquals("u_enabled", enabledParam.id)
        assertEquals(ParameterType.BOOL, enabledParam.type)
        assertEquals(true, enabledParam.defaultValue)
        assertEquals("Enabled", enabledParam.name)

        val disabledParam = descriptor.parameters[1]
        assertEquals("u_disabled", disabledParam.id)
        assertEquals(false, disabledParam.defaultValue)
    }

    @Test(expected = ShaderParseException::class)
    fun `parse shader missing id tag throws exception`() {
        val shaderSource = """
            /**
             * @shader Invalid Shader
             * @version 1.0.0
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        parser.parse(shaderSource, "invalid.frag")
    }

    @Test(expected = ShaderParseException::class)
    fun `parse shader missing shader tag throws exception`() {
        val shaderSource = """
            /**
             * @id test
             * @version 1.0.0
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        parser.parse(shaderSource, "invalid.frag")
    }

    @Test(expected = ShaderParseException::class)
    fun `parse shader missing version tag throws exception`() {
        val shaderSource = """
            /**
             * @shader Test
             * @id test
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        parser.parse(shaderSource, "invalid.frag")
    }

    @Test(expected = ShaderParseException::class)
    fun `parse shader with invalid parameter type throws exception`() {
        val shaderSource = """
            /**
             * @shader Bad Param
             * @id bad_param
             * @version 1.0.0
             *
             * @param u_speed INVALID_TYPE 1.0
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        parser.parse(shaderSource, "bad.frag")
    }

    @Test
    fun `parse shader with no metadata comment returns error`() {
        val shaderSource = """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        try {
            parser.parse(shaderSource, "no-metadata.frag")
            fail("Expected ShaderParseException")
        } catch (e: ShaderParseException) {
            assertTrue(e.message?.contains("No metadata comment found") == true)
        }
    }

    @Test
    fun `parse tags with whitespace variations`() {
        val shaderSource = """
            /**
             * @shader Tag Test
             * @id tag_test
             * @version 1.0.0
             * @tags  winter,  snow ,particle,  weather
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        val descriptor = parser.parse(shaderSource, "tags.frag")

        assertEquals(listOf("winter", "snow", "particle", "weather"), descriptor.tags)
    }

    @Test
    fun `parse parameter without optional attributes`() {
        val shaderSource = """
            /**
             * @shader Simple Param
             * @id simple_param
             * @version 1.0.0
             *
             * @param u_value float 1.0
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        val descriptor = parser.parse(shaderSource, "simple.frag")

        val param = descriptor.parameters[0]
        assertEquals("u_value", param.id)
        assertEquals("u_value", param.name) // Should default to id if name not provided
        assertEquals(ParameterType.FLOAT, param.type)
        assertEquals(1.0f, param.defaultValue)
        assertNull(param.minValue)
        assertNull(param.maxValue)
        assertNull(param.step)
        assertEquals("", param.description)
    }

    @Test
    fun `parse multiline description`() {
        val shaderSource = """
            /**
             * @shader Multiline Test
             * @id multiline_test
             * @version 1.0.0
             * @description This is a longer description
             * that spans multiple lines
             */

            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0);
            }
        """.trimIndent()

        val descriptor = parser.parse(shaderSource, "multiline.frag")

        // Multiline support is optional for V1, but parser should handle gracefully
        assertNotNull(descriptor.description)
    }

    @Test
    fun `extractMetadataComment extracts JavaDoc block`() {
        val shaderSource = """
            /**
             * @shader Test
             * @id test
             * @version 1.0.0
             */

            precision mediump float;
            void main() {}
        """.trimIndent()

        val metadataComment = parser.extractMetadataComment(shaderSource)

        assertNotNull(metadataComment)
        assertTrue(metadataComment!!.contains("@shader Test"))
        assertTrue(metadataComment.contains("@id test"))
        assertTrue(metadataComment.contains("@version 1.0.0"))
    }

    @Test
    fun `parseTag extracts tag value correctly`() {
        val line = " * @shader Falling Snow  "
        val value = parser.parseTag(line, "shader")

        assertEquals("Falling Snow", value)
    }

    @Test
    fun `parseTag returns null for non-matching tag`() {
        val line = " * @shader Falling Snow"
        val value = parser.parseTag(line, "id")

        assertNull(value)
    }

    @Test
    fun `parseParameter parses complete parameter line`() {
        val line = " * @param u_speed float 1.5 min=0.0 max=5.0 step=0.1 name=\"Speed\" desc=\"Movement speed\""
        val param = parser.parseParameter(line)

        assertNotNull(param)
        assertEquals("u_speed", param?.id)
        assertEquals("Speed", param?.name)
        assertEquals(ParameterType.FLOAT, param?.type)
        assertEquals(1.5f, param?.defaultValue)
        assertEquals(0.0f, param?.minValue)
        assertEquals(5.0f, param?.maxValue)
        assertEquals(0.1f, param?.step)
        assertEquals("Movement speed", param?.description)
    }

    @Test
    fun `parse snow shader from actual file content`() {
        val shaderSource = """
/**
 * @shader Falling Snow
 * @id snow
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/aetherteam/aether-lwp-shaders
 * @license MIT
 * @description Gentle falling snow with lateral drift. Particles fall downward with subtle side-to-side motion, creating a peaceful winter atmosphere.
 * @tags winter, weather, particles, gentle
 * @minOpenGL 2.0
 *
 * @param u_particleCount float 100.0 min=10.0 max=200.0 step=1.0 name="Particle Count" desc="Number of visible snow particles"
 * @param u_speed float 1.0 min=0.1 max=3.0 step=0.1 name="Fall Speed" desc="How fast snow falls"
 * @param u_driftAmount float 0.5 min=0.0 max=1.0 step=0.05 name="Lateral Drift" desc="Amount of side-to-side wobble"
 */

precision mediump float;

uniform sampler2D u_backgroundTexture;
uniform float u_time;
void main() {
    gl_FragColor = vec4(1.0);
}
        """.trimIndent()

        val descriptor = parser.parse(shaderSource, "snow.frag")

        // Test that it parses without throwing
        assertNotNull(descriptor)
        assertEquals("snow", descriptor.id)
        assertEquals("Falling Snow", descriptor.name)
        assertEquals("1.0.0", descriptor.version)
        assertEquals(3, descriptor.parameters.size)

        // Test that validation passes
        descriptor.validate()

        // Check parameters
        val particleCount = descriptor.parameters[0]
        assertEquals("u_particleCount", particleCount.id)
        assertEquals(100.0f, particleCount.defaultValue)
        assertEquals(10.0f, particleCount.minValue)
        assertEquals(200.0f, particleCount.maxValue)
    }
}
