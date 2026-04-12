/**
 * Java All Call Graph Server 前端应用 - 模板管理模块
 * 包含：模板列表、模板详情、创建、编辑、删除、复制模板、入口类/方法相关
 */

/**
 * 加载模板列表
 */
async function loadTemplates(projectId) {
    try {
        const response = await fetch(`${API_BASE}/projects/${projectId}/templates`);
        const result = await response.json();
        if (result.code === 200) {
            renderTemplateList(result.data.templates);
        }
    } catch (error) {
        console.error('加载模板列表失败', error);
    }
}

/**
 * 渲染模板列表
 */
function renderTemplateList(templates) {
    const container = document.getElementById('templateList');
    if (!templates || templates.length === 0) {
        container.innerHTML = '<div class="text-muted">暂无模板，请新建模板</div>';
        return;
    }

    container.innerHTML = templates.map(template => `
        <div class="template-item" onclick="selectTemplate('${template.templateId}')">
            <div class="template-name">${template.description || template.templateId}</div>
            <span class="template-direction">${template.direction === 'caller' ? '向下' : '向上'}</span>
        </div>
    `).join('');
}

/**
 * 选择模板
 */
async function selectTemplate(templateId) {
    try {
        const response = await fetch(`${API_BASE}/templates/${templateId}`);
        const result = await response.json();
        if (result.code === 200) {
            currentTemplate = result.data;
            showTemplateDetail();
        }
    } catch (error) {
        console.error('加载模板详情失败', error);
        showToast('加载模板详情失败', 'error');
    }
}

/**
 * 显示模板详情
 */
async function showTemplateDetail() {
    document.getElementById('projectDetail').classList.add('hidden');
    document.getElementById('templateDetail').classList.remove('hidden');

    document.getElementById('templateTitle').textContent = currentTemplate.description || '模板详情';
    document.getElementById('detailTemplateId').textContent = currentTemplate.templateId;
    document.getElementById('detailTemplateDescription').textContent = currentTemplate.description || '-';
    document.getElementById('detailDirection').textContent = currentTemplate.direction === 'caller' ? '向下调用链' : '向上调用链';

    // 检查项目是否正在执行，如果是则禁用模板按钮
    await checkProjectExecutionAndDisableButtons();
}

/**
 * 检查项目是否正在执行，如果是则禁用模板按钮
 */
async function checkProjectExecutionAndDisableButtons() {
    if (!currentProject) return;
    
    try {
        const response = await fetch(`${API_BASE}/projects/${currentProject.projectId}/executing`);
        const result = await response.json();
        if (result.code === 200 && result.data.executing) {
            // 项目正在执行，禁用模板按钮
            disableTemplateButtons();
        }
    } catch (error) {
        console.error('检查项目执行状态失败', error);
    }
}

/**
 * 返回项目
 */
function backToProject() {
    document.getElementById('templateDetail').classList.add('hidden');
    document.getElementById('projectDetail').classList.remove('hidden');
}

/**
 * 显示创建模板模态框
 */
function showCreateTemplateModal() {
    document.getElementById('templateModalTitle').textContent = '新建模板';
    // 新建模板时不显示"编辑配置参数"按钮，创建后可通过编辑功能修改配置参数
    document.getElementById('templateModalHeaderActions').innerHTML = '';
    // 隐藏所有模板表单，显示新建模板表单
    hideAllTemplateModalForms();
    document.getElementById('createTemplateForm').classList.remove('hidden');
    // 清空表单
    document.getElementById('templateDescription').value = '';
    document.getElementById('templateDirection').value = 'caller';
    document.getElementById('entryPoints').value = '';
    document.getElementById('enableFindStack').checked = false;
    document.getElementById('findStackKeywords').value = '';
    document.getElementById('findStackKeywordsGroup').style.display = 'none';
    updateEntryPointLabel();
    document.getElementById('templateModalConfirm').onclick = createTemplate;
    document.getElementById('templateModal').classList.remove('hidden');
}

/**
 * 显示创建模板的配置编辑器
 */
function showTemplateConfigEditor() {
    if (!templateConfigDefinitions) {
        showToast('配置定义加载中，请稍后重试', 'warning');
        return;
    }

    // 获取模板配置定义（后端已过滤并设置只读属性）
    const templateConfig = getTemplateConfigDefinitions();

    // 检查是否有有效的配置项
    const hasMainConfig = templateConfig.mainConfig && templateConfig.mainConfig.length > 0;
    const hasDbConfig = templateConfig.dbConfig && templateConfig.dbConfig.length > 0;
    const hasListConfig = templateConfig.listConfig && templateConfig.listConfig.length > 0;
    const hasSetConfig = templateConfig.setConfig && templateConfig.setConfig.length > 0;
    const hasElConfig = templateConfig.elConfig && templateConfig.elConfig.length > 0;

    // 如果没有任何配置项，显示提示
    if (!hasMainConfig && !hasDbConfig && !hasListConfig && !hasSetConfig && !hasElConfig) {
        showToast('模板配置定义加载失败，请刷新页面重试', 'error');
        console.error('templateConfigDefinitions:', templateConfigDefinitions);
        return;
    }

    let html = '<div class="tabs">';
    let firstTab = true;
    
    if (hasMainConfig) {
        html += `<div class="tab ${firstTab ? 'active' : ''}" onclick="switchConfigTab(this, 'template-main')">主配置</div>`;
        firstTab = false;
    }
    if (hasDbConfig) {
        html += `<div class="tab ${firstTab ? 'active' : ''}" onclick="switchConfigTab(this, 'template-db')">数据库配置</div>`;
        firstTab = false;
    }
    if (hasListConfig) {
        html += `<div class="tab ${firstTab ? 'active' : ''}" onclick="switchConfigTab(this, 'template-list')">列表配置</div>`;
        firstTab = false;
    }
    if (hasSetConfig) {
        html += `<div class="tab ${firstTab ? 'active' : ''}" onclick="switchConfigTab(this, 'template-set')">Set配置</div>`;
        firstTab = false;
    }
    if (hasElConfig) {
        html += `<div class="tab ${firstTab ? 'active' : ''}" onclick="switchConfigTab(this, 'template-el')">EL配置</div>`;
        firstTab = false;
    }
    html += '</div>';

    // 渲染Tab内容
    // 数据库配置Tab使用只读模式，因为模板的数据库配置不允许人工编辑，自动使用项目的数据库配置
    firstTab = true;
    if (hasMainConfig) {
        html += `<div id="tab-template-main" class="tab-content ${firstTab ? 'active' : ''}">`;
        html += renderConfigTableWithValues(templateConfig.mainConfig, 'template.mainConfig', {}, false);
        html += '</div>';
        firstTab = false;
    }
    if (hasDbConfig) {
        html += `<div id="tab-template-db" class="tab-content ${firstTab ? 'active' : ''}">`;
        html += renderConfigTableWithValues(templateConfig.dbConfig, 'template.dbConfig', {}, true);
        html += '</div>';
        firstTab = false;
    }
    if (hasListConfig) {
        html += `<div id="tab-template-list" class="tab-content ${firstTab ? 'active' : ''}">`;
        html += renderListConfigTable(templateConfig.listConfig, 'template.listConfig');
        html += '</div>';
        firstTab = false;
    }
    if (hasSetConfig) {
        html += `<div id="tab-template-set" class="tab-content ${firstTab ? 'active' : ''}">`;
        html += renderListConfigTable(templateConfig.setConfig, 'template.setConfig');
        html += '</div>';
        firstTab = false;
    }
    if (hasElConfig) {
        html += `<div id="tab-template-el" class="tab-content ${firstTab ? 'active' : ''}">`;
        html += renderListConfigTable(templateConfig.elConfig, 'template.elConfig');
        html += '</div>';
        firstTab = false;
    }

    document.getElementById('configModalBody').innerHTML = html;
    document.getElementById('configModalTitle').textContent = '模板配置参数';
    document.getElementById('configModal').classList.remove('hidden');
    
    // 初始化依赖关系
    setTimeout(initDependencyRules, 100);
}

/**
 * 获取模板配置定义（直接使用后端返回的配置定义）
 * 后端已根据模板场景过滤了不需要展示的配置项，并设置了只读属性
 */
function getTemplateConfigDefinitions() {
    if (!templateConfigDefinitions || !templateConfigDefinitions.jacg) {
        return {
            mainConfig: [],
            dbConfig: [],
            listConfig: [],
            setConfig: [],
            elConfig: []
        };
    }

    // 处理主配置：对于只读配置项（如CKE_APP_NAME），使用项目中的值
    const mainConfig = (templateConfigDefinitions.jacg.mainConfig || []).map(config => {
        // 如果是不可编辑的配置项，使用项目中的值作为当前值
        if (!config.editable && currentProject && currentProject.jacgConfig && currentProject.jacgConfig.mainConfig) {
            const projectValue = currentProject.jacgConfig.mainConfig[config.key];
            if (projectValue !== undefined) {
                // 创建副本并设置当前值
                return Object.assign({}, config, { currentValue: projectValue });
            }
        }
        return config;
    });

    return {
        mainConfig: mainConfig,
        dbConfig: templateConfigDefinitions.jacg.dbConfig || [],
        listConfig: templateConfigDefinitions.jacg.listConfig || [],
        setConfig: templateConfigDefinitions.jacg.setConfig || [],
        elConfig: templateConfigDefinitions.jacg.elConfig || []
    };
}

/**
 * 创建模板
 */
async function createTemplate() {
    const description = document.getElementById('templateDescription').value;
    const direction = document.getElementById('templateDirection').value;
    const entryPointsText = document.getElementById('entryPoints').value;
    const entryPoints = entryPointsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集关键字配置
    const enableFindStack = document.getElementById('enableFindStack')?.checked || false;
    const findStackKeywordsText = document.getElementById('findStackKeywords')?.value || '';
    const findStackKeywords = findStackKeywordsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集配置数据（如果已打开配置编辑器）
    const templateConfig = collectTemplateConfig();

    // 构建listConfig，包含关键字配置
    const listConfig = Object.assign(
        {},
        templateConfig ? templateConfig.listConfig : {}
    );
    if (enableFindStack && findStackKeywords.length > 0) {
        if (direction === 'caller') {
            listConfig['OCFULE_FIND_STACK_KEYWORD_4ER'] = findStackKeywords;
        } else {
            listConfig['OCFULE_FIND_STACK_KEYWORD_4EE'] = findStackKeywords;
        }
    }

    const templateData = {
        description: description,
        direction: direction,
        jacgConfig: {
            mainConfig: templateConfig ? templateConfig.mainConfig : {},
            dbConfig: templateConfig ? templateConfig.dbConfig : {},
            listConfig: listConfig,
            setConfig: Object.assign(
                {},
                templateConfig ? templateConfig.setConfig : {},
                direction === 'caller' 
                    ? { 'OCFUSE_METHOD_CLASS_4CALLER': entryPoints }
                    : { 'OCFUSE_METHOD_CLASS_4CALLEE': entryPoints }
            ),
            elConfig: templateConfig ? templateConfig.elConfig : {}
        }
    };

    try {
        const response = await fetch(`${API_BASE}/projects/${currentProject.projectId}/templates`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(templateData)
        });
        const result = await response.json();
        if (result.code === 200) {
            closeTemplateModal();
            closeConfigModal();
            loadTemplates(currentProject.projectId);
            showToast('模板创建成功', 'success');
        } else {
            if (!handleApiError(result)) {
                showToast('创建失败: ' + result.message, 'error');
            }
        }
    } catch (error) {
        console.error('创建模板失败', error);
        showToast('创建模板失败', 'error');
    }
}

/**
 * 编辑模板
 */
function editTemplate() {
    // 获取当前模板的入口类/方法配置
    const setConfig = currentTemplate.jacgConfig && currentTemplate.jacgConfig.setConfig 
        ? currentTemplate.jacgConfig.setConfig 
        : {};
    const listConfig = currentTemplate.jacgConfig && currentTemplate.jacgConfig.listConfig 
        ? currentTemplate.jacgConfig.listConfig 
        : {};
    const entryPoints = currentTemplate.direction === 'caller' 
        ? (setConfig['OCFUSE_METHOD_CLASS_4CALLER'] || [])
        : (setConfig['OCFUSE_METHOD_CLASS_4CALLEE'] || []);
    const entryPointsText = entryPoints.join('\n');
    
    // 获取关键字配置
    const findStackKeywords = currentTemplate.direction === 'caller' 
        ? (listConfig['OCFULE_FIND_STACK_KEYWORD_4ER'] || [])
        : (listConfig['OCFULE_FIND_STACK_KEYWORD_4EE'] || []);
    const findStackKeywordsText = findStackKeywords.join('\n');
    const enableFindStack = findStackKeywords.length > 0;
    
    document.getElementById('templateModalTitle').textContent = '编辑模板';
    document.getElementById('templateModalHeaderActions').innerHTML = `<button class="btn btn-secondary btn-sm" onclick="showEditTemplateConfigEditor()">编辑配置参数</button>`;
    // 隐藏所有表单，显示编辑模板表单
    hideAllTemplateModalForms();
    document.getElementById('editTemplateForm').classList.remove('hidden');
    // 填充数据
    document.getElementById('editTemplateDescription').value = currentTemplate.description || '';
    document.getElementById('editTemplateDirection').value = currentTemplate.direction || 'caller';
    document.getElementById('editEntryPoints').value = entryPointsText;
    document.getElementById('editEnableFindStack').checked = enableFindStack;
    document.getElementById('editFindStackKeywords').value = findStackKeywordsText;
    document.getElementById('editFindStackKeywordsGroup').style.display = enableFindStack ? 'block' : 'none';
    updateEditEntryPointLabel();
    document.getElementById('templateModalConfirm').onclick = updateTemplate;
    document.getElementById('templateModal').classList.remove('hidden');
}

/**
 * 关闭模板编辑模态框
 */
function closeTemplateModal() {
    document.getElementById('templateModal').classList.add('hidden');
    document.getElementById('templateModalHeaderActions').innerHTML = '';
    hideAllTemplateModalForms();
}

/**
 * 确认模板模态框
 */
function confirmTemplateModal() {
    updateTemplate();
}

/**
 * 显示编辑模板的配置编辑器
 */
function showEditTemplateConfigEditor() {
    if (!configDefinitions) {
        showToast('配置定义加载中，请稍后重试', 'warning');
        return;
    }

    // 获取模板配置定义
    const templateConfig = getTemplateConfigDefinitions();
    const currentJacgConfig = currentTemplate.jacgConfig || {};

    // 获取数据库配置提示模板
    const dbConfigTipTemplate = document.getElementById('templateDbConfigTipTemplate');
    const dbConfigTipHtml = dbConfigTipTemplate ? dbConfigTipTemplate.innerHTML : '';

    // 定义所有配置Tab（按顺序）
    // 数据库配置Tab使用只读模式，因为模板的数据库配置不允许人工编辑
    // 数据库配置Tab添加提示信息
    const tabConfigs = [
        { id: 'template-main', name: '主配置', render: () => renderConfigTableWithValues(templateConfig.mainConfig, 'template.mainConfig', currentJacgConfig.mainConfig || {}, false) },
        { id: 'template-db', name: '数据库配置', render: () => dbConfigTipHtml + renderConfigTableWithValues(templateConfig.dbConfig, 'template.dbConfig', currentJacgConfig.dbConfig || {}, true) },
        { id: 'template-list', name: '列表配置', render: () => renderListConfigTableWithValues(templateConfig.listConfig, 'template.listConfig', currentJacgConfig.listConfig || {}) },
        { id: 'template-set', name: 'Set配置', render: () => renderListConfigTableWithValues(templateConfig.setConfig, 'template.setConfig', currentJacgConfig.setConfig || {}) },
        { id: 'template-el', name: 'EL配置', render: () => renderListConfigTableWithValues(templateConfig.elConfig, 'template.elConfig', currentJacgConfig.elConfig || {}) }
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

    document.getElementById('templateConfigModalBody').innerHTML = html;
    document.getElementById('templateConfigModalTitle').textContent = '编辑模板配置参数';
    document.getElementById('templateConfigModal').classList.remove('hidden');
    
    // 初始化依赖关系
    setTimeout(initDependencyRules, 100);
}

/**
 * 关闭模板配置模态框
 */
function closeTemplateConfigModal() {
    document.getElementById('templateConfigModal').classList.add('hidden');
}

/**
 * 应用模板配置模态框
 */
function applyTemplateConfigModal() {
    // 调用应用模板函数保存配置
    applyTemplate();
}

/**
 * 确认模板配置模态框（保存配置并关闭窗口）
 */
async function confirmTemplateConfigModal() {
    // 先保存配置
    await applyTemplate();
    // 然后关闭配置模态框
    closeTemplateConfigModal();
}

/**
 * 更新模板
 */
async function updateTemplate() {
    const description = document.getElementById('editTemplateDescription').value;
    const direction = document.getElementById('editTemplateDirection').value;
    const entryPointsText = document.getElementById('editEntryPoints') ? document.getElementById('editEntryPoints').value : '';
    const entryPoints = entryPointsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集关键字配置
    const enableFindStack = document.getElementById('editEnableFindStack')?.checked || false;
    const findStackKeywordsText = document.getElementById('editFindStackKeywords')?.value || '';
    const findStackKeywords = findStackKeywordsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集配置数据（如果已打开配置编辑器）
    const templateConfig = collectTemplateConfig();

    // 合并入口类/方法配置
    const finalSetConfig = Object.assign(
        {},
        templateConfig ? templateConfig.setConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.setConfig : {}),
        direction === 'caller' 
            ? { 'OCFUSE_METHOD_CLASS_4CALLER': entryPoints }
            : { 'OCFUSE_METHOD_CLASS_4CALLEE': entryPoints }
    );

    // 构建listConfig，包含关键字配置
    const finalListConfig = Object.assign(
        {},
        templateConfig ? templateConfig.listConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.listConfig : {})
    );
    if (enableFindStack && findStackKeywords.length > 0) {
        if (direction === 'caller') {
            finalListConfig['OCFULE_FIND_STACK_KEYWORD_4ER'] = findStackKeywords;
        } else {
            finalListConfig['OCFULE_FIND_STACK_KEYWORD_4EE'] = findStackKeywords;
        }
    }

    try {
        const response = await fetch(`${API_BASE}/templates/${currentTemplate.templateId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                description: description,
                direction: direction,
                jacgConfig: {
                    mainConfig: templateConfig ? templateConfig.mainConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.mainConfig : {}),
                    dbConfig: templateConfig ? templateConfig.dbConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.dbConfig : {}),
                    listConfig: finalListConfig,
                    setConfig: finalSetConfig,
                    elConfig: templateConfig ? templateConfig.elConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.elConfig : {})
                }
            })
        });
        const result = await response.json();
        if (result.code === 200) {
            closeTemplateModal();
            closeTemplateConfigModal();
            // 重新加载模板详情以更新 currentTemplate 对象
            await selectTemplate(currentTemplate.templateId);
            // 同时刷新模板列表，更新列表中的方向显示
            loadTemplates(currentProject.projectId);
            showToast('更新成功', 'success');
        } else {
            if (!handleApiError(result)) {
                showToast('更新失败: ' + result.message, 'error');
            }
        }
    } catch (error) {
        console.error('更新模板失败', error);
        showToast('更新模板失败', 'error');
    }
}

/**
 * 应用模板（不关闭窗口但保存配置）
 */
async function applyTemplate() {
    const description = document.getElementById('editTemplateDescription').value;
    const direction = document.getElementById('editTemplateDirection').value;
    const entryPointsText = document.getElementById('editEntryPoints') ? document.getElementById('editEntryPoints').value : '';
    const entryPoints = entryPointsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集关键字配置
    const enableFindStack = document.getElementById('editEnableFindStack')?.checked || false;
    const findStackKeywordsText = document.getElementById('editFindStackKeywords')?.value || '';
    const findStackKeywords = findStackKeywordsText.split('\n').map(p => p.trim()).filter(p => p);

    // 收集配置数据（如果已打开配置编辑器）
    const templateConfig = collectTemplateConfig();

    // 合并入口类/方法配置
    const finalSetConfig = Object.assign(
        {},
        templateConfig ? templateConfig.setConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.setConfig : {}),
        direction === 'caller' 
            ? { 'OCFUSE_METHOD_CLASS_4CALLER': entryPoints }
            : { 'OCFUSE_METHOD_CLASS_4CALLEE': entryPoints }
    );

    // 构建listConfig，包含关键字配置
    const finalListConfig = Object.assign(
        {},
        templateConfig ? templateConfig.listConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.listConfig : {})
    );
    if (enableFindStack && findStackKeywords.length > 0) {
        if (direction === 'caller') {
            finalListConfig['OCFULE_FIND_STACK_KEYWORD_4ER'] = findStackKeywords;
        } else {
            finalListConfig['OCFULE_FIND_STACK_KEYWORD_4EE'] = findStackKeywords;
        }
    }

    try {
        const response = await fetch(`${API_BASE}/templates/${currentTemplate.templateId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                description: description,
                direction: direction,
                jacgConfig: {
                    mainConfig: templateConfig ? templateConfig.mainConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.mainConfig : {}),
                    dbConfig: templateConfig ? templateConfig.dbConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.dbConfig : {}),
                    listConfig: finalListConfig,
                    setConfig: finalSetConfig,
          elConfig: templateConfig ? templateConfig.elConfig : (currentTemplate.jacgConfig ? currentTemplate.jacgConfig.elConfig : {})
                }
            })
        });
        const result = await response.json();
        if (result.code === 200) {
            // 不关闭窗口，只刷新数据
            loadTemplates(currentProject.projectId);
            // 更新当前模板数据
            const templateResponse = await fetch(`${API_BASE}/templates/${currentTemplate.templateId}`);
            const templateResult = await templateResponse.json();
            if (templateResult.code === 200) {
                currentTemplate = templateResult.data;
            }
            showToast('已应用', 'success');
        } else {
            if (!handleApiError(result)) {
                showToast('应用失败: ' + result.message, 'error');
            }
        }
    } catch (error) {
        console.error('应用模板失败', error);
        showToast('应用模板失败', 'error');
    }
}

/**
 * 删除模板
 */
async function deleteTemplate() {
    if (!confirm('确定要删除此模板吗？此操作不可恢复。')) {
        return;
    }

    try {
        const response = await fetch(`${API_BASE}/templates/${currentTemplate.templateId}`, {
            method: 'DELETE'
        });
        const result = await response.json();
        if (result.code === 200) {
            currentTemplate = null;
            document.getElementById('templateDetail').classList.add('hidden');
            document.getElementById('projectDetail').classList.remove('hidden');
            loadTemplates(currentProject.projectId);
            showToast('删除成功', 'success');
        } else {
            showToast('删除失败: ' + result.message, 'error');
        }
    } catch (error) {
        console.error('删除模板失败', error);
        showToast('删除模板失败', 'error');
    }
}

/**
 * 复制模板
 */
function copyTemplate() {
    document.getElementById('modalTitle').textContent = '复制模板';
    document.getElementById('modalHeaderActions').innerHTML = '';
    // 隐藏所有表单，显示复制模板表单
    hideAllModalForms();
    document.getElementById('copyTemplateForm').classList.remove('hidden');
    // 填充数据
    document.getElementById('copyTemplateDescription').value = (currentTemplate.description || '') + '-副本';
    document.getElementById('modalConfirm').onclick = doCopyTemplate;
    document.getElementById('modal').classList.remove('hidden');
}

/**
 * 执行复制模板
 */
async function doCopyTemplate() {
    const description = document.getElementById('copyTemplateDescription').value;

    try {
        const response = await fetch(`${API_BASE}/templates/${currentTemplate.templateId}/copy`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ description: description })
        });
        const result = await response.json();
        if (result.code === 200) {
            closeModal();
            loadTemplates(currentProject.projectId);
            showToast('复制成功', 'success');
        } else {
            showToast('复制失败: ' + result.message, 'error');
        }
    } catch (error) {
        console.error('复制模板失败', error);
        showToast('复制模板失败', 'error');
    }
}

/**
 * 禁用模板详情页的操作按钮
 * 禁用：生成调用链、生成调用链根据关键字生成堆栈、编辑、删除、复制
 * 不禁用：查询执行记录、返回项目
 */
function disableTemplateButtons() {
    const buttons = document.querySelectorAll('#templateDetail .panel-actions button');
    buttons.forEach(btn => {
        const onclick = btn.getAttribute('onclick');
        // 排除"返回项目"和"查询执行记录"按钮，禁用其他所有按钮
        if (onclick && onclick.indexOf('backToProject') === -1 && onclick.indexOf('showTemplateExecutionRecordModal') === -1) {
            btn.disabled = true;
        }
    });
}

/**
 * 启用模板详情页的所有操作按钮
 */
function enableTemplateButtons() {
    // 启用所有按钮
    const buttons = document.querySelectorAll('#templateDetail .panel-actions button');
    buttons.forEach(btn => {
        btn.disabled = false;
    });
}

/**
 * 执行调用链生成
 */
async function executeCallGraph() {
    // 禁用所有操作按钮
    disableTemplateButtons();
    
    // 显示执行中动态效果
    const executeBtn = document.querySelector('#templateDetail .panel-actions .btn-success');
    if (executeBtn) {
        executeBtn.innerHTML = '<span class="executing-spinner"></span> 执行中...';
    }

    try {
        const response = await fetch(`${API_BASE}/templates/${currentTemplate.templateId}/execute/callgraph`, {
            method: 'POST'
        });
        const result = await response.json();
        if (result.code === 200) {
            showToast('调用链生成已开始执行', 'info');
            // 开始轮询执行状态
            pollTemplateExecutionStatus(currentTemplate.templateId, executeBtn, '生成调用链');
        } else {
            showToast('执行失败: ' + result.message, 'error');
            resetExecuteButton(executeBtn, '生成调用链');
            enableTemplateButtons();
        }
    } catch (error) {
        console.error('执行调用链生成失败', error);
        showToast('执行调用链生成失败', 'error');
        resetExecuteButton(executeBtn, '生成调用链');
        enableTemplateButtons();
    }
}

/**
 * 执行生成调用链根据关键字生成堆栈
 */
async function executeFindStack() {
    // 禁用所有操作按钮
    disableTemplateButtons();
    
    // 显示执行中动态效果
    const executeBtn = document.querySelector('#templateDetail .panel-actions .btn-warning');
    if (executeBtn) {
        executeBtn.innerHTML = '<span class="executing-spinner"></span> 执行中...';
    }

    try {
        const response = await fetch(`${API_BASE}/templates/${currentTemplate.templateId}/execute/findstack`, {
            method: 'POST'
        });
        const result = await response.json();
        if (result.code === 200) {
            showToast('生成调用链根据关键字生成堆栈已开始执行', 'info');
            // 开始轮询执行状态
            pollTemplateExecutionStatus(currentTemplate.templateId, executeBtn, '生成调用链根据<br>关键字生成堆栈');
        } else {
            showToast('执行失败: ' + result.message, 'error');
            resetExecuteButton(executeBtn, '生成调用链根据<br>关键字生成堆栈');
            enableTemplateButtons();
        }
    } catch (error) {
        console.error('执行生成调用链根据关键字生成堆栈失败', error);
        showToast('执行生成调用链根据关键字生成堆栈失败', 'error');
        resetExecuteButton(executeBtn, '生成调用链根据<br>关键字生成堆栈');
        enableTemplateButtons();
    }
}

/**
 * 轮询模板执行状态
 * @param {string} templateId 模板ID
 * @param {HTMLElement} executeBtn 执行按钮元素
 * @param {string} buttonText 按钮文本（用于重置），可包含HTML
 */
async function pollTemplateExecutionStatus(templateId, executeBtn, buttonText = '生成调用链') {
    try {
        // 同时检查模板和项目的执行状态
        const [templateResponse, projectResponse] = await Promise.all([
            fetch(`${API_BASE}/templates/${templateId}/executing`),
            fetch(`${API_BASE}/projects/${currentProject.projectId}/executing`)
        ]);
        const templateResult = await templateResponse.json();
        const projectResult = await projectResponse.json();

        const executing = templateResult.code === 200 && templateResult.data.executing;
        const projectExecuting = projectResult.code === 200 && projectResult.data.executing;
        const duration = templateResult.data?.duration;
        const status = templateResult.data?.status;

        if (executing) {
            // 模板正在执行，更新按钮显示当前耗时
            if (executeBtn && duration) {
                executeBtn.innerHTML = `<span class="executing-spinner"></span> 执行中 ${formatDuration(duration)}`;
            }
            // 继续轮询
            setTimeout(() => pollTemplateExecutionStatus(templateId, executeBtn, buttonText), 1000);
        } else if (projectExecuting) {
            // 项目正在执行，继续轮询并保持按钮禁用
            setTimeout(() => pollTemplateExecutionStatus(templateId, executeBtn, buttonText), 1000);
        } else {
            // 都不在执行，启用所有按钮
            resetExecuteButton(executeBtn, buttonText);
            enableTemplateButtons();
            // 显示执行耗时
            const durationText = duration ? `，耗时 ${formatDuration(duration)}` : '';
            const statusText = status === 'completed' ? '执行完成' : '执行失败';
            // 去掉HTML标签用于显示
            const plainText = buttonText.replace(/<br>/g, '');
            showToast(`${plainText}${statusText}${durationText}`, status === 'completed' ? 'success' : 'error');
        }
    } catch (error) {
        console.error('查询执行状态失败', error);
        // 继续轮询
        setTimeout(() => pollTemplateExecutionStatus(templateId, executeBtn, buttonText), 1000);
    }
}

// ============================================================
// 入口类/方法和关键字相关
// ============================================================

/**
 * 显示入口类/方法参数描述
 */
function showEntryPointDescription() {
    // 根据当前显示的表单获取正确的方向值
    const editForm = document.getElementById('editTemplateForm');
    const createForm = document.getElementById('createTemplateForm');
    let direction;
    if (editForm && !editForm.classList.contains('hidden')) {
        direction = document.getElementById('editTemplateDirection')?.value || 'caller';
    } else if (createForm && !createForm.classList.contains('hidden')) {
        direction = document.getElementById('templateDirection')?.value || 'caller';
    } else {
        direction = 'caller';
    }
    // 调用后端API，根据方向获取配置描述
    showConfigDescriptionByDirection('entry-point', direction);
}

/**
 * 显示关键字参数描述
 */
function showFindStackKeywordsDescription() {
    // 根据当前显示的表单获取正确的方向值
    const editForm = document.getElementById('editTemplateForm');
    const createForm = document.getElementById('createTemplateForm');
    let direction;
    if (editForm && !editForm.classList.contains('hidden')) {
        direction = document.getElementById('editTemplateDirection')?.value || 'caller';
    } else if (createForm && !createForm.classList.contains('hidden')) {
        direction = document.getElementById('templateDirection')?.value || 'caller';
    } else {
        direction = 'caller';
    }
    // 调用后端API，根据方向获取配置描述
    showConfigDescriptionByDirection('find-stack-keyword', direction);
}

/**
 * 切换关键字输入框显示（新建模板）
 */
function toggleFindStackKeywords() {
    const checkbox = document.getElementById('enableFindStack');
    const group = document.getElementById('findStackKeywordsGroup');
    if (checkbox && group) {
        group.style.display = checkbox.checked ? 'block' : 'none';
        updateFindStackKeywordsLabel();
    }
}

/**
 * 切换关键字输入框显示（编辑模板）
 */
function toggleEditFindStackKeywords() {
    const checkbox = document.getElementById('editEnableFindStack');
    const group = document.getElementById('editFindStackKeywordsGroup');
    if (checkbox && group) {
        group.style.display = checkbox.checked ? 'block' : 'none';
        updateEditFindStackKeywordsLabel();
    }
}

/**
 * 更新入口类/方法标签（新建模板）
 */
function updateEntryPointLabel() {
    updateFindStackKeywordsLabel();
}

/**
 * 更新入口类/方法标签（编辑模板）
 */
function updateEditEntryPointLabel() {
    updateEditFindStackKeywordsLabel();
}

/**
 * 更新关键字标签（新建模板）
 */
function updateFindStackKeywordsLabel() {
    const direction = document.getElementById('templateDirection')?.value || 'caller';
    const label = document.getElementById('findStackKeywordsLabel');
    if (label) {
        label.textContent = direction === 'caller' ? '关键字（向下调用链）' : '关键字（向上调用链）';
    }
}

/**
 * 更新关键字标签（编辑模板）
 */
function updateEditFindStackKeywordsLabel() {
    const direction = document.getElementById('editTemplateDirection')?.value || 'caller';
    const label = document.getElementById('editFindStackKeywordsLabel');
    if (label) {
        label.textContent = direction === 'caller' ? '关键字（向下调用链）' : '关键字（向上调用链）';
    }
}