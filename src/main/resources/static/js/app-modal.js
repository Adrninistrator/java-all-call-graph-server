/**
 * Java All Call Graph Server 前端应用 - 模态框和执行记录模块
 * 包含：模态框操作、执行状态显示、执行记录查询
 */

// 当前执行ID（用于刷新状态按钮）
let currentExecId = null;

// ============================================================
// 模态框操作
// ============================================================

/**
 * 关闭模态框
 */
function closeModal() {
    document.getElementById('modal').classList.add('hidden');
    document.getElementById('modalHeaderActions').innerHTML = '';
    hideAllModalForms();
}

/**
 * 隐藏所有项目模态框表单
 */
function hideAllModalForms() {
    const forms = document.querySelectorAll('#modalBody .modal-form');
    forms.forEach(form => form.classList.add('hidden'));
}

/**
 * 隐藏所有模板模态框表单
 */
function hideAllTemplateModalForms() {
    const forms = document.querySelectorAll('#templateModalBody .modal-form');
    forms.forEach(form => form.classList.add('hidden'));
}

/**
 * 确认模态框
 */
function confirmModal() {
    // 由具体操作设置
}

/**
 * 关闭配置模态框
 */
function closeConfigModal() {
    document.getElementById('configModal').classList.add('hidden');
}

/**
 * 确认配置模态框（保存配置并关闭窗口）
 */
async function confirmConfigModal() {
    // 先保存配置
    await applyConfigModal();
    // 然后关闭配置模态框
    closeConfigModal();
}

/**
 * 应用配置模态框（不关闭窗口但暂存配置）
 */
async function applyConfigModal() {
    const description = document.getElementById('editProjectDescription').value;
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
            // 不关闭窗口，只刷新数据
            selectProject(currentProject.projectId);
            showToast('已应用', 'success');
        } else {
            if (!handleApiError(result)) {
                showToast('应用失败: ' + result.message, 'error');
            }
        }
    } catch (error) {
        console.error('应用项目失败', error);
        showToast('应用项目失败', 'error');
    }
}

/**
 * 关闭执行模态框
 */
function closeExecModal() {
    document.getElementById('execModal').classList.add('hidden');
}

/**
 * 显示执行状态
 */
function showExecutionStatus(execId, type) {
    currentExecId = execId;
    document.getElementById('execId').textContent = execId;
    document.getElementById('execStatus').textContent = '状态: 检查中...';
    document.getElementById('execModal').classList.remove('hidden');
    pollExecutionStatus(execId);
}

/**
 * 轮询执行状态
 */
async function pollExecutionStatus(execId) {
    try {
        const response = await fetch(`${API_BASE}/executions/${execId}/status`);
        const result = await response.json();
        if (result.code === 200) {
            const status = result.data.status;
            const statusText = getStatusText(status);
            document.getElementById('execStatus').textContent = `状态: ${statusText}`;
            
            if (status === 'running') {
                setTimeout(() => pollExecutionStatus(execId), 2000);
            } else {
                if (status === 'completed') {
                    showToast('执行完成', 'success');
                } else {
                    showToast('执行失败: ' + (result.data.errorMessage || '未知错误'), 'error');
                }
                closeExecModal();
                if (currentProject) {
                    loadExecutionHistory(currentProject.projectId);
                }
            }
        }
    } catch (error) {
        console.error('查询执行状态失败', error);
    }
}

// ============================================================
// 执行记录查询相关功能
// ============================================================

/**
 * 显示执行记录查询模态框
 */
function showExecutionRecordModal() {
    if (!currentProject) {
        showToast('请先选择项目', 'warning');
        return;
    }
    
    // 重置查询条件
    document.getElementById('queryMinStartTime').value = '';
    document.getElementById('queryPageSize').value = '10';
    
    // 清空记录列表（显示加载中）
    document.getElementById('executionRecordList').innerHTML = '<div class="text-muted">加载中...</div>';
    document.getElementById('executionRecordPagination').innerHTML = '';
    
    // 显示模态框
    document.getElementById('executionRecordModal').classList.remove('hidden');
    
    // 使用默认条件查询一次
    queryExecutionRecords(1);
}

/**
 * 关闭执行记录查询模态框
 */
function closeExecutionRecordModal() {
    document.getElementById('executionRecordModal').classList.add('hidden');
}

/**
 * 查询执行记录
 */
async function queryExecutionRecords(pageNum = 1) {
    if (!currentProject) {
        showToast('请先选择项目', 'warning');
        return;
    }
    
    // 获取查询参数
    const minStartTime = document.getElementById('queryMinStartTime').value;
    const pageSize = parseInt(document.getElementById('queryPageSize').value);
    
    // 构建查询URL
    let url = `${API_BASE}/execution-records/project/${currentProject.projectId}?pageNum=${pageNum}&pageSize=${pageSize}`;
    if (minStartTime) {
        // 转换为 yyyy-MM-dd HH:mm:ss 格式
        const date = new Date(minStartTime);
        const formatted = date.getFullYear() + '-' +
            String(date.getMonth() + 1).padStart(2, '0') + '-' +
            String(date.getDate()).padStart(2, '0') + ' ' +
            String(date.getHours()).padStart(2, '0') + ':' +
            String(date.getMinutes()).padStart(2, '0') + ':' +
            String(date.getSeconds()).padStart(2, '0');
        url += `&minStartTime=${encodeURIComponent(formatted)}`;
    }
    
    try {
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 200) {
            renderExecutionRecordList(result.data);
        } else {
            showToast('查询失败: ' + result.message, 'error');
        }
    } catch (error) {
        console.error('查询执行记录失败', error);
        showToast('查询执行记录失败', 'error');
    }
}

/**
 * 渲染执行记录列表
 */
function renderExecutionRecordList(data) {
    const container = document.getElementById('executionRecordList');
    
    if (!data.records || data.records.length === 0) {
        container.innerHTML = '<div class="text-muted">暂无执行记录</div>';
        document.getElementById('executionRecordPagination').innerHTML = '';
        return;
    }
    
    // 渲染记录列表
    let html = '<table class="record-table">';
    html += '<thead><tr>';
    html += '<th>执行ID</th>';
    html += '<th>项目描述</th>';
    html += '<th>Jar文件</th>';
    html += '<th>开始时间</th>';
    html += '<th>结束时间</th>';
    html += '<th>耗时</th>';
    html += '<th>状态</th>';
    html += '<th>错误信息</th>';
    html += '<th>操作</th>';
    html += '</tr></thead>';
    html += '<tbody>';
    
    data.records.forEach(record => {
        const jarFilesShort = record.jarFiles ? 
            (record.jarFiles.length > 50 ? record.jarFiles.substring(0, 50) + '...' : record.jarFiles) 
            : '-';
        const jarFilesFull = record.jarFiles || '-';
        const durationText = record.duration ? formatDuration(record.duration) : '-';
        
        html += '<tr>';
        html += `<td>${escapeHtml(record.execId)}</td>`;
        html += `<td>${escapeHtml(record.projectDesc || '-')}</td>`;
        html += `<td title="${escapeHtml(jarFilesFull)}">${escapeHtml(jarFilesShort.replace(/\n/g, '<br>'))}</td>`;
        html += `<td>${formatDateTime(record.startTime)}</td>`;
        html += `<td>${record.endTime ? formatDateTime(record.endTime) : '-'}</td>`;
        html += `<td>${durationText}</td>`;
        html += `<td><span class="exec-status status-${record.status}">${getStatusText(record.status)}</span></td>`;
        html += `<td>${record.errorMessage ? escapeHtml(record.errorMessage.substring(0, 50) + '...') : '-'}</td>`;
        html += `<td><button class="btn btn-sm btn-info" onclick="showExecutionRecordDetail(${record.id})">详情</button></td>`;
        html += '</tr>';
    });
    
    html += '</tbody></table>';
    container.innerHTML = html;
    
    // 渲染分页
    renderExecutionRecordPagination(data);
}

/**
 * 渲染执行记录分页
 */
function renderExecutionRecordPagination(data) {
    const container = document.getElementById('executionRecordPagination');
    
    if (data.totalPages <= 1) {
        container.innerHTML = `<div class="pagination-info">共 ${data.total} 条记录</div>`;
        return;
    }
    
    let html = '<div class="pagination-controls">';
    html += `<span class="pagination-info">共 ${data.total} 条记录，第 ${data.pageNum}/${data.totalPages} 页</span>`;
    
    // 上一页
    if (data.pageNum > 1) {
        html += `<button class="btn btn-sm" onclick="queryExecutionRecords(${data.pageNum - 1})">上一页</button>`;
    }
    
    // 页码
    const startPage = Math.max(1, data.pageNum - 2);
    const endPage = Math.min(data.totalPages, data.pageNum + 2);
    
    for (let i = startPage; i <= endPage; i++) {
        if (i === data.pageNum) {
            html += `<button class="btn btn-sm btn-primary">${i}</button>`;
        } else {
            html += `<button class="btn btn-sm" onclick="queryExecutionRecords(${i})">${i}</button>`;
        }
    }
    
    // 下一页
    if (data.pageNum < data.totalPages) {
        html += `<button class="btn btn-sm" onclick="queryExecutionRecords(${data.pageNum + 1})">下一页</button>`;
    }
    
    html += '</div>';
    container.innerHTML = html;
}

/**
 * 显示执行记录详情
 */
async function showExecutionRecordDetail(id) {
    // 创建详情模态框
    let modalHtml = `
        <div class="modal" id="recordDetailModal" onclick="closeRecordDetailModal(event)">
            <div class="modal-content" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h3>执行记录详情</h3>
                    <button class="modal-close" onclick="closeRecordDetailModal()">&times;</button>
                </div>
                <div id="recordDetailBody" class="modal-body">
                    <div class="text-center">加载中...</div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary" onclick="closeRecordDetailModal()">关闭</button>
                </div>
            </div>
        </div>
    `;
    
    // 移除已存在的模态框
    const existingModal = document.getElementById('recordDetailModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // 添加模态框到body
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
 try {
        const response = await fetch(`${API_BASE}/execution-records/${id}`);
        const result = await response.json();
        
        if (result.code === 200) {
            const record = result.data;
            // 格式化Jar文件列表（每行显示一个）
            const jarFilesHtml = record.jarFiles 
                ? record.jarFiles.split('\n').map(f => `<div>${escapeHtml(f)}</div>`).join('') 
                : '-';
            
            // 渲染详情表格
            const durationText = record.duration ? formatDuration(record.duration) : '-';
            let detailHtml = `
                <table class="detail-table">
                    <tr><th>执行ID</th><td>${escapeHtml(record.execId)}</td></tr>
                    <tr><th>项目ID</th><td>${escapeHtml(record.projectId)}</td></tr>
                    <tr><th>项目描述</th><td>${escapeHtml(record.projectDesc || '-')}</td></tr>
                    <tr><th>Jar文件列表</th><td class="jar-files-list">${jarFilesHtml}</td></tr>
                    <tr><th>开始时间</th><td>${formatDateTime(record.startTime)}</td></tr>
                    <tr><th>结束时间</th><td>${record.endTime ? formatDateTime(record.endTime) : '-'}</td></tr>
                    <tr><th>执行耗时</th><td>${durationText}</td></tr>
                    <tr><th>执行状态</th><td><span class="exec-status status-${record.status}">${getStatusText(record.status)}</span></td></tr>
                    <tr><th>错误信息</th><td>${record.errorMessage ? escapeHtml(record.errorMessage) : '-'}</td></tr>
                    <tr><th>日志文件路径</th><td>${record.logFilePath ? escapeHtml(record.logFilePath) : '-'}</td></tr>
                    <tr><th>创建时间</th><td>${formatDateTime(record.createTime)}</td></tr>
                    <tr><th>更新时间</th><td>${record.updateTime ? formatDateTime(record.updateTime) : '-'}</td></tr>
                </table>
            `;
            document.getElementById('recordDetailBody').innerHTML = detailHtml;
        } else {
            document.getElementById('recordDetailBody').innerHTML = `<div class="text-muted">加载失败: ${result.message}</div>`;
        }
    } catch (error) {
        console.error('获取执行记录详情失败', error);
        document.getElementById('recordDetailBody').innerHTML = '<div class="text-muted">加载失败</div>';
    }
}

/**
 * 关闭执行记录详情模态框
 */
function closeRecordDetailModal(event) {
    if (event && event.target !== event.currentTarget) {
        return;
    }
    const modal = document.getElementById('recordDetailModal');
    if (modal) {
        modal.remove();
    }
}

// ============================================================
// 模板执行记录查询相关功能
// ============================================================

// 当前模板记录类型（callgraph 或 findstack）
let currentTemplateRecordType = 'callgraph';

/**
 * 显示模板执行记录查询模态框
 */
function showTemplateExecutionRecordModal() {
    if (!currentTemplate) {
        showToast('请先选择模板', 'warning');
        return;
    }
    
    // 重置查询条件
    document.getElementById('templateQueryMinStartTime').value = '';
    document.getElementById('templateQueryPageSize').value = '10';
    
    // 清空记录列表（显示加载中）
    document.getElementById('templateExecutionRecordList').innerHTML = '<div class="text-muted">加载中...</div>';
    document.getElementById('templateExecutionRecordPagination').innerHTML = '';
    
    // 重置Tab状态
    currentTemplateRecordType = 'callgraph';
    const tabs = document.querySelectorAll('#templateExecutionRecordModal .tabs .tab');
    tabs.forEach((tab, index) => {
        tab.classList.toggle('active', index === 0);
    });
    
    // 显示模态框
    document.getElementById('templateExecutionRecordModal').classList.remove('hidden');
    
    // 使用默认条件查询一次
    queryTemplateExecutionRecords(1);
}

/**
 * 关闭模板执行记录查询模态框
 */
function closeTemplateExecutionRecordModal() {
    document.getElementById('templateExecutionRecordModal').classList.add('hidden');
}

/**
 * 切换模板记录类型
 */
function switchTemplateRecordType(type) {
    currentTemplateRecordType = type;
    
    // 更新Tab状态
    const tabs = document.querySelectorAll('#templateExecutionRecordModal .tabs .tab');
    tabs.forEach((tab, index) => {
        if (type === 'callgraph') {
            tab.classList.toggle('active', index === 0);
        } else {
            tab.classList.toggle('active', index === 1);
        }
    });
    
    // 重新查询
    queryTemplateExecutionRecords(1);
}

/**
 * 查询模板执行记录
 */
async function queryTemplateExecutionRecords(pageNum = 1) {
    if (!currentTemplate) {
        showToast('请先选择模板', 'warning');
        return;
    }
    
    // 获取查询参数
    const minStartTime = document.getElementById('templateQueryMinStartTime').value;
    const pageSize = parseInt(document.getElementById('templateQueryPageSize').value);
    
    // 构建查询URL
    let url;
    if (currentTemplateRecordType === 'callgraph') {
        url = `${API_BASE}/template/execution/call-graph/records/${currentTemplate.templateId}?page=${pageNum}&pageSize=${pageSize}`;
    } else {
        url = `${API_BASE}/template/execution/find-stack/records/${currentTemplate.templateId}?page=${pageNum}&pageSize=${pageSize}`;
    }
    
    if (minStartTime) {
        // 转换为 yyyy-MM-dd HH:mm:ss 格式
        const date = new Date(minStartTime);
        const formatted = date.getFullYear() + '-' +
            String(date.getMonth() + 1).padStart(2, '0') + '-' +
            String(date.getDate()).padStart(2, '0') + ' ' +
            String(date.getHours()).padStart(2, '0') + ':' +
            String(date.getMinutes()).padStart(2, '0') + ':' +
            String(date.getSeconds()).padStart(2, '0');
        url += `&minStartTime=${encodeURIComponent(formatted)}`;
    }
    
    try {
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 200) {
            renderTemplateExecutionRecordList(result.data);
        } else {
            showToast('查询失败: ' + result.message, 'error');
        }
    } catch (error) {
        console.error('查询模板执行记录失败', error);
        showToast('查询模板执行记录失败', 'error');
    }
}

/**
 * 渲染模板执行记录列表
 */
function renderTemplateExecutionRecordList(data) {
    const container = document.getElementById('templateExecutionRecordList');
    
    if (!data.records || data.records.length === 0) {
        container.innerHTML = '<div class="text-muted">暂无执行记录</div>';
        document.getElementById('templateExecutionRecordPagination').innerHTML = '';
        return;
    }
    
    // 渲染记录列表
    let html = '<table class="record-table">';
    html += '<thead><tr>';
    if (currentTemplateRecordType === 'callgraph') {
        html += '<th>执行ID</th>';
        html += '<th>方向</th>';
        html += '<th>入口类/方法</th>';
        html += '<th>开始时间</th>';
        html += '<th>结束时间</th>';
        html += '<th>耗时</th>';
        html += '<th>状态</th>';
        html += '<th>操作</th>';
    } else {
        html += '<th>执行ID</th>';
        html += '<th>方向</th>';
        html += '<th>入口类/方法</th>';
        html += '<th>关键字</th>';
        html += '<th>开始时间</th>';
        html += '<th>结束时间</th>';
        html += '<th>耗时</th>';
        html += '<th>状态</th>';
        html += '<th>操作</th>';
    }
    html += '</tr></thead>';
    html += '<tbody>';
    
    data.records.forEach(record => {
        const entryMethodsShort = record.entryMethods ? 
            (record.entryMethods.length > 30 ? record.entryMethods.substring(0, 30) + '...' : record.entryMethods) 
            : '-';
        const entryMethodsFull = record.entryMethods || '-';
        const keywordsShort = record.keywords ? 
            (record.keywords.length > 30 ? record.keywords.substring(0, 30) + '...' : record.keywords) 
            : '-';
        const keywordsFull = record.keywords || '-';
        const durationText = record.duration ? formatDuration(record.duration) : '-';
        
        html += '<tr>';
        html += `<td>${escapeHtml(record.execId || '-')}</td>`;
        html += `<td>${record.direction === 'caller' ? '向下' : '向上'}</td>`;
        html += `<td title="${escapeHtml(entryMethodsFull.replace(/\n/g, '&#10;'))}">${escapeHtml(entryMethodsShort.replace(/\n/g, '<br>'))}</td>`;
        if (currentTemplateRecordType === 'findstack') {
            html += `<td title="${escapeHtml(keywordsFull.replace(/\n/g, '&#10;'))}">${escapeHtml(keywordsShort.replace(/\n/g, '<br>'))}</td>`;
        }
        html += `<td>${formatDateTime(record.startTime)}</td>`;
        html += `<td>${record.endTime ? formatDateTime(record.endTime) : '-'}</td>`;
        html += `<td>${durationText}</td>`;
        html += `<td><span class="exec-status status-${record.status}">${getStatusText(record.status)}</span></td>`;
        html += `<td>
            <button class="btn btn-sm btn-info" onclick="showTemplateExecutionRecordDetail(${record.id}, '${currentTemplateRecordType}')">详情</button>
            ${record.outputDir ? `<button class="btn btn-sm btn-secondary output-dir-btn" data-record-type="${currentTemplateRecordType}" data-record-id="${record.id}">打开目录</button>` : ''}
        </td>`;
        html += '</tr>';
    });
    
    html += '</tbody></table>';
    container.innerHTML = html;
    
    // 渲染分页
    renderTemplateExecutionRecordPagination(data);
}

/**
 * 渲染模板执行记录分页
 */
function renderTemplateExecutionRecordPagination(data) {
    const container = document.getElementById('templateExecutionRecordPagination');
    
    const total = data.total || data.records.length;
    
    if (total <= 0) {
        container.innerHTML = `<div class="pagination-info">共 0 条记录</div>`;
        return;
    }
    
    let html = '<div class="pagination-controls">';
    html += `<span class="pagination-info">共 ${total} 条记录</span>`;
    
    html += '</div>';
    container.innerHTML = html;
}

/**
 * 显示模板执行记录详情
 */
async function showTemplateExecutionRecordDetail(id, type) {
    // 创建详情模态框
    let modalHtml = `
        <div class="modal" id="templateRecordDetailModal" onclick="closeTemplateRecordDetailModal(event)">
            <div class="modal-content" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h3>${type === 'callgraph' ? '生成调用链记录详情' : '生成堆栈记录详情'}</h3>
                    <button class="modal-close" onclick="closeTemplateRecordDetailModal()">&times;</button>
                </div>
                <div id="templateRecordDetailBody" class="modal-body">
                    <div class="text-center">加载中...</div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary" onclick="closeTemplateRecordDetailModal()">关闭</button>
                </div>
            </div>
        </div>
    `;
    
    // 移除已存在的模态框
    const existingModal = document.getElementById('templateRecordDetailModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // 添加模态框到body
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    try {
        let url;
        if (type === 'callgraph') {
            url = `${API_BASE}/template/execution/call-graph/detail/${id}`;
        } else {
            url = `${API_BASE}/template/execution/find-stack/detail/${id}`;
        }
        
        const response = await fetch(url);
        const result = await response.json();
        
        if (result.code === 200) {
            const record = result.data;
            // 格式化入口类/方法列表（每行显示一个）
            const entryMethodsHtml = record.entryMethods 
                ? record.entryMethods.split('\n').map(f => `<div>${escapeHtml(f)}</div>`).join('') 
                : '-';
            
            // 格式化关键字列表（每行显示一个）
            const keywordsHtml = record.keywords 
                ? record.keywords.split('\n').map(f => `<div>${escapeHtml(f)}</div>`).join('') 
                : '-';
            
            const durationText = record.duration ? formatDuration(record.duration) : '-';
            
            let detailHtml = `
                <table class="detail-table">
                    <tr><th>执行ID</th><td>${escapeHtml(record.execId || '-')}</td></tr>
                    <tr><th>项目ID</th><td>${escapeHtml(record.projectId)}</td></tr>
                    <tr><th>模板ID</th><td>${escapeHtml(record.templateId)}</td></tr>
                    <tr><th>调用链方向</th><td>${record.direction === 'caller' ? '向下调用链' : '向上调用链'}</td></tr>
                    <tr><th>入口类/方法列表</th><td class="jar-files-list">${entryMethodsHtml}</td></tr>
                    ${type === 'findstack' ? `<tr><th>关键字列表</th><td class="jar-files-list">${keywordsHtml}</td></tr>` : ''}
                    <tr><th>开始时间</th><td>${formatDateTime(record.startTime)}</td></tr>
                    <tr><th>结束时间</th><td>${record.endTime ? formatDateTime(record.endTime) : '-'}</td></tr>
                    <tr><th>执行耗时</th><td>${durationText}</td></tr>
                    <tr><th>执行状态</th><td><span class="exec-status status-${record.status}">${getStatusText(record.status)}</span></td></tr>
                    <tr><th>错误信息</th><td>${record.errorMessage ? escapeHtml(record.errorMessage) : '-'}</td></tr>
                    <tr><th>输出目录路径</th><td>${record.outputDir ? escapeHtml(record.outputDir) : '-'}</td></tr>
                    <tr><th>日志文件路径</th><td>${record.logFilePath ? escapeHtml(record.logFilePath) : '-'}</td></tr>
                    ${record.callGraphFiles ? `<tr><th>调用链文件</th><td><button class="btn btn-sm btn-info" onclick="showCallGraphFilesModal(${record.id})">查看调用链文件</button></td></tr>` : ''}
                </table>
            `;
            document.getElementById('templateRecordDetailBody').innerHTML = detailHtml;
        } else {
            document.getElementById('templateRecordDetailBody').innerHTML = `<div class="text-muted">加载失败: ${result.message}</div>`;
        }
    } catch (error) {
        console.error('获取模板执行记录详情失败', error);
        document.getElementById('templateRecordDetailBody').innerHTML = '<div class="text-muted">加载失败</div>';
    }
}

/**
 * 关闭模板执行记录详情模态框
 */
function closeTemplateRecordDetailModal(event) {
    if (event && event.target !== event.currentTarget) {
        return;
    }
    const modal = document.getElementById('templateRecordDetailModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 打开执行记录的输出目录（安全方式，后端处理路径）
 * @param {string} recordType - 记录类型：callgraph 或 findstack
 * @param {number} recordId - 记录ID
 */
async function openExecutionOutputDirectory(recordType, recordId) {
    try {
        const response = await fetch(`${API_BASE}/system/open-execution-output-directory`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ recordType: recordType, recordId: recordId })
        });
        const result = await response.json();
        if (result.code === 200) {
            showToast('目录已打开', 'success');
        } else {
            showToast(result.message || '打开目录失败', 'error');
        }
    } catch (error) {
        console.error('打开输出目录失败', error);
        showToast('打开输出目录失败', 'error');
    }
}

/**
 * 事件委托：处理打开输出目录按钮点击
 * 使用安全的后端处理方式，避免前端直接传递路径
 */
document.addEventListener('click', function(e) {
    const btn = e.target.closest('.output-dir-btn');
    if (btn) {
        const recordType = btn.getAttribute('data-record-type');
        const recordId = btn.getAttribute('data-record-id');
        if (recordType && recordId) {
            openExecutionOutputDirectory(recordType, parseInt(recordId, 10));
        }
    }
});

/**
 * 显示调用链文件路径Map的模态框
 * @param {number} recordId - 调用链执行记录ID
 */
async function showCallGraphFilesModal(recordId) {
    let modalHtml = `
        <div class="modal" id="callGraphFilesModal" onclick="closeCallGraphFilesModal(event)">
            <div class="modal-content" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h3>调用链文件路径</h3>
                    <button class="modal-close" onclick="closeCallGraphFilesModal()">&times;</button>
                </div>
                <div id="callGraphFilesBody" class="modal-body">
                    <div class="text-center">加载中...</div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary" onclick="closeCallGraphFilesModal()">关闭</button>
                </div>
            </div>
        </div>
    `;
    
    const existingModal = document.getElementById('callGraphFilesModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    document.body.insertAdjacentHTML('beforeend', modalHtml);
    
    try {
        const response = await fetch(`${API_BASE}/template/execution/call-graph/files/${recordId}`);
        const result = await response.json();

        if (result.code === 200) {
            const fileInfoList = result.data;
            if (!fileInfoList || fileInfoList.length === 0) {
                document.getElementById('callGraphFilesBody').innerHTML = '<div class="text-muted">无调用链文件信息</div>';
                return;
            }

            let html = '<table class="record-table">';
            html += '<thead><tr><th>入口方法</th><th>原始文本</th><th>文件路径</th></tr></thead>';
            html += '<tbody>';

            for (const fileInfo of fileInfoList) {
                html += '<tr>';
                html += `<td>${escapeHtml(fileInfo.entryMethod || '-')}</td>`;
                html += `<td>${escapeHtml(fileInfo.origText || '-')}</td>`;
                html += `<td style="word-break: break-all;">${escapeHtml(fileInfo.filePath || '-')}</td>`;
                html += '</tr>';
            }
            
            html += '</tbody></table>';
            document.getElementById('callGraphFilesBody').innerHTML = html;
        } else {
            document.getElementById('callGraphFilesBody').innerHTML = `<div class="text-muted">加载失败: ${result.message}</div>`;
        }
    } catch (error) {
        console.error('获取调用链文件信息失败', error);
        document.getElementById('callGraphFilesBody').innerHTML = '<div class="text-muted">加载失败</div>';
    }
}

/**
 * 关闭调用链文件模态框
 */
function closeCallGraphFilesModal(event) {
    if (event && event.target !== event.currentTarget) {
        return;
    }
    const modal = document.getElementById('callGraphFilesModal');
    if (modal) {
        modal.remove();
    }
}