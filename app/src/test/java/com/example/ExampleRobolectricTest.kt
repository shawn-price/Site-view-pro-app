package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.JobMode
import com.example.viewmodel.CameraHudViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SiteView Pro", appName)
  }

  @Test
  fun `verify job mode switching and calculations`() {
    val viewModel = CameraHudViewModel()
    assertEquals(JobMode.PAINTING, viewModel.uiState.value.activeJobMode)

    viewModel.setJobMode(JobMode.PLASTERING)
    assertEquals(JobMode.PLASTERING, viewModel.uiState.value.activeJobMode)

    viewModel.setJobMode(JobMode.SCREEDING)
    assertEquals(JobMode.SCREEDING, viewModel.uiState.value.activeJobMode)

    assertTrue(viewModel.uiState.value.targetPoints.isNotEmpty())
    assertNotNull(viewModel.uiState.value.jobCalculationSummary)
  }

  @Test
  fun `verify military binocular zoom and pinch gestures`() {
    val viewModel = CameraHudViewModel()
    assertEquals(1.0f, viewModel.uiState.value.zoomLevel)

    // Set discrete military binocular steps
    viewModel.setZoomLevel(2.0f)
    assertEquals(2.0f, viewModel.uiState.value.zoomLevel)

    viewModel.setZoomLevel(4.0f)
    assertEquals(4.0f, viewModel.uiState.value.zoomLevel)

    viewModel.setZoomLevel(8.0f)
    assertEquals(8.0f, viewModel.uiState.value.zoomLevel)

    // Cycle binocular zoom
    viewModel.cycleBinocularZoom()
    assertEquals(1.0f, viewModel.uiState.value.zoomLevel)

    // Test continuous pinch-to-zoom
    viewModel.onPinchZoom(1.5f)
    assertEquals(1.5f, viewModel.uiState.value.zoomLevel, 0.05f)

    // Max clamp test
    viewModel.onPinchZoom(10.0f)
    assertEquals(8.0f, viewModel.uiState.value.zoomLevel)

    // Min clamp test
    viewModel.onPinchZoom(0.01f)
    assertEquals(1.0f, viewModel.uiState.value.zoomLevel)
  }
}
