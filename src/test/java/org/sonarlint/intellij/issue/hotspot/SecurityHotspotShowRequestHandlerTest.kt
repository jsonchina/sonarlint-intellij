/*
 * SonarLint for IntelliJ IDEA
 * Copyright (C) 2015-2020 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.issue.hotspot

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.sonarlint.intellij.AbstractSonarLintLightTests
import org.sonarlint.intellij.actions.SonarLintToolWindow
import org.sonarlint.intellij.any
import org.sonarlint.intellij.config.global.ServerConnection
import org.sonarlint.intellij.core.BoundProject
import org.sonarlint.intellij.core.ProjectBindingAssistant
import org.sonarlint.intellij.editor.SonarLintHighlighting
import org.sonarlint.intellij.eq
import org.sonarlint.intellij.ui.BalloonNotifier
import org.sonarsource.sonarlint.core.client.api.common.TextRange
import org.sonarsource.sonarlint.core.client.api.connected.RemoteHotspot
import org.sonarsource.sonarlint.core.client.api.connected.WsHelper
import java.util.Optional

const val FILE_PATH = "com/sonarsource/sample/MyFile.java"
const val CONNECTED_URL = "serverUrl"
const val PROJECT_KEY = "projectKey"
const val HOTSPOT_KEY = "hotspotKey"

private fun aRemoteHotspot(textRange: TextRange): RemoteHotspot {
    return RemoteHotspot("Very hotspot",
            FILE_PATH,
            textRange,
            "author",
            RemoteHotspot.Status.TO_REVIEW,
            null,
            RemoteHotspot.Rule("rulekey", "rulename", "category", RemoteHotspot.Rule.Probability.HIGH, "", "", ""))
}

class SecurityHotspotShowRequestHandlerTest : AbstractSonarLintLightTests() {
    private lateinit var projectBindingAssistant: ProjectBindingAssistant
    private lateinit var wsHelper: WsHelper
    private lateinit var balloonNotifier: BalloonNotifier
    private lateinit var toolWindow: SonarLintToolWindow
    private lateinit var highlighter: SonarLintHighlighting
    private lateinit var requestHandler: SecurityHotspotShowRequestHandler

    @Before
    fun prepare() {
        wsHelper = mock(WsHelper::class.java)
        projectBindingAssistant = mock(ProjectBindingAssistant::class.java)
        balloonNotifier = mock(BalloonNotifier::class.java)
        requestHandler = SecurityHotspotShowRequestHandler(projectBindingAssistant, wsHelper, balloonNotifier)
        toolWindow = mock(SonarLintToolWindow::class.java)
        highlighter = mock(SonarLintHighlighting::class.java)
        replaceProjectService(SonarLintToolWindow::class.java, toolWindow)
        replaceProjectService(SonarLintHighlighting::class.java, highlighter)
    }

    @Test
    fun it_should_do_nothing_when_there_is_no_bound_project() {
        `when`(projectBindingAssistant.bind(PROJECT_KEY, CONNECTED_URL)).thenReturn(null)

        requestHandler.open(PROJECT_KEY, HOTSPOT_KEY, CONNECTED_URL)

        verifyZeroInteractions(wsHelper)
    }

    @Test
    fun it_should_show_a_balloon_notification_when_an_error_occurs_when_fetching_hotspot_details() {
        val connection = ServerConnection.newBuilder().setHostUrl(CONNECTED_URL).build()
        `when`(projectBindingAssistant.bind(PROJECT_KEY, CONNECTED_URL)).thenReturn(BoundProject(project, connection))
        `when`(wsHelper.getHotspot(any(), any())).thenReturn(Optional.empty())

        requestHandler.open(PROJECT_KEY, HOTSPOT_KEY, CONNECTED_URL)

        verify(balloonNotifier).showBalloon(eq(project), eq("Cannot fetch hotspot details. Server is unreachable or credentials are invalid."), any())
    }

    @Test
    fun it_should_partially_display_a_hotspot_and_a_balloon_notification_if_file_is_not_found() {
        val connection = ServerConnection.newBuilder().setHostUrl(CONNECTED_URL).build()
        `when`(projectBindingAssistant.bind(PROJECT_KEY, CONNECTED_URL)).thenReturn(BoundProject(project, connection))
        val remoteHotspot = aRemoteHotspot(TextRange(1, 14, 1, 20))
        `when`(wsHelper.getHotspot(any(), any())).thenReturn(Optional.of(remoteHotspot))

        requestHandler.open(PROJECT_KEY, HOTSPOT_KEY, CONNECTED_URL)

        verify(toolWindow).show(eq(LocalHotspot(Location(null, null), remoteHotspot)), any())
        verifyZeroInteractions(highlighter)
        verify(balloonNotifier).showBalloon(eq(project), eq("Cannot find hotspot file in the project."), any())
    }

    @Test
    fun it_should_open_a_hotspot_file_if_found() {
        val connection = ServerConnection.newBuilder().setHostUrl(CONNECTED_URL).build()
        `when`(projectBindingAssistant.bind(PROJECT_KEY, CONNECTED_URL)).thenReturn(BoundProject(project, connection))
        val remoteHotspot = aRemoteHotspot(TextRange(1, 14, 1, 20))
        `when`(wsHelper.getHotspot(any(), any())).thenReturn(Optional.of(remoteHotspot))
        val file = myFixture.copyFileToProject(FILE_PATH)

        requestHandler.open(PROJECT_KEY, HOTSPOT_KEY, CONNECTED_URL)

        val localHotspotCaptor = ArgumentCaptor.forClass(LocalHotspot::class.java)
        verify(toolWindow).show(localHotspotCaptor.capture(), any())
        val localHotspot = localHotspotCaptor.value
        assertThat(localHotspot.primaryLocation.file).isEqualTo(file)
        assertThat(localHotspot.primaryLocation.range)
                .extracting("startOffset", "endOffset")
                .containsOnly(14, 20)
        verify(highlighter).highlight(localHotspot)
        assertThat(FileEditorManager.getInstance(project).openFiles)
                .extracting<String, RuntimeException> { obj: VirtualFile -> obj.name }
                .containsOnly("MyFile.java")
    }

    @Test
    fun it_should_show_a_balloon_notification_when_the_text_range_does_not_match() {
        val connection = ServerConnection.newBuilder().setHostUrl(CONNECTED_URL).build()
        `when`(projectBindingAssistant.bind(PROJECT_KEY, CONNECTED_URL)).thenReturn(BoundProject(project, connection))
        val remoteHotspot = aRemoteHotspot(TextRange(10, 14, 10, 20))
        `when`(wsHelper.getHotspot(any(), any())).thenReturn(Optional.of(remoteHotspot))
        val file = myFixture.copyFileToProject(FILE_PATH)

        requestHandler.open(PROJECT_KEY, HOTSPOT_KEY, CONNECTED_URL)

        val localHotspotCaptor = ArgumentCaptor.forClass(LocalHotspot::class.java)
        verify(toolWindow).show(localHotspotCaptor.capture(), any())
        val (primaryLocation) = localHotspotCaptor.value
        assertThat(primaryLocation.file).isEqualTo(file)
        assertThat(primaryLocation.range).isNull()
        verify(balloonNotifier).showBalloon(eq(project), eq("Source code in the file does not match. Make sure it corresponds to the code that had been analyzed."), any())
    }
}
