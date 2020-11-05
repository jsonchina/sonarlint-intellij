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
package org.sonarlint.intellij.config.global.wizard;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import org.sonarlint.intellij.config.global.ServerConnection;
import org.sonarlint.intellij.util.SonarLintUtils;
import org.sonarsource.sonarlint.core.client.api.connected.RemoteOrganization;

public class WizardModel {
  private static final String SONARCLOUD_URL = "https://sonarcloud.io";
  private ServerType serverType;
  private String serverUrl;
  private String token;
  private String login;
  private char[] password;
  private String name;
  private String organizationKey;
  private boolean proxyEnabled;
  private boolean notificationsDisabled = false;
  private boolean notificationsSupported = false;

  private List<RemoteOrganization> organizationList = new ArrayList<>();

  public enum ServerType {
    SONARCLOUD,
    SONARQUBE
  }

  public WizardModel() {

  }

  public WizardModel(ServerConnection connectionToEdit) {
    if (SonarLintUtils.isSonarCloudAlias(connectionToEdit.getHostUrl())) {
      serverType = ServerType.SONARCLOUD;
    } else {
      serverType = ServerType.SONARQUBE;
      serverUrl = connectionToEdit.getHostUrl();
    }
    this.proxyEnabled = connectionToEdit.enableProxy();
    this.token = connectionToEdit.getToken();
    this.login = connectionToEdit.getLogin();
    String pass = connectionToEdit.getPassword();
    if (pass != null) {
      this.password = pass.toCharArray();
    }
    this.organizationKey = connectionToEdit.getOrganizationKey();
    this.notificationsDisabled = connectionToEdit.isDisableNotifications();
    this.name = connectionToEdit.getName();
  }

  @CheckForNull
  public ServerType getServerType() {
    return serverType;
  }

  public WizardModel setServerType(ServerType serverType) {
    this.serverType = serverType;
    return this;
  }

  public boolean isNotificationsSupported() {
    return notificationsSupported;
  }

  public WizardModel setNotificationsSupported(boolean notificationsSupported) {
    this.notificationsSupported = notificationsSupported;
    return this;
  }

  public boolean isNotificationsDisabled() {
    return notificationsDisabled;
  }

  public WizardModel setNotificationsDisabled(boolean notificationsDisabled) {
    this.notificationsDisabled = notificationsDisabled;
    return this;
  }

  public boolean isProxyEnabled() {
    return proxyEnabled;
  }

  public WizardModel setProxyEnabled(boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
    return this;
  }

  @CheckForNull
  public List<RemoteOrganization> getOrganizationList() {
    return organizationList;
  }

  public WizardModel setOrganizationList(List<RemoteOrganization> organizationList) {
    this.organizationList = organizationList;
    return this;
  }

  @CheckForNull
  public String getServerUrl() {
    return serverUrl;
  }

  public WizardModel setServerUrl(@Nullable String serverUrl) {
    this.serverUrl = serverUrl;
    return this;
  }

  @CheckForNull
  public String getToken() {
    return token;
  }

  public WizardModel setToken(@Nullable String token) {
    this.token = token;
    return this;
  }

  @CheckForNull
  public String getLogin() {
    return login;
  }

  public WizardModel setLogin(@Nullable String login) {
    this.login = login;
    return this;
  }

  @CheckForNull
  public char[] getPassword() {
    return password;
  }

  public WizardModel setPassword(@Nullable char[] password) {
    this.password = password;
    return this;
  }

  @CheckForNull
  public String getName() {
    return name;
  }

  public WizardModel setName(String name) {
    this.name = name;
    return this;
  }

  @CheckForNull
  public String getOrganizationKey() {
    return organizationKey;
  }

  public WizardModel setOrganizationKey(@Nullable String organization) {
    this.organizationKey = organization;
    return this;
  }

  public ServerConnection createServerWithoutOrganization() {
    return createServer(null);
  }

  public ServerConnection createServer() {
    return createServer(organizationKey);
  }

  private ServerConnection createServer(@Nullable String organizationKey) {
    ServerConnection.Builder builder = ServerConnection.newBuilder()
      .setOrganizationKey(organizationKey)
      .setEnableProxy(proxyEnabled)
      .setName(name);

    if (serverType == ServerType.SONARCLOUD) {
      builder.setHostUrl(SONARCLOUD_URL);

    } else {
      builder.setHostUrl(serverUrl);
    }

    if (token != null) {
      builder.setToken(token)
        .setLogin(null)
        .setPassword(null);
    } else {
      builder.setToken(null)
        .setLogin(login)
        .setPassword(new String(password));
    }
    builder.setDisableNotifications(notificationsDisabled);
    return builder.build();
  }
}
