package com.github.adrninistrator.jacgserver.model.entity;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 项目列表实体
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public class ProjectListEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 项目列表
     */
    private List<ProjectListItem> projects = new ArrayList<>();

    public List<ProjectListItem> getProjects() {
        return projects;
    }

    public void setProjects(List<ProjectListItem> projects) {
        this.projects = projects;
    }

    /**
     * 项目列表项
     */
    public static class ProjectListItem implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 项目ID
         */
        private String projectId;

        /**
         * 项目描述
         */
        private String description;

        /**
         * 项目根目录（被解析的代码对应项目的根目录）
         */
        private String projectRootDir;

        /**
         * 创建时间
         */
        private String createTime;

        /**
         * 更新时间
         */
        private String updateTime;

        /**
         * 是否通过MCP创建
         */
        private boolean createdByMcp;

        public String getProjectId() {
            return projectId;
        }

        public void setProjectId(String projectId) {
            this.projectId = projectId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getCreateTime() {
            return createTime;
        }

        public void setCreateTime(String createTime) {
            this.createTime = createTime;
        }

        public String getUpdateTime() {
            return updateTime;
        }

        public void setUpdateTime(String updateTime) {
            this.updateTime = updateTime;
        }

        public String getProjectRootDir() {
            return projectRootDir;
        }

        public void setProjectRootDir(String projectRootDir) {
            this.projectRootDir = projectRootDir;
        }

        public boolean isCreatedByMcp() {
            return createdByMcp;
        }

        public void setCreatedByMcp(boolean createdByMcp) {
            this.createdByMcp = createdByMcp;
        }
    }
}
