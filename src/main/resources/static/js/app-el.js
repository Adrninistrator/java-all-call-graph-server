/**
 * Java All Call Graph Server 前端应用 - EL表达式模块
 * 包含：EL表达式右键菜单、固定菜单加载、表达式插入
 */

/**
 * 加载EL表达式固定菜单
 */
async function loadElFixedMenu() {
    try {
        const response = await fetch(`${API_BASE}/config/el-menu/fixed`);
        const result = await response.json();
        if (result.code === 200) {
            elFixedMenu = result.data.categories;
        }
    } catch (error) {
        console.error('加载EL表达式固定菜单失败', error);
    }
}

/**
 * 初始化EL表达式右键菜单
 */
function initElContextMenu() {
    // 使用事件委托处理右键菜单
    document.addEventListener('contextmenu', function(e) {
        const target = e.target;
        if (target && target.classList && target.classList.contains('config-textarea')) {
            const name = target.getAttribute('name') || '';
            if (name.includes('.elConfig.')) {
                e.preventDefault();
                showElContextMenu(e, target, name);
            }
        }
    });
    
    // 点击其他地方关闭菜单
    document.addEventListener('click', function(e) {
        const menu = document.getElementById('elContextMenu');
        if (menu && !menu.contains(e.target)) {
            hideElContextMenu();
        }
    });
}

/**
 * 显示EL表达式右键菜单
 */
async function showElContextMenu(event, textarea, name) {
    // 解析配置类型和枚举名称
    const parts = name.split('.elConfig.');
    if (parts.length < 2) return;
    
    const enumName = parts[1];
    const type = name.includes('javaCG2') || name.includes('javacg2') ? 'javacg2' : 'jacg';
    
    // 使用HTML中预定义的菜单容器
    const menu = document.getElementById('elContextMenu');
    
    // 显示加载中
    menu.innerHTML = '<div class="el-menu-loading">加载中...</div>';
    menu.style.display = 'block';
    
    // 计算菜单位置，确保不超出屏幕
    const menuWidth = 420; // 菜单预估宽度
    const menuHeight = 400; // 菜单预估最大高度
    const scrollX = window.pageXOffset || document.documentElement.scrollLeft;
    const scrollY = window.pageYOffset || document.documentElement.scrollTop;
    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;
    
    let left = event.clientX;
    let top = event.clientY;
    
    // 确保菜单不超出右边界
    if (left + menuWidth > viewportWidth - 10) {
        left = viewportWidth - menuWidth - 10;
    }
    // 确保菜单不超出下边界
    if (top + menuHeight > viewportHeight - 10) {
        top = viewportHeight - menuHeight - 10;
    }
    // 确保菜单不超出左边界
    if (left < 10) {
        left = 10;
    }
    // 确保菜单不超出上边界
    if (top < 10) {
        top = 10;
    }
    
    menu.style.left = (left + scrollX) + 'px';
    menu.style.top = (top + scrollY) + 'px';
    
    try {
        // 加载变量菜单
        const varResponse = await fetch(`${API_BASE}/config/el-menu/variables/${type}/${enumName}`);
        const varResult = await varResponse.json();
        const variables = varResult.code === 200 ? varResult.data.variables : [];
        
        // 构建所有一级菜单数据
        const categories = [];
        
        // 变量菜单
        if (variables && variables.length > 0) {
            categories.push({
                id: 'el-menu-cat-variables',
                name: '变量',
                items: variables
            });
        }
        
        // 固定菜单
        if (elFixedMenu && elFixedMenu.length > 0) {
            elFixedMenu.forEach((category, index) => {
                categories.push({
                    id: `el-menu-cat-${index}`,
                    name: category.name,
                    items: category.items
                });
            });
        }
        
        // 渲染双列菜单
        let html = '<div class="el-menu-container">';
        
        // 左侧：一级菜单列表
        html += '<div class="el-menu-left">';
        categories.forEach((category, index) => {
            const isFirst = index === 0;
            html += `<div class="el-menu-category-item ${isFirst ? 'active' : ''}" data-category-id="${category.id}" onmouseenter="switchElMenuCategory('${category.id}')">${escapeHtml(category.name)}</div>`;
        });
        html += '</div>';
        
        // 右侧：二级菜单内容
        html += '<div class="el-menu-right">';
        categories.forEach((category, index) => {
            const isFirst = index === 0;
            html += `<div class="el-menu-items-panel ${isFirst ? 'active' : ''}" id="${category.id}">`;
            category.items.forEach(item => {
                html += `<div class="el-menu-item" onclick="insertElText('${textarea.id}', '${escapeHtml(item.insertText)}')">${escapeHtml(item.displayText)}</div>`;
            });
            html += '</div>';
        });
        html += '</div>';
        
        html += '</div>';
        
        menu.innerHTML = html;
        
        // 存储当前textarea的引用
        menu.dataset.textareaId = textarea.id;
        
    } catch (error) {
        console.error('加载EL菜单失败', error);
        menu.innerHTML = '<div class="el-menu-error">加载失败</div>';
    }
}

/**
 * 隐藏EL表达式右键菜单
 */
function hideElContextMenu() {
    document.getElementById('elContextMenu').style.display = 'none';
}

/**
 * 切换EL菜单一级菜单
 */
function switchElMenuCategory(categoryId) {
    // 更新一级菜单选中状态
    document.querySelectorAll('.el-menu-category-item').forEach(item => {
        item.classList.remove('active');
        if (item.getAttribute('data-category-id') === categoryId) {
            item.classList.add('active');
        }
    });
    
    // 更新二级菜单显示
    document.querySelectorAll('.el-menu-items-panel').forEach(panel => {
        panel.classList.remove('active');
    });
    const targetPanel = document.getElementById(categoryId);
    if (targetPanel) {
        targetPanel.classList.add('active');
    }
}

/**
 * 插入EL表达式文本
 */
function insertElText(textareaId, text) {
    const textarea = document.getElementById(textareaId);
    if (!textarea) return;
    
    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const value = textarea.value;
    
    // 假如光标不在最前面，则需要先插入空格再插入对应文本
    let insertText = text;
    if (start > 0) {
        // 检查光标前一个字符是否已经是空格，如果不是则添加空格
        const charBefore = value.charAt(start - 1);
        if (charBefore !== ' ' && charBefore !== '\n' && charBefore !== '\t') {
            insertText = ' ' + text;
        }
    }
    
    // 在光标位置插入文本
    textarea.value = value.substring(0, start) + insertText + value.substring(end);
    
    // 设置新的光标位置
    const newPos = start + insertText.length;
    textarea.selectionStart = newPos;
    textarea.selectionEnd = newPos;
    textarea.focus();
    
    // 关闭菜单
    hideElContextMenu();
}

/**
 * 显示EL表达式示例内容
 */
async function showElExample(type, enumName, paramName) {
    // 显示加载中
    const loadingHtml = `
        <div class="modal" id="elExampleModal" onclick="closeElExampleModal(event)">
            <div class="modal-content modal-content-large" onclick="event.stopPropagation()">
                <div class="modal-header">
                    <h3>EL表达式示例 - ${paramName}</h3>
                    <button class="modal-close" onclick="closeElExampleModal()">&times;</button>
                </div>
                <div class="modal-body">
                    <div class="el-example-loading">加载中...</div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-primary" onclick="closeElExampleModal()">关闭</button>
                </div>
            </div>
        </div>
    `;
    
    // 移除已存在的模态框
    const existingModal = document.getElementById('elExampleModal');
    if (existingModal) {
        existingModal.remove();
    }
    
    // 添加模态框到body
    document.body.insertAdjacentHTML('beforeend', loadingHtml);
    
    try {
        const response = await fetch(`${API_BASE}/config/el-example/${type}/${enumName}`);
        const result = await response.json();
        
        if (result.code === 200) {
            // 渲染Markdown内容
            const markdownHtml = renderMarkdown(result.data.content);
            document.querySelector('#elExampleModal .modal-body').innerHTML = `<div class="el-example-content">${markdownHtml}</div>`;
        } else {
            document.querySelector('#elExampleModal .modal-body').innerHTML = `<div class="el-example-error">加载失败: ${result.message}</div>`;
        }
    } catch (error) {
        console.error('加载EL示例失败', error);
        document.querySelector('#elExampleModal .modal-body').innerHTML = `<div class="el-example-error">加载失败: ${error.message}</div>`;
    }
}

/**
 * 关闭EL示例模态框
 */
function closeElExampleModal(event) {
    if (event && event.target !== event.currentTarget) {
        return;
    }
    const modal = document.getElementById('elExampleModal');
    if (modal) {
        modal.remove();
    }
}

/**
 * 简单的Markdown渲染（支持常用语法）
 */
function renderMarkdown(markdown) {
    if (!markdown) return '';
    
    let html = markdown;
    
    // 代码块（```code```）
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, '<pre><code class="language-$1">$2</code></pre>');
    
    // 行内代码（`code`）
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    // 标题
    html = html.replace(/^### (.+)$/gm, '<h4>$1</h4>');
    html = html.replace(/^## (.+)$/gm, '<h3>$1</h3>');
    html = html.replace(/^# (.+)$/gm, '<h2>$1</h2>');
    
    // 粗体和斜体
    html = html.replace(/\*\*\*(.+?)\*\*\*/g, '<strong><em>$1</em></strong>');
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*(.+?)\*/g, '<em>$1</em>');
    
    // 无序列表
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*<\/li>\n?)+/g, '<ul>$&</ul>');
    
    // 有序列表
    html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');
    
    // 段落（空行分隔）
    html = html.replace(/\n\n/g, '</p><p>');
    html = '<p>' + html + '</p>';
    
    // 清理空段落
    html = html.replace(/<p>\s*<\/p>/g, '');
    html = html.replace(/<p>\s*<\/code>/g, '</code>');
    html = html.replace(/<code>\s*<\/p>/g, '<code>');
    
    return html;
}