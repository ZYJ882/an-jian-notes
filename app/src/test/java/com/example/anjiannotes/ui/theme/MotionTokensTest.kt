package com.example.anjiannotes.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 动效规范契约：时长必须保持「快速轻量」量级，
 * 防止后续改动引入长动画拖慢交互节奏。
 */
class MotionTokensTest {
    @Test
    fun durationsStayFastAndLightweight() {
        assertTrue("微反馈时长过长", MotionTokens.DurationMicro <= 100)
        assertTrue("快速档时长过长", MotionTokens.DurationFast <= 150)
        assertTrue("中速档时长过长", MotionTokens.DurationMedium <= 200)
    }

    @Test
    fun durationsAreStrictlyOrdered() {
        assertTrue(MotionTokens.DurationMicro < MotionTokens.DurationFast)
        assertTrue(MotionTokens.DurationFast < MotionTokens.DurationMedium)
    }
}
