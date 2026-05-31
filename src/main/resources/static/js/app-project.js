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
    document.getElementById('detailProjectRootDir').textContent = currentProject.projectRootDir || '-';
    document.getElementById('detailCreateTime').textContent = currentProject.createTime;
}

/**
 * 显示创建项目模态框
 */
function showCreateProjectModal() {
    document.getElementById('modalTitle').textContent = '新建项目';
    document.getElementById('modalHeaderActions').innerHTML = '';
    // 隐藏所有表单，显示创建项目表单
    hideAllModalForms();
    document.getElementById('createProjectForm').classList.remove('hidden');
    // 清空表单
    document.getElementById('createProjectDescription').value = '';
    document.getElementById('createProjectRootDir').value = '';
    document.getElementById('createJarPaths').value = '';
    document.getElementById('modalConfirm').onclick = createProject;
    document.getElementById('modal').classList.remove('hidden');
}

/**
 * 创建项目
 */
async function createProject() {
    const description = document.getElementById('createProjectDescription').value;
    const projectRootDir = document.getElementById('createProjectRootDir').value;
    const jarPathsText = document.getElementById('createJarPaths').value;
    const jarPaths = jarPathsText.split('\n').map(p => p.trim()).filter(p => p);

    if (!description || !description.trim()) {
        showToast('项目描述不能为空', 'warning');
        return;
    }

    if (jarPaths.length === 0) {
        showToast('Jar/Class 文件路径不能为空', 'warning');
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/projects`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                description: description,
                projectRootDir: projectRootDir || null,
                javacg2Config: {
                    mainConfig: {},
                    listConfig: { [Javacg2Enum.OCFULE_JAR_DIR]: jarPaths },
                    setConfig: {},
                    elConfig: {}
                },
                jacgConfig: {
                    mainConfig: {},
                    dbConfig: {},
                    listConfig: {},
                    setConfig: {},
                    elConfig: {}
                }
            })
        });
        const result = await response.json();
        if (result.code === 200) {
            closeModal();
            loadProjects();
            showToast('创建成功', 'success');
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
    const jarPaths = currentProject.javacg2Config && currentProject.javacg2Config.listConfig 
        ? (currentProject.javacg2Config.listConfig[Javacg2Enum.OCFULE_JAR_DIR] || []) 
        : [];
    const jarPathsText = jarPaths.join('\n');
    
    document.getElementById('modalTitle').textContent = '编辑项目';
    document.getElementById('modalHeaderActions').innerHTML = `<button class="btn btn-secondary btn-sm" onclick="showEditProjectConfigEditor()">编辑配置参数</button>`;
    // 隐藏所有表单，显示编辑项目表单
    hideAllModalForms();
    document.getElementById('editProjectForm').classList.remove('hidden');
    // 填充数据
    document.getElementById('editProjectDescription').value = currentProject.description || '';
    document.getElementById('editProjectRootDir').value = currentProject.projectRootDir || '';
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
    const currentJavacg2Config = currentProject.javacg2Config || {};
    const currentJacgConfig = currentProject.jacgConfig || {};

    // 定义所有配置Tab（按顺序）
    const tabConfigs = [
        { id: 'javacg2-main', name: 'javacg2主配置', type: 'main', source: 'javacg2', render: () => renderConfigTableWithValues(configDefinitions.javacg2.mainConfig, 'javacg2.mainConfig', currentJavacg2Config.mainConfig || {}) },
        { id: 'javacg2-list', name: 'javacg2列表配置', type: 'list', source: 'javacg2', render: () => renderListConfigTableWithValues(configDefinitions.javacg2.listConfig, 'javacg2.listConfig', currentJavacg2Config.listConfig || {}) },
        { id: 'javacg2-set', name: 'javacg2 Set配置', type: 'set', source: 'javacg2', render: () => renderListConfigTableWithValues(configDefinitions.javacg2.setConfig, 'javacg2.setConfig', currentJavacg2Config.setConfig || {}) },
        { id: 'javacg2-el', name: 'javacg2 EL配置', type: 'el', source: 'javacg2', render: () => renderListConfigTableWithValues(configDefinitions.javacg2.elConfig, 'javacg2.elConfig', currentJavacg2Config.elConfig || {}) },
        { id: 'jacg-main', name: 'jacg主配置', type: 'main', source: 'jacg', render: () => renderConfigTableWithValues(configDefinitions.jacg.mainConfig, 'jacg.mainConfig', currentJacgConfig.mainConfig || {}) },
        { id: 'jacg-db', name: 'jacg数据库配置', type: 'db', source: 'jacg', render: () => renderConfigTableWithValues(configDefinitions.jacg.dbConfig, 'jacg.dbConfig', currentJacgConfig.dbConfig || {}) },
        { id: 'jacg-list', name: 'jacg列表配置', type: 'list', source: 'jacg', render: () => renderListConfigTableWithValues(configDefinitions.jacg.listConfig, 'jacg.listConfig', currentJacgConfig.listConfig || {}) },
        { id: 'jacg-set', name: 'jacg Set配置', type: 'set', source: 'jacg', render: () => renderListConfigTableWithValues(configDefinitions.jacg.setConfig, 'jacg.setConfig', currentJacgConfig.setConfig || {}) },
        { id: 'jacg-el', name: 'jacg EL配置', type: 'el', source: 'jacg', render: () => renderListConfigTableWithValues(configDefinitions.jacg.elConfig, 'jacg.elConfig', currentJacgConfig.elConfig || {}) }
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
    const projectRootDir = document.getElementById('editProjectRootDir') ? document.getElementById('editProjectRootDir').value : '';
    const jarPathsText = document.getElementById('editJarPaths') ? document.getElementById('editJarPaths').value : '';
    const jarPaths = jarPathsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集配置数据（如果已打开配置编辑器）
    const javacg2Config = collectJavacg2Config();
    const jacgConfig = collectJacgConfig();

    // 合并Jar路径配置
    const finalListConfig = Object.assign(
        {},
        javacg2Config ? javacg2Config.listConfig : (currentProject.javacg2Config ? currentProject.javacg2Config.listConfig : {}),
        { [Javacg2Enum.OCFULE_JAR_DIR]: jarPaths }
    );

    try {
        const response = await fetch(`${API_BASE}/projects/${currentProject.projectId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                description: description,
                projectRootDir: projectRootDir || null,
                javacg2Config: {
                    mainConfig: javacg2Config ? javacg2Config.mainConfig : (currentProject.javacg2Config ? currentProject.javacg2Config.mainConfig : {}),
                    listConfig: finalListConfig,
                    setConfig: javacg2Config ? javacg2Config.setConfig : (currentProject.javacg2Config ? currentProject.javacg2Config.setConfig : {}),
                    elConfig: javacg2Config ? javacg2Config.elConfig : (currentProject.javacg2Config ? currentProject.javacg2Config.elConfig : {})
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
 * 不禁用：打开日志目录、查询执行记录
 */
function disableProjectButtons() {
    const buttons = document.querySelectorAll('#projectDetail .panel-actions button');
    buttons.forEach(btn => {
        const onclick = btn.getAttribute('onclick');
        // 排除"打开日志目录"和"查询执行记录"按钮，禁用其他所有按钮
        if (onclick && onclick.indexOf('openProjectLogDir') === -1 && onclick.indexOf('showExecutionRecordModal') === -1) {
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

// 批量管理状态
let batchProjects = [];
let batchSelectedProjectIds = new Set();

/**
 * 显示批量管理模态框
 */
function showBatchManageModal() {
    batchSelectedProjectIds = new Set();
    loadBatchProjectList();
    document.getElementById('batchManageModal').classList.remove('hidden');
}

/**
 * 关闭批量管理模态框
 */
function closeBatchManageModal() {
    document.getElementById('batchManageModal').classList.add('hidden');
    batchSelectedProjectIds = new Set();
}

/**
 * 加载批量管理项目列表
 */
async function loadBatchProjectList() {
    try {
        const response = await fetch(`${API_BASE}/projects`);
        const result = await response.json();
        if (result.code === 200) {
            batchProjects = result.data.projects || [];
            renderBatchProjectList();
            updateBatchSelectAllState();
            updateBatchDeleteButton();
        }
    } catch (error) {
        console.error('加载批量管理项目列表失败', error);
        showToast('加载项目列表失败', 'error');
    }
}

/**
 * 渲染批量管理项目列表
 */
function renderBatchProjectList() {
    const container = document.getElementById('batchProjectList');
    if (!batchProjects || batchProjects.length === 0) {
        container.innerHTML = '<div class="text-muted" style="padding: 1rem; text-align: center;">暂无项目</div>';
        return;
    }

    container.innerHTML = batchProjects.map(project => `
        <div class="batch-project-item ${batchSelectedProjectIds.has(project.projectId) ? 'selected' : ''}" 
             onclick="toggleBatchProject('${project.projectId}')">
            <input type="checkbox" class="batch-project-checkbox" 
                   ${batchSelectedProjectIds.has(project.projectId) ? 'checked' : ''}
                   onchange="toggleBatchProject('${project.projectId}')">
            <div class="batch-project-info">
                <div class="batch-project-name">${project.description || project.projectId}</div>
                <div class="batch-project-meta">ID: ${project.projectId} | ${project.createTime || ''}</div>
            </div>
        </div>
    `).join('');

    updateBatchSelectedCount();
}

/**
 * 切换批量选择项目
 */
function toggleBatchProject(projectId) {
    if (batchSelectedProjectIds.has(projectId)) {
        batchSelectedProjectIds.delete(projectId);
    } else {
        batchSelectedProjectIds.add(projectId);
    }
    renderBatchProjectList();
    updateBatchSelectAllState();
    updateBatchDeleteButton();
}

/**
 * 全选/取消全选
 */
function toggleBatchSelectAll() {
    const selectAll = document.getElementById('batchSelectAll').checked;
    if (selectAll) {
        batchProjects.forEach(p => batchSelectedProjectIds.add(p.projectId));
    } else {
        batchSelectedProjectIds.clear();
    }
    renderBatchProjectList();
    updateBatchDeleteButton();
}

/**
 * 更新全选状态
 */
function updateBatchSelectAllState() {
    const selectAll = document.getElementById('batchSelectAll');
    if (!selectAll) return;
    if (batchProjects.length > 0 && batchSelectedProjectIds.size === batchProjects.length) {
        selectAll.checked = true;
        selectAll.indeterminate = false;
    } else if (batchSelectedProjectIds.size > 0) {
        selectAll.checked = false;
        selectAll.indeterminate = true;
    } else {
        selectAll.checked = false;
        selectAll.indeterminate = false;
    }
}

/**
 * 更新批量删除按钮状态
 */
function updateBatchDeleteButton() {
    const btn = document.getElementById('batchDeleteBtn');
    if (btn) {
        btn.disabled = batchSelectedProjectIds.size === 0;
    }
}

/**
 * 更新已选择数量
 */
function updateBatchSelectedCount() {
    const countEl = document.getElementById('batchSelectedCount');
    if (countEl) {
        countEl.textContent = `已选择 ${batchSelectedProjectIds.size} 个项目`;
    }
}

/**
 * 批量删除选中项目
 */
async function batchDeleteSelectedProjects() {
    if (batchSelectedProjectIds.size === 0) {
        showToast('请先选择要删除的项目', 'warning');
        return;
    }

    if (!confirm(`确定要删除选中的 ${batchSelectedProjectIds.size} 个项目吗？此操作不可恢复。`)) {
        return;
    }

    const projectIds = Array.from(batchSelectedProjectIds);
    const deleteBtn = document.getElementById('batchDeleteBtn');
    deleteBtn.disabled = true;
    deleteBtn.textContent = '删除中...';

    try {
        const response = await fetch(`${API_BASE}/projects/batch-delete`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ projectIds: projectIds })
        });
        const result = await response.json();
        if (result.code === 200) {
            const data = result.data;
            const msg = `批量删除完成：成功 ${data.successCount} 个，失败 ${data.failCount} 个`;
            showToast(msg, data.failCount > 0 ? 'warning' : 'success');
            
            // 如果有失败项，显示详细信息
            if (data.errors && data.errors.length > 0) {
                const errorDetails = data.errors.map(e => `${e.projectId}: ${e.error}`).join('\n');
                console.warn('批量删除失败详情:\n' + errorDetails);
            }
            
            // 清除已删除的选中项
            batchSelectedProjectIds.clear();
            // 刷新列表
            loadBatchProjectList();
            loadProjects();
            // 如果当前选中的项目被删除了，回到欢迎页
            if (currentProject && projectIds.includes(currentProject.projectId)) {
                currentProject = null;
                document.getElementById('projectDetail').classList.add('hidden');
                document.getElementById('templateDetail').classList.add('hidden');
                document.getElementById('welcomePanel').classList.remove('hidden');
            }
        } else {
            showToast('批量删除失败: ' + (result.message || '未知错误'), 'error');
        }
    } catch (error) {
        console.error('批量删除项目失败', error);
        showToast('批量删除项目失败', 'error');
    } finally {
        deleteBtn.disabled = batchSelectedProjectIds.size === 0;
        deleteBtn.textContent = '删除选中';
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
    html += '<div class="tab active" onclick="switchConfigTab(this, \'javacg2-main\')">javacg2主配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'javacg2-list\')">javacg2列表配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'javacg2-set\')">javacg2 Set配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'javacg2-el\')">javacg2 EL配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-main\')">jacg主配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-db\')">jacg数据库配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-list\')">jacg列表配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-set\')">jacg Set配置</div>';
    html += '<div class="tab" onclick="switchConfigTab(this, \'jacg-el\')">jacg EL配置</div>';
    html += '</div>';

    // javacg2主配置
    html += '<div id="tab-javacg2-main" class="tab-content active">';
    html += renderConfigTable(configDefinitions.javacg2.mainConfig, 'javacg2.mainConfig');
    html += '</div>';

    // javacg2列表配置
    html += '<div id="tab-javacg2-list" class="tab-content">';
    html += renderListConfigTable(configDefinitions.javacg2.listConfig, 'javacg2.listConfig');
    html += '</div>';

    // javacg2 Set配置
    html += '<div id="tab-javacg2-set" class="tab-content">';
    html += renderListConfigTable(configDefinitions.javacg2.setConfig, 'javacg2.setConfig');
    html += '</div>';

    // javacg2 EL配置
    html += '<div id="tab-javacg2-el" class="tab-content">';
    html += renderListConfigTable(configDefinitions.javacg2.elConfig, 'javacg2.elConfig');
    html += '</div>';

    // jacg主配置
    html += '<div id="tab-jacg-main" class="tab-content">';
    html += renderConfigTable(configDefinitions.jacg.mainConfig, 'jacg.mainConfig');
    html += '</div>';

    // jacg数据库配置
    html += '<div id="tab-jacg-db" class="tab-content">';
    html += renderConfigTable(configDefinitions.jacg.dbConfig, 'jacg.dbConfig');
    html += '</div>';

    // jacg列表配置
    html += '<div id="tab-jacg-list" class="tab-content">';
    html += renderListConfigTable(configDefinitions.jacg.listConfig, 'jacg.listConfig');
    html += '</div>';

    // jacg Set配置
    html += '<div id="tab-jacg-set" class="tab-content">';
    html += renderListConfigTable(configDefinitions.jacg.setConfig, 'jacg.setConfig');
    html += '</div>';

    // jacg EL配置
    html += '<div id="tab-jacg-el" class="tab-content">';
    html += renderListConfigTable(configDefinitions.jacg.elConfig, 'jacg.elConfig');
    html += '</div>';

    document.getElementById('configModalBody').innerHTML = html;
    document.getElementById('configModalTitle').textContent = '项目配置参数';
    document.getElementById('configModal').classList.remove('hidden');
    
    // 初始化依赖关系
    setTimeout(initDependencyRules, 100);
}