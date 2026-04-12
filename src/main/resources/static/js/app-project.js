/**
 * Java All Call Graph Server 前端应用 - 项目管理模块
 * 包含：项目列表、项目详情、创建、编辑、删除、复制项目
 */

/**
 * 加载项目列表
 */
async function loadProjects() {
    try {
        const response = await fetch(`${API_BASE}/projects`);
        const result = await response.json();
        if (result.code === 200) {
            renderProjectList(result.data.projects);
        }
    } catch (error) {
        console.error('加载项目列表失败', error);
    }
}

/**
 * 渲染项目列表
 */
function renderProjectList(projects) {
    const container = document.getElementById('projectList');
    if (!projects || projects.length === 0) {
        container.innerHTML = '<div class="text-muted" style="padding: 1rem; text-align: center;">暂无项目</div>';
        return;
    }

    container.innerHTML = projects.map(project => `
        <div class="project-item ${currentProject && currentProject.projectId === project.projectId ? 'active' : ''}" 
             onclick="selectProject('${project.projectId}')">
            <div class="project-name">${project.description || project.projectId}</div>
            <div class="project-time">${project.createTime}</div>
        </div>
    `).join('');
}

/**
 * 选择项目
 */
async function selectProject(projectId) {
    try {
        const response = await fetch(`${API_BASE}/projects/${projectId}`);
        const result = await response.json();
        if (result.code === 200) {
            currentProject = result.data;
            showProjectDetail();
            loadTemplates(projectId);
            loadProjects(); // 刷新列表高亮
        }
    } catch (error) {
        console.error('加载项目详情失败', error);
        showToast('加载项目详情失败', 'error');
    }
}

/**
 * 显示项目详情
 */
function showProjectDetail() {
    document.getElementById('welcomePanel').classList.add('hidden');
    document.getElementById('projectDetail').classList.remove('hidden');
    document.getElementById('templateDetail').classList.add('hidden');

    document.getElementById('projectTitle').textContent = currentProject.description || '项目详情';
    document.getElementById('detailProjectId').textContent = currentProject.projectId;
    document.getElementById('detailDescription').textContent = currentProject.description || '-';
    document.getElementById('detailCreateTime').textContent = currentProject.createTime;
}

/**
 * 显示创建项目模态框
 */
function showCreateProjectModal() {
    document.getElementById('modalTitle').textContent = '新建项目';
    document.getElementById('modalHeaderActions').innerHTML = '';
    // 隐藏所有表单，显示新建项目表单
    hideAllModalForms();
    document.getElementById('createProjectForm').classList.remove('hidden');
    // 清空表单
    document.getElementById('projectDescription').value = '';
    document.getElementById('jarPaths').value = '';
    document.getElementById('modalConfirm').onclick = createProject;
    document.getElementById('modal').classList.remove('hidden');
}

/**
 * 创建项目
 */
async function createProject() {
    const description = document.getElementById('projectDescription').value;
    const jarPathsText = document.getElementById('jarPaths').value;
    const jarPaths = jarPathsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集配置数据（如果已打开配置编辑器）
    const javaCG2Config = collectJavaCG2Config();
    const jacgConfig = collectJACGConfig();

    const projectData = {
        description: description,
        javaCG2Config: {
            mainConfig: javaCG2Config ? javaCG2Config.mainConfig : {},
            listConfig: Object.assign(
                {},
                javaCG2Config ? javaCG2Config.listConfig : {},
                { 'OCFULE_JAR_DIR': jarPaths }
            ),
            setConfig: javaCG2Config ? javaCG2Config.setConfig : {},
            elConfig: javaCG2Config ? javaCG2Config.elConfig : {}
        },
        jacgConfig: jacgConfig || {
            mainConfig: {},
            dbConfig: {},
            listConfig: {},
            setConfig: {},
            elConfig: {}
        }
    };

    try {
        const response = await fetch(`${API_BASE}/projects`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(projectData)
        });
        const result = await response.json();
        if (result.code === 200) {
            closeModal();
            closeConfigModal();
            loadProjects();
            showToast('项目创建成功', 'success');
        } else {
            if (!handleApiError(result)) {
                showToast('创建失败: ' + result.message, 'error');
            }
        }
    } catch (error) {
        console.error('创建项目失败', error);
        showToast('创建项目失败', 'error');
    }
}

/**
 * 编辑项目
 */
function editProject() {
    // 获取当前项目的Jar路径配置
    const jarPaths = currentProject.javaCG2Config && currentProject.javaCG2Config.listConfig 
        ? (currentProject.javaCG2Config.listConfig['OCFULE_JAR_DIR'] || []) 
        : [];
    const jarPathsText = jarPaths.join('\n');
    
    document.getElementById('modalTitle').textContent = '编辑项目';
    document.getElementById('modalHeaderActions').innerHTML = `<button class="btn btn-secondary btn-sm" onclick="showEditProjectConfigEditor()">编辑配置参数</button>`;
    // 隐藏所有表单，显示编辑项目表单
    hideAllModalForms();
    document.getElementById('editProjectForm').classList.remove('hidden');
    // 填充数据
    document.getElementById('editProjectDescription').value = currentProject.description || '';
   document.getElementById('editJarPaths').value = jarPathsText;
    document.getElementById('modalConfirm').onclick = updateProject;
    document.getElementById('modal').classList.remove('hidden');
}

/**
 * 显示编辑项目配置编辑器
 */
function showEditProjectConfigEditor() {
    if (!configDefinitions) {
        showToast('配置定义加载中，请稍后重试', 'warning');
        return;
    }

    // 获取当前项目的配置值
    const currentJavaCG2Config = currentProject.javaCG2Config || {};
    const currentJacgConfig = currentProject.jacgConfig || {};

    // 定义所有配置Tab（按顺序）
    const tabConfigs = [
        { id: 'javacg2-main', name: 'JavaCG2主配置', type: 'main', source: 'javaCG2', render: () => renderConfigTableWithValues(configDefinitions.javaCG2.mainConfig, 'javaCG2.mainConfig', currentJavaCG2Config.mainConfig || {}) },
        { id: 'javacg2-list', name: 'JavaCG2列表配置', type: 'list', source: 'javaCG2', render: () => renderListConfigTableWithValues(configDefinitions.javaCG2.listConfig, 'javaCG2.listConfig', currentJavaCG2Config.listConfig || {}) },
        { id: 'javacg2-set', name: 'JavaCG2 Set配置', type: 'set', source: 'javaCG2', render: () => renderListConfigTableWithValues(configDefinitions.javaCG2.setConfig, 'javaCG2.setConfig', currentJavaCG2Config.setConfig || {}) },
        { id: 'javacg2-el', name: 'JavaCG2 EL配置', type: 'el', source: 'javaCG2', render: () => renderListConfigTableWithValues(configDefinitions.javaCG2.elConfig, 'javaCG2.elConfig', currentJavaCG2Config.elConfig || {}) },
        { id: 'jacg-main', name: 'JACG主配置', type: 'main', source: 'jacg', render: () => renderConfigTableWithValues(configDefinitions.jacg.mainConfig, 'jacg.mainConfig', currentJacgConfig.mainConfig || {}) },
        { id: 'jacg-db', name: 'JACG数据库配置', type: 'db', source: 'jacg', render: () => renderConfigTableWithValues(configDefinitions.jacg.dbConfig, 'jacg.dbConfig', currentJacgConfig.dbConfig || {}) },
        { id: 'jacg-list', name: 'JACG列表配置', type: 'list', source: 'jacg', render: () => renderListConfigTableWithValues(configDefinitions.jacg.listConfig, 'jacg.listConfig', currentJacgConfig.listConfig || {}) },
        { id: 'jacg-set', name: 'JACG Set配置', type: 'set', source: 'jacg', render: () => renderListConfigTableWithValues(configDefinitions.jacg.setConfig, 'jacg.setConfig', currentJacgConfig.setConfig || {}) },
        { id: 'jacg-el', name: 'JACG EL配置', type: 'el', source: 'jacg', render: () => renderListConfigTableWithValues(configDefinitions.jacg.elConfig, 'jacg.elConfig', currentJacgConfig.elConfig || {}) }
    ];

    // 生成Tab内容，检查是否有配置项
    const tabContents = tabConfigs.map(tab => {
        const content = tab.render();
        return { ...tab, content: content, hasContent: content && content.trim().length > 0 };
    });

    // 过滤出有内容的Tab
    const visibleTabs = tabContents.filter(tab => tab.hasContent);

    // 生成HTML
    let html = '<div class="tabs">';
    let firstVisible = true;
    visibleTabs.forEach(tab => {
        html += `<div class="tab ${firstVisible ? 'active' : ''}" onclick="switchConfigTab(this, '${tab.id}')">${tab.name}</div>`;
        firstVisible = false;
    });
    html += '</div>';

    // 生成Tab内容
    firstVisible = true;
    tabContents.forEach(tab => {
        if (tab.hasContent) {
            html += `<div id="tab-${tab.id}" class="tab-content ${firstVisible ? 'active' : ''}">${tab.content}</div>`;
            firstVisible = false;
        }
    });

    document.getElementById('configModalBody').innerHTML = html;
    document.getElementById('configModalTitle').textContent = '编辑项目配置参数';
    document.getElementById('configModal').classList.remove('hidden');
    
    // 初始化依赖关系
    setTimeout(initDependencyRules, 100);
}

/**
 * 更新项目
 */
async function updateProject() {
    const description = document.getElementById('editProjectDescription').value;
    const jarPathsText = document.getElementById('editJarPaths') ? document.getElementById('editJarPaths').value : '';
    const jarPaths = jarPathsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集配置数据（如果已打开配置编辑器）
    const javaCG2Config = collectJavaCG2Config();
    const jacgConfig = collectJACGConfig();

    // 合并Jar路径配置
    const finalListConfig = Object.assign(
        {},
        javaCG2Config ? javaCG2Config.listConfig : (currentProject.javaCG2Config ? currentProject.javaCG2Config.listConfig : {}),
        { 'OCFULE_JAR_DIR': jarPaths }
    );

    try {
        const response = await fetch(`${API_BASE}/projects/${currentProject.projectId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                description: description,
                javaCG2Config: {
                    mainConfig: javaCG2Config ? javaCG2Config.mainConfig : (currentProject.javaCG2Config ? currentProject.javaCG2Config.mainConfig : {}),
                    listConfig: finalListConfig,
                    setConfig: javaCG2Config ? javaCG2Config.setConfig : (currentProject.javaCG2Config ? currentProject.javaCG2Config.setConfig : {}),
                    elConfig: javaCG2Config ? javaCG2Config.elConfig : (currentProject.javaCG2Config ? currentProject.javaCG2Config.elConfig : {})
                },
                jacgConfig: jacgConfig || currentProject.jacgConfig || {}
            })
        });
        const result = await response.json();
        if (result.code === 200) {
            closeModal();
            closeConfigModal();
            selectProject(currentProject.projectId);
            showToast('更新成功', 'success');
        } else {
            if (!handleApiError(result)) {
                showToast('更新失败: ' + result.message, 'error');
            }
        }
    } catch (error) {
        console.error('更新项目失败', error);
        showToast('更新项目失败', 'error');
    }
}

/**
 * 删除项目
 */
async function deleteProject() {
    if (!confirm('确定要删除此项目吗？此操作不可恢复。')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/projects/${currentProject.projectId}`, {
            method: 'DELETE'
        });
        const result = await response.json();
        if (result.code === 200) {
            currentProject = null;
            document.getElementById('projectDetail').classList.add('hidden');
            document.getElementById('welcomePanel').classList.remove('hidden');
            loadProjects();
            showToast('删除成功', 'success');
        } else {
            showToast('删除失败: ' + result.message, 'error');
        }
    } catch (error) {
        console.error('删除项目失败', error);
        showToast('删除项目失败', 'error');
    }
}

/**
 * 复制项目
 */
function copyProject() {
    document.getElementById('modalTitle').textContent = '复制项目';
    document.getElementById('modalHeaderActions').innerHTML = '';
    // 隐藏所有表单，显示复制项目表单
    hideAllModalForms();
    document.getElementById('copyProjectForm').classList.remove('hidden');
    // 填充数据
    document.getElementById('copyProjectDescription').value = (currentProject.description || '') + '-副本';
    document.getElementById('modalConfirm').onclick = doCopyProject;
    document.getElementById('modal').classList.remove('hidden');
}

/**
 * 执行复制项目
 */
async function doCopyProject() {
    const description = document.getElementById('copyProjectDescription').value;

    try {
        const response = await fetch(`${API_BASE}/projects/${currentProject.projectId}/copy`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ description: description })
        });
        const result = await response.json();
        if (result.code === 200) {
            closeModal();
            loadProjects();
            showToast('复制成功', 'success');
        } else {
            showToast('复制失败: ' + result.message, 'error');
        }
    } catch (error) {
        console.error('复制项目失败', error);
        showToast('复制项目失败', 'error');
    }
}

/**
 * 执行静态分析
 */
async function executeAnalysis() {
    // 禁用项目详情页的所有操作按钮
    disableProjectButtons();
    
    // 显示执行中动态效果
    const executeBtn = document.querySelector('#projectDetail .panel-actions .btn-success');
    if (executeBtn) {
        executeBtn.innerHTML = '<span class="executing-spinner"></span> 执行中...';
    }

    try {
        const response = await fetch(`${API_BASE}/projects/${currentProject.projectId}/execute/analysis`, {
            method: 'POST'
        });
        const result = await response.json();
        if (result.code === 200) {
            showToast('静态分析已开始执行', 'info');
            // 开始轮询执行状态
            pollProjectExecutionStatus(currentProject.projectId, executeBtn);
        } else {
            showToast('执行失败: ' + result.message, 'error');
            resetExecuteButton(executeBtn, '执行静态分析');
            enableProjectButtons();
        }
    } catch (error) {
        console.error('执行静态分析失败', error);
        showToast('执行静态分析失败', 'error');
        resetExecuteButton(executeBtn, '执行静态分析');
        enableProjectButtons();
    }
}

/**
 * 禁用项目详情页的所有操作按钮
 * 禁用：执行静态分析、编辑、删除、复制
 * 不禁用：打开日志目录
 */
function disableProjectButtons() {
    const buttons = document.querySelectorAll('#projectDetail .panel-actions button');
    buttons.forEach(btn => {
        const onclick = btn.getAttribute('onclick');
        // 排除"打开日志目录"按钮，禁用其他所有按钮
        if (onclick && onclick.indexOf('openProjectLogDir') === -1) {
            btn.disabled = true;
        }
    });
    
    // 禁用模板列表中的所有按钮（包括模板的生成调用链等）
    disableAllTemplateButtons();
}

/**
 * 启用项目详情页的所有操作按钮
 */
function enableProjectButtons() {
    // 启用项目操作按钮
    const buttons = document.querySelectorAll('#projectDetail .panel-actions button');
    buttons.forEach(btn => {
        btn.disabled = false;
    });
    
    // 启用模板列表中的所有按钮
    enableAllTemplateButtons();
}

/**
 * 禁用所有模板相关的按钮（在模板详情页未打开时禁用模板列表中的按钮）
 */
function disableAllTemplateButtons() {
    // 禁用新建模板按钮
    const newTemplateBtn = document.querySelector('#projectDetail .panel-content button[onclick="showCreateTemplateModal()"]');
    if (newTemplateBtn) {
        newTemplateBtn.disabled = true;
    }
}

/**
 * 启用所有模板相关的按钮
 */
function enableAllTemplateButtons() {
    // 启用新建模板按钮
    const newTemplateBtn = document.querySelector('#projectDetail .panel-content button[onclick="showCreateTemplateModal()"]');
    if (newTemplateBtn) {
        newTemplateBtn.disabled = false;
    }
}

/**
 * 轮询项目执行状态
 */
async function pollProjectExecutionStatus(projectId, executeBtn) {
    try {
        const response = await fetch(`${API_BASE}/projects/${projectId}/executing`);
        const result = await response.json();
        if (result.code === 200) {
            const executing = result.data.executing;
            const duration = result.data.duration;
            const status = result.data.status;
            if (executing) {
                // 更新按钮显示当前耗时
                if (executeBtn && duration) {
                    executeBtn.innerHTML = `<span class="executing-spinner"></span> 执行中 ${formatDuration(duration)}`;
                }
                // 继续轮询
                setTimeout(() => pollProjectExecutionStatus(projectId, executeBtn), 1000);
            } else {
                // 执行完毕，启用所有按钮
                resetExecuteButton(executeBtn, '执行静态分析');
                enableProjectButtons();
                // 显示执行耗时
                const durationText = duration ? `，耗时 ${formatDuration(duration)}` : '';
                const statusText = status === 'completed' ? '执行完成' : '执行失败';
                showToast(`静态分析${statusText}${durationText}`, status === 'completed' ? 'success' : 'error');
            }
        } else {
            resetExecuteButton(executeBtn, '执行静态分析');
            enableProjectButtons();
        }
    } catch (error) {
        console.error('查询执行状态失败', error);
        // 继续轮询
        setTimeout(() => pollProjectExecutionStatus(projectId, executeBtn), 1000);
    }
}

/**
 * 重置执行按钮状态
 */
function resetExecuteButton(btn, text) {
    if (btn) {
        btn.disabled = false;
        btn.innerHTML = text;
    }
}

/**
 * 显示项目配置编辑器
 */
function showProjectConfigEditor() {
    if (!configDefinitions) {
        showToast('配置定义加载中，请稍后重试', 'warning');
        return;
    }

    let html = '<div class="tabs">';
    html += '<div class="tab active" onclick="switchConfigTab(this, \'javacg2-main\')">JavaCG2主配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'javacg2-list\')">JavaCG2列表配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'javacg2-set\')">JavaCG2 Set配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'javacg2-el\')">JavaCG2 EL配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-main\')">JACG主配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-db\')">JACG数据库配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-list\')">JACG列表配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-set\')">JACG Set配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-el\')">JACG EL配置</div>';
    html += '</div>';

    // JavaCG2主配置
    html += '<div id="tab-javacg2-main" class="tab-content active">';
    html += renderConfigTable(configDefinitions.javaCG2.mainConfig, 'javaCG2.mainConfig');
    html += '</div>';

    // JavaCG2列表配置
    html += '<div id="tab-javacg2-list" class="tab-content">';
    html += renderListConfigTable(configDefinitions.javaCG2.listConfig, 'javaCG2.listConfig');
    html += '</div>';

    // JavaCG2 Set配置
    html += '<div id="tab-javacg2-set" class="tab-content">';
    html += renderListConfigTable(configDefinitions.javaCG2.setConfig, 'javaCG2.setConfig');
    html += '</div>';

    // JavaCG2 EL配置
    html += '<div id="tab-javacg2-el" class="tab-content">';
    html += renderListConfigTable(configDefinitions.javaCG2.elConfig, 'javaCG2.elConfig');
    html += '</div>';

    // JACG主配置
    html += '<div id="tab-jacg-main" class="tab-content">';
    html += renderConfigTable(configDefinitions.jacg.mainConfig, 'jacg.mainConfig');
    html += '</div>';

    // JACG数据库配置
    html += '<div id="tab-jacg-db" class="tab-content">';
    html += renderConfigTable(configDefinitions.jacg.dbConfig, 'jacg.dbConfig');
    html += '</div>';

    // JACG列表配置
    html += '<div id="tab-jacg-list" class="tab-content">';
    html += renderListConfigTable(configDefinitions.jacg.listConfig, 'jacg.listConfig');
    html += '</div>';

    // JACG Set配置
    html += '<div id="tab-jacg-set" class="tab-content">';
    html += renderListConfigTable(configDefinitions.jacg.setConfig, 'jacg.setConfig');
    html += '</div>';

    // JACG EL配置
    html += '<div id="tab-jacg-el" class="tab-content">';
    html += renderListConfigTable(configDefinitions.jacg.elConfig, 'jacg.elConfig');
    html += '</div>';

    document.getElementById('configModalBody').innerHTML = html;
    document.getElementById('configModalTitle').textContent = '项目配置参数';
    document.getElementById('configModal').classList.remove('hidden');
    
    // 初始化依赖关系
    setTimeout(initDependencyRules, 100);
}