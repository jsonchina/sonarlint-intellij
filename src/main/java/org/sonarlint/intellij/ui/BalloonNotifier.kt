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
package org.sonarlint.intellij.ui

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationListener
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import javax.swing.event.HyperlinkEvent

open class BalloonNotifier {

    private val OPEN_IN_IDE_GROUP = NotificationGroup.balloonGroup("SonarLint: Open in IDE")

    open fun showBalloon(project: Project, message: String, link: Link? = null) {
        val balloon = OPEN_IN_IDE_GROUP.createNotification(
                "<b>Open in IDE event</b>",
                message + link?.let { "<br/><a href=\"${it.message}\">${it.message}</a>" },
                NotificationType.ERROR,
                object : NotificationListener.UrlOpeningListener(true) {
                    override fun hyperlinkActivated(notification: Notification, event: HyperlinkEvent) {
                        link?.run { onClicked() }
                    }
                })
        balloon.isImportant = true

        balloon.notify(project)
    }

    open class Link(val message: String, val onClicked: () -> Unit)

}
