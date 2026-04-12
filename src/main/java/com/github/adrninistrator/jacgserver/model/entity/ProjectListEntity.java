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
         * 创建时间
         */
        private String createTime;

        /**
         * 更新时间
         */
        private String updateTime;

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
    }
}
