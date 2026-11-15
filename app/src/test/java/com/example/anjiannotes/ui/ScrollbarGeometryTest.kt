package com.example.anjiannotes.ui

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 滚动条滑块几何契约：把三处 Canvas 共用的「封顶 + 钳制 + 线性定位」规则
 * 固化为可回归测试，防止将来任一入口重新写回略有差异的公式。
 */
class ScrollbarGeometryTest {

    @Test
    fun thumbShorterThanViewport_keepsRequestedHeight() {
        val geometry = scrollbarThumbGeometry(viewportHeight = 200f, thumbHeight = 42f, scrollFraction = 0f)
        assertEquals(42f, geometry.height, 0f)
        assertEquals(0f, geometry.top, 0f)
    }

    @Test
    fun thumbTallerThanViewport_isCapped() {
        val geometry = scrollbarThumbGeometry(viewportHeight = 30f, thumbHeight = 42f, scrollFraction = 0.5f)
        assertEquals(30f, geometry.height, 0f)
        assertEquals(0f, geometry.top, 0f)
    }

    @Test
    fun startAndEndFractions_pinThumbToTrackEdges() {
        val start = scrollbarThumbGeometry(viewportHeight = 200f, thumbHeight = 40f, scrollFraction = 0f)
        val end = scrollbarThumbGeometry(viewportHeight = 200f, thumbHeight = 40f, scrollFraction = 1f)
        assertEquals(0f, start.top, 0f)
        assertEquals(160f, end.top, 0f)
    }

    @Test
    fun middleFraction_positionsThumbWithinTrack() {
        val geometry = scrollbarThumbGeometry(viewportHeight = 200f, thumbHeight = 40f, scrollFraction = 0.25f)
        assertEquals(40f, geometry.top, 0f)
    }

    @Test
    fun outOfRangeFractions_areClamped() {
        val beforeStart = scrollbarThumbGeometry(viewportHeight = 200f, thumbHeight = 40f, scrollFraction = -1f)
        val afterEnd = scrollbarThumbGeometry(viewportHeight = 200f, thumbHeight = 40f, scrollFraction = 2f)
        assertEquals(0f, beforeStart.top, 0f)
        assertEquals(160f, afterEnd.top, 0f)
    }

    @Test
    fun zeroHeightViewport_doesNotProduceNegativeGeometry() {
        val geometry = scrollbarThumbGeometry(viewportHeight = 0f, thumbHeight = 42f, scrollFraction = 1f)
        assertEquals(0f, geometry.height, 0f)
        assertEquals(0f, geometry.top, 0f)
    }
}
