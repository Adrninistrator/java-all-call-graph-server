/**
 * Java All Call Graph Server 前端应用 - 配置渲染模块
 * 包含：配置表格渲染、配置数据收集、配置描述显示、依赖规则处理
 */

/**
 * 渲染配置表格
 */
function renderConfigTable(configs, prefix) {
    return renderConfigTableWithValues(configs, prefix, {});
}

/**
 * 渲染配置表格（带当前值）
 * 采用上下布局：参数名和描述在上方，值编辑区在下方
 * 对于主配置和数据库配置，如果所有配置项都来自同一个配置文件，则首先展示配置文件名称
 * @param {Array} configs 配置项列表
 * @param {string} prefix 前缀
 * @param {Object} currentValues 当前值
 * @param {boolean} readOnly 是否只读模式（默认false）
 */
function renderConfigTableWithValues(configs, prefix, currentValues, readOnly = false) {
    // 判断是否为主配置或数据库配置（仅这两种需要显示配置文件名称）
    const isMainOrDbConfig = prefix.includes('.mainConfig') || prefix.includes('.dbConfig');
    
    // 检查是否所有配置项都来自同一个配置文件
    let configFileName = '';
    if (isMainOrDbConfig && configs && configs.length > 0) {
        const firstFileName = configs[0].fileName;
        const allSameFile = configs.every(config => config.fileName === firstFileName);
        if (allSameFile && firstFileName) {
            configFileName = firstFileName;
        }
    }
    
    let html = '';
    // 如果所有配置项都来自同一个配置文件，首先展示配置文件名称（仅主配置和数据库配置）
    if (configFileName) {
        html += `<div class="config-file-name">配置文件：${configFileName}</div>`;
    }
    html += '<div class="config-list">';
    configs.forEach(config => {
        // 获取当前值，优先级：传入的值 > config.currentValue > 默认值
        let currentValue = currentValues[config.key] !== undefined ? currentValues[config.key] : config.defaultValue;
        // 对于只读配置项，使用 currentValue 属性（项目中的值）
        if (!config.editable && config.currentValue !== undefined) {
            currentValue = config.currentValue;
        }
        
        // 计算初始状态：如果有依赖关系，根据依赖条件判断初始状态
        let rowAttrs = '';
        let disabledAttr = '';
        let initialDisabled = false;
        
        if (config.dependsOn) {
            // 添加data-depends-on属性用于联动
            rowAttrs = 'data-depends-on="' + encodeURIComponent(JSON.stringify(config.dependsOn)) + '"';
            
            // 根据依赖参数的当前值判断初始状态
            const dependsOn = config.dependsOn;
            const dependKey = dependsOn.paramKey;
            // 获取依赖参数的当前值（从currentValues或默认值）
            let dependValue = currentValues[dependKey];
            if (dependValue === undefined) {
                // 从configs中查找依赖参数的默认值
                const dependConfig = configs.find(c => c.key === dependKey);
                dependValue = dependConfig ? dependConfig.defaultValue : null;
            }
            
            // 判断依赖条件是否满足
            const conditionMet = dependsOn.paramValue === String(dependValue);
            if (dependsOn.action === 'enable') {
                // enable: 条件满足时启用，否则禁用
                initialDisabled = !conditionMet;
            } else if (dependsOn.action === 'disable') {
                // disable: 条件满足时禁用，否则启用
                initialDisabled = conditionMet;
            }
        }
        
        // 对于不可编辑的配置项，添加 disabled 属性
        // 如果是只读模式，所有配置项都禁用
        if (!config.editable || initialDisabled || readOnly) {
            disabledAttr = 'disabled';
        }
        
        // 添加 data-editable 和 data-readonly 属性，用于依赖关系处理时判断是否可编辑
        const editableAttr = `data-editable="${config.editable !== false && !readOnly}"`;
        
        // 设置初始样式
        // 对于不可编辑的配置项（editable=false 或 readOnly），也需要设置禁用样式
        let rowStyle = '';
        if (initialDisabled || !config.editable || readOnly) {
            rowStyle = 'style="opacity: 0.5;"';
        }
        
        html += `<div class="config-item" ${rowAttrs} data-config-key="${config.key}" ${editableAttr} ${rowStyle}>`;
        
        // 上部：参数名和描述
        html += '<div class="config-item-header">';
        html += `<div class="config-item-name">${config.name}</div>`;
        html += `<div class="config-item-desc">
            <span class="desc-brief">${config.descriptionBrief || ''}</span>
            ${config.description && config.description.length > 1 ? 
                `<button type="button" class="btn-desc-help" onclick="showDescriptionDetail('${config.name}', ${JSON.stringify(config.description).replace(/"/g, '&quot;')})" title="查看完整说明">?</button>` : 
                ''}
        </div>`;
        html += '</div>';
        
        // 下部：值编辑区
        html += '<div class="config-item-value">';
        if (config.enumOptions && config.enumOptions.length > 0) {
            // 枚举类型：使用下拉框
            html += `<select class="config-select" name="${prefix}.${config.key}" data-config-key="${config.key}" onchange="handleConfigChange(this)" ${disabledAttr}>`;
            config.enumOptions.forEach(option => {
                const selected = option.value === currentValue ? 'selected' : '';
                html += `<option value="${option.value}" ${selected}>${option.value} - ${option.description}</option>`;
            });
            html += '</select>';
        } else if (config.type === 'Boolean') {
            // Boolean类型：使用单选框，显示"是""否"
            const trueChecked = currentValue === 'true' || currentValue === true ? 'checked' : '';
            const falseChecked = currentValue === 'false' || currentValue === false ? 'checked' : '';
            html += `<div class="config-radio-group">
                <label class="config-radio-label">
                    <input type="radio" name="${prefix}.${config.key}" value="true" ${trueChecked} data-config-key="${config.key}" onchange="handleConfigChange(this)" ${disabledAttr}> 是
                </label>
                <label class="config-radio-label">
                    <input type="radio" name="${prefix}.${config.key}" value="false" ${falseChecked} data-config-key="${config.key}" onchange="handleConfigChange(this)" ${disabledAttr}> 否
                </label>
            </div>`;
        } else if (config.type === 'Integer') {
            // Integer类型：使用数字输入框
            const minAttr = config.minValue !== null && config.minValue !== undefined ? `min="${config.minValue}"` : 'min="0"';
            const maxAttr = config.maxValue !== null && config.maxValue !== undefined ? `max="${config.maxValue}"` : '';
            html += `<input type="number" class="config-input" name="${prefix}.${config.key}" value="${currentValue || ''}" ${minAttr} ${maxAttr} data-config-key="${config.key}" onchange="handleConfigChange(this)" ${disabledAttr}>`;
        } else {
            // 其他类型：使用文本输入框
            html += `<input type="text" class="config-input" name="${prefix}.${config.key}" value="${currentValue || ''}" data-config-key="${config.key}" onchange="handleConfigChange(this)" ${disabledAttr}>`;
        }
        html += '</div>';
        
        html += '</div>';
    });
    html += '</div>';
    return html;
}

/**
 * 渲染列表配置表格
 */
function renderListConfigTable(configs, prefix) {
    return renderListConfigTableWithValues(configs, prefix, {});
}

/**
 * 渲染列表配置表格（带当前值）
 * 采用上下布局：参数名和描述在上方，值编辑区在下方
 */
function renderListConfigTableWithValues(configs, prefix, currentValues) {
    // 过滤不可见的配置项
    const visibleConfigs = configs.filter(config => config.visible !== false);
    
    // 如果没有可见配置项，返回空字符串
    if (visibleConfigs.length === 0) {
        return '';
    }
    
    // 判断是否为EL配置（用于添加示例按钮）
    const isElConfig = prefix.includes('.elConfig');
    // 确定EL配置类型（javacg2 或 jacg）
    let elConfigType = '';
    if (isElConfig) {
        if (prefix.includes('javaCG2') || prefix.includes('javacg2')) {
            elConfigType = 'javacg2';
        } else {
            elConfigType = 'jacg';
        }
    }
    
    let html = '';
    html += '<div class="config-list">';
    visibleConfigs.forEach(config => {
        // 获取当前值
        const currentValue = currentValues[config.key];
        // EL配置是字符串，List/Set配置是数组
        let currentValueText = '';
        if (isElConfig) {
            // EL配置直接使用字符串值
            currentValueText = typeof currentValue === 'string' ? currentValue : '';
        } else {
            // List/Set配置使用数组，用换行连接
            currentValueText = Array.isArray(currentValue) ? currentValue.join('\n') : '';
        }
        
        // 生成唯一ID用于textarea
        const textareaId = `textarea_${prefix.replace(/\./g, '_')}_${config.key}`;
        
        html += '<div class="config-item">';
        
        // 上部：参数名和描述
        html += '<div class="config-item-header">';
        html += `<div class="config-item-name">${config.name}</div>`;
        html += `<div class="config-item-desc">
            <span class="desc-brief">${config.descriptionBrief || ''}</span>
            ${config.description && config.description.length > 1 ? 
                `<button type="button" class="btn-desc-help" onclick="showDescriptionDetail('${config.name}', ${JSON.stringify(config.description).replace(/"/g, '&quot;')})" title="查看完整说明">?</button>` : 
                ''}
            ${isElConfig ? 
                `<button type="button" class="btn-el-example" onclick="showElExample('${elConfigType}', '${config.key}', '${config.name}')" title="查看示例">示例</button>` : 
                ''}
        </div>`;
        html += '</div>';
        
        // 下部：值编辑区（使用textarea，适合多行输入）
        html += '<div class="config-item-value">';
        if (isElConfig) {
            html += `<textarea id="${textareaId}" class="config-textarea el-textarea" name="${prefix}.${config.key}" placeholder="右键支持表达式快捷输入">${currentValueText}</textarea>`;
        } else {
            html += `<textarea class="config-textarea" name="${prefix}.${config.key}" placeholder="每行一个值">${currentValueText}</textarea>`;
        }
        html += '</div>';
        
        html += '</div>';
    });
    html += '</div>';
    return html;
}

/**
 * 显示完整描述详情
 */
function showDescriptionDetail(paramName, descriptions, fileName) {
    let html = `<div class="desc-detail-modal">`;
    
    // 显示配置文件名（不显示参数名称）
    if (fileName) {
        html += `<div class="desc-detail-filename">配置文件：${fileName}</div>`;
    }
    
    html += `<div class="desc-detail-content">`;
    if (Array.isArray(descriptions)) {
        descriptions.forEach((desc, index) => {
            html += `<p>${index + 1}. ${desc}</p>`;
        });
    } else {
        html += `<p>${descriptions}</p>`;
    }
    html += '</div></div>';
    
    // 使用alert的替代方案：创建一个简单的模态框
    const modalHtml = `
        <div class="modal" id="descModal" onclick="closeDescModal(event)">
            <div class="modal-content" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h3>参数说明详情</h3>
                    <button class="modal-close" onclick="closeDescModal()">&times;</button>
                </div>
                <div class="modal-body">
                    ${html}
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary" onclick="closeDescModal()">关闭</button>
                </div>
            </div>
        </div>
    `;
    
    // 移除已存在的描述模态框
    const existingModal = document.getElementById('descModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // 添加新的模态框到body
    document.body.insertAdjacentHTML('beforeend', modalHtml);
}

/**
 * 关闭描述详情模态框
 */
function closeDescModal(event) {
    if (event && event.target !== event.currentTarget) {
        return;
    }
    const modal = document.getElementById('descModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 显示配置参数描述（从后端API获取）
 */
async function showConfigDescription(configType, enumName) {
    try {
        const response = await fetch(`${API_BASE}/config/description/${configType}/${enumName}`);
        const result = await response.json();
        if (result.code === 200 && result.data && result.data.descriptions) {
            // 使用参数名称（配置文件名）而不是枚举名称
            const paramName = result.data.paramName || enumName;
            const fileName = result.data.fileName || '';
            showDescriptionDetail(paramName, result.data.descriptions, fileName);
        } else {
            showToast('未找到参数说明', 'warning');
        }
    } catch (error) {
        console.error('获取参数描述失败', error);
        showToast('获取参数描述失败', 'error');
    }
}

/**
 * 根据方向显示配置参数描述（从后端API获取）
 * 后端根据方向返回对应的配置信息，前端只负责展示
 * 
 * @param configType 配置类型（entry-point/find-stack-keyword）
 * @param direction 调用链方向（caller/callee）
 */
async function showConfigDescriptionByDirection(configType, direction) {
    try {
        const response = await fetch(`${API_BASE}/config/description/${configType}/${direction}`);
        const result = await response.json();
        if (result.code === 200 && result.data && result.data.descriptions) {
            const paramName = result.data.paramName || '';
            const fileName = result.data.fileName || '';
            showDescriptionDetail(paramName, result.data.descriptions, fileName);
        } else {
            showToast('未找到参数说明', 'warning');
        }
    } catch (error) {
        console.error('获取参数描述失败', error);
        showToast('获取参数描述失败', 'error');
    }
}

/**
 * 切换配置标签页
 */
function switchConfigTab(element, tabId) {
    // 找到最近的模态框容器，只在当前模态框内查找
    const modal = element.closest('.modal-content');
    if (!modal) return;

    // 只在当前模态框内操作 tab 和 tab-content
    modal.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    modal.querySelectorAll('.tab-content').forEach(t => t.classList.remove('active'));
    element.classList.add('active');

    const tabContent = modal.querySelector('#tab-' + tabId);
    if (tabContent) {
        tabContent.classList.add('active');
    }
}

/**
 * 处理配置参数变化
 */
function handleConfigChange(element) {
    // 获取当前参数的key和值
    const configKey = element.getAttribute('data-config-key');
    let configValue;
    
    if (element.type === 'radio') {
        // 单选框：获取选中的值
        const radioGroup = document.querySelectorAll(`input[data-config-key="${configKey}"]`);
        radioGroup.forEach(radio => {
            if (radio.checked) {
                configValue = radio.value;
            }
        });
    } else if (element.type === 'checkbox') {
        configValue = element.checked ? 'true' : 'false';
    } else {
        configValue = element.value;
    }
    
    // 应用依赖规则
    applyDependencyRules(configKey, configValue);
}

/**
 * 应用依赖规则
 * @param {string} changedKey - 变化的参数key
 * @param {string} changedValue - 变化后的值
 */
function applyDependencyRules(changedKey, changedValue) {
    // 查找所有有依赖关系的配置项
    const dependentItems = document.querySelectorAll('.config-item[data-depends-on]');
    
    dependentItems.forEach(item => {
        try {
            const dependsOnStr = item.getAttribute('data-depends-on');
            if (!dependsOnStr) return;
            
            const dependsOn = JSON.parse(decodeURIComponent(dependsOnStr));
            
            // 检查是否依赖当前变化的参数
            if (dependsOn.paramKey === changedKey) {
                const input = item.querySelector('input, select, textarea');
                const conditionMet = dependsOn.paramValue === changedValue;
                
                // 检查配置项是否可编辑（通过 data-editable 属性判断）
                const editableAttr = item.getAttribute('data-editable');
                const isEditable = editableAttr === 'true';
                
                switch (dependsOn.action) {
                    case 'enable':
                        // 条件满足时启用，否则禁用
                        // 但如果配置项本身不可编辑（editable=false 或 readOnly模式），则保持禁用
                        if (input) {
                            const shouldEnable = conditionMet && isEditable;
                            input.disabled = !shouldEnable;
                            if (!shouldEnable) {
                                input.classList.add('disabled');
                            } else {
                                input.classList.remove('disabled');
                            }
                        }
                        // 设置整行样式：如果配置项不可编辑，始终保持禁用样式
                        item.style.opacity = (conditionMet && isEditable) ? '1' : '0.5';
                        break;
                    case 'disable':
                        // 条件满足时禁用，否则启用
                        // 但如果配置项本身不可编辑，则保持禁用
                        if (input) {
                            const shouldDisable = conditionMet || !isEditable;
                            input.disabled = shouldDisable;
                            if (shouldDisable) {
                                input.classList.add('disabled');
                            } else {
                                input.classList.remove('disabled');
                            }
                        }
                        // 设置整行样式：如果配置项不可编辑，始终保持禁用样式
                        item.style.opacity = (conditionMet || !isEditable) ? '0.5' : '1';
                        break;
                    case 'show':
                        // 条件满足时显示，否则隐藏
                        item.style.display = conditionMet ? '' : 'none';
                        break;
                    case 'hide':
                        // 条件满足时隐藏，否则显示
                        item.style.display = conditionMet ? 'none' : '';
                        break;
                }
            }
        } catch (e) {
            console.error('解析依赖关系失败', e);
        }
    });
}

/**
 * 初始化配置依赖关系
 * 在配置表格渲染完成后调用，初始化所有依赖参数的状态
 */
function initDependencyRules() {
    // 遍历所有配置参数，应用初始依赖规则
    const configInputs = document.querySelectorAll('[data-config-key]');
    configInputs.forEach(input => {
        const configKey = input.getAttribute('data-config-key');
        let configValue;
        
        if (input.type === 'radio') {
            if (input.checked) {
                configValue = input.value;
                applyDependencyRules(configKey, configValue);
            }
        } else {
            configValue = input.value;
            applyDependencyRules(configKey, configValue);
        }
    });
}

/**
 * 收集JavaCG2配置数据
 */
function collectJavaCG2Config() {
    const configModalBody = document.getElementById('configModalBody');
    if (!configModalBody || configModalBody.innerHTML === '') {
        return null;
    }

    const config = {
        mainConfig: {},
        listConfig: {},
        setConfig: {},
        elConfig: {}
    };

    // 收集主配置
    const mainConfigInputs = configModalBody.querySelectorAll('[name^="javaCG2.mainConfig."]');
    mainConfigInputs.forEach(input => {
        if (input.type === 'radio' && !input.checked) return;
        const key = input.getAttribute('data-config-key');
        if (key) {
            config.mainConfig[key] = input.value;
        }
    });

    // 收集列表配置
    const listConfigTextareas = configModalBody.querySelectorAll('[name^="javaCG2.listConfig."]');
    listConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('javaCG2.listConfig.', '');
        const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l);
        if (lines.length > 0) {
            config.listConfig[key] = lines;
        }
    });

    // 收集Set配置
    const setConfigTextareas = configModalBody.querySelectorAll('[name^="javaCG2.setConfig."]');
    setConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('javaCG2.setConfig.', '');
        const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l);
        if (lines.length > 0) {
            config.setConfig[key] = lines;
        }
    });

    // 收集EL配置（EL表达式配置为单个字符串，不需要按行分割）
    const elConfigTextareas = configModalBody.querySelectorAll('[name^="javaCG2.elConfig."]');
    elConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('javaCG2.elConfig.', '');
        const value = textarea.value.trim();
        if (value) {
            config.elConfig[key] = value;
        }
    });

    return config;
}

/**
 * 收集JACG配置数据
 */
function collectJACGConfig() {
    const configModalBody = document.getElementById('configModalBody');
    if (!configModalBody || configModalBody.innerHTML === '') {
        return null;
    }

    const config = {
        mainConfig: {},
        dbConfig: {},
        listConfig: {},
        setConfig: {},
        elConfig: {}
    };

    // 收集主配置
    const mainConfigInputs = configModalBody.querySelectorAll('[name^="jacg.mainConfig."]');
    mainConfigInputs.forEach(input => {
        if (input.type === 'radio' && !input.checked) return;
        const key = input.getAttribute('data-config-key');
        if (key) {
            config.mainConfig[key] = input.value;
        }
    });

    // 收集数据库配置
    const dbConfigInputs = configModalBody.querySelectorAll('[name^="jacg.dbConfig."]');
    dbConfigInputs.forEach(input => {
        if (input.type === 'radio' && !input.checked) return;
        const key = input.getAttribute('data-config-key');
        if (key) {
            config.dbConfig[key] = input.value;
        }
    });

    // 收集列表配置
    const listConfigTextareas = configModalBody.querySelectorAll('[name^="jacg.listConfig."]');
    listConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('jacg.listConfig.', '');
        const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l);
        if (lines.length > 0) {
            config.listConfig[key] = lines;
        }
    });

    // 收集Set配置
    const setConfigTextareas = configModalBody.querySelectorAll('[name^="jacg.setConfig."]');
    setConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('jacg.setConfig.', '');
        const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l);
        if (lines.length > 0) {
            config.setConfig[key] = lines;
        }
    });

    // 收集EL配置（EL表达式配置为单个字符串，不需要按行分割）
    const elConfigTextareas = configModalBody.querySelectorAll('[name^="jacg.elConfig."]');
    elConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('jacg.elConfig.', '');
        const value = textarea.value.trim();
        if (value) {
            config.elConfig[key] = value;
        }
    });

    return config;
}

/**
 * 收集模板配置数据
 */
function collectTemplateConfig() {
    // 优先使用templateConfigModalBody，如果没有则使用configModalBody
    let configModalBody = document.getElementById('templateConfigModalBody');
    if (!configModalBody || configModalBody.innerHTML === '') {
        configModalBody = document.getElementById('configModalBody');
    }
    if (!configModalBody || configModalBody.innerHTML === '') {
        return null;
    }

    const config = {
        mainConfig: {},
        dbConfig: {},
        listConfig: {},
        setConfig: {},
        elConfig: {}
    };

    // 收集主配置（包括disabled的只读配置项，如CKE_APP_NAME）
    const mainConfigInputs = configModalBody.querySelectorAll('[name^="template.mainConfig."]');
    mainConfigInputs.forEach(input => {
        if (input.type === 'radio' && !input.checked) return;
        const key = input.getAttribute('data-config-key');
        if (key) {
            config.mainConfig[key] = input.value;
        }
    });

    // 收集数据库配置
    // 注意：模板的数据库配置不允许人工编辑，后端会自动使用项目的数据库配置覆盖
    // 但前端仍然需要收集当前显示的值（只读），以便后端处理
    const dbConfigInputs = configModalBody.querySelectorAll('[name^="template.dbConfig."]');
    dbConfigInputs.forEach(input => {
        if (input.type === 'radio' && !input.checked) return;
        const key = input.getAttribute('data-config-key');
        if (key) {
            config.dbConfig[key] = input.value;
        }
    });
    
    // 对于模板配置，数据库配置需要使用项目的值
    // 后端会自动覆盖，但前端也需要确保传递正确的值
    if (currentProject && currentProject.jacgConfig && currentProject.jacgConfig.dbConfig) {
        config.dbConfig = Object.assign({}, currentProject.jacgConfig.dbConfig);
    }

    // 收集列表配置
    const listConfigTextareas = configModalBody.querySelectorAll('[name^="template.listConfig."]');
    listConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('template.listConfig.', '');
        const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l);
        if (lines.length > 0) {
            config.listConfig[key] = lines;
        }
    });

    // 收集Set配置
    const setConfigTextareas = configModalBody.querySelectorAll('[name^="template.setConfig."]');
    setConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('template.setConfig.', '');
        const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l);
        if (lines.length > 0) {
            config.setConfig[key] = lines;
        }
    });

    // 收集EL配置
    const elConfigTextareas = configModalBody.querySelectorAll('[name^="template.elConfig."]');
    elConfigTextareas.forEach(textarea => {
        const name = textarea.getAttribute('name');
        const key = name.replace('template.elConfig.', '');
        const lines = textarea.value.split('\n').map(l => l.trim()).filter(l => l);
        if (lines.length > 0) {
            config.elConfig[key] = lines;
        }
    });

    // 对于模板配置，确保 CKE_APP_NAME 使用项目中的值
    if (currentProject && currentProject.jacgConfig && currentProject.jacgConfig.mainConfig) {
        const projectAppName = currentProject.jacgConfig.mainConfig['CKE_APP_NAME'];
        if (projectAppName !== undefined) {
            config.mainConfig['CKE_APP_NAME'] = projectAppName;
        }
    }

    return config;
}