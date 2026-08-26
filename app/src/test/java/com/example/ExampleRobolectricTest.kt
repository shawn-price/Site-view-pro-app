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
}
