package io.mixeway.fortifyscaapi.pojo;

import java.util.List;

public class CreateScanRequest {
    private String repoUrl;
    private String groupName;
    private List<Project> projects;
    private String cloudCtrlToken;
    private String username;
    private String password;
    private int versionId;
    private Boolean single;
    private String dTrackUuid;
    private String sscUrl;

    public String getSscUrl() {
        return sscUrl;
    }

    public void setSscUrl(String sscUrl) {
        this.sscUrl = sscUrl;
    }

    public String getdTrackUuid() {
        return dTrackUuid;
    }

    public void setdTrackUuid(String dTrackUuid) {
        this.dTrackUuid = dTrackUuid;
    }

    public Boolean getSingle() {
        return single;
    }

    public void setSingle(Boolean single) {
        this.single = single;
    }

    public int getVersionId() {
        return versionId;
    }

    public void setVersionId(int versionId) {
        this.versionId = versionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getRepoUrl() {
        return repoUrl;
    }

    public void setRepoUrl(String repoUrl) {
        this.repoUrl = repoUrl;
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public List<Project> getProjects() {
        return projects;
    }

    public void setProjects(List<Project> projects) {
        this.projects = projects;
    }

    public String getCloudCtrlToken() {
        return cloudCtrlToken;
    }

    public void setCloudCtrlToken(String cloudCtrlToken) {
        this.cloudCtrlToken = cloudCtrlToken;
    }
}
