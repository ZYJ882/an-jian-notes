package com.example.anjiannotes.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 动效档位契约：
 * 1) 四档按速度递增（极速 → 标准 → 舒展 → 沉浸），各时长单调变长；
 * 2) 极速档与既有 MotionTokens 完全一致，保证默认观感不变；
 * 3) 未知/缺失的持久化值回落到极速档，冷启动行为稳定。
 */
class MotionLevelTest {
    @Test
    fun fourLevelsAreStrictlyIncreasing() {
        val specs = MotionLevel.entries.map { it.spec }
        assertEquals("档位数量应为四档", 4, specs.size)
        specs.zipWithNext().forEach { (current, next) ->
            assertTrue("micro 未随档位递增", current.micro < next.micro)
            assertTrue("fast 未随档位递增", current.fast < next.fast)
            assertTrue("medium 未随档位递增", current.medium < next.medium)
        }
    }

    @Test
    fun fastestMatchesLegacyTokens() {
        val spec = MotionLevel.FASTEST.spec
        assertEquals("极速档 micro 应与旧常量一致", MotionTokens.DurationMicro, spec.micro)
        assertEquals("极速档 fast 应与旧常量一致", MotionTokens.DurationFast, spec.fast)
        assertEquals("极速档 medium 应与旧常量一致", MotionTokens.DurationMedium, spec.medium)
    }

    @Test
    fun fromKeyFallsBackToFastest() {
        assertEquals(MotionLevel.FASTEST, MotionLevel.fromKey(null))
        assertEquals(MotionLevel.FASTEST, MotionLevel.fromKey("unknown"))
        MotionLevel.entries.forEach { level ->
            assertEquals(level, MotionLevel.fromKey(level.key))
        }
    }
}
