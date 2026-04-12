/**
 * Java All Call Graph Server 前端应用 - 核心模块
 * 包含：全局变量、工具函数、Toast通知、错误处理、初始化
 */

// API 基础路径
const API_BASE = '/api/v1';

// 当前状态
let currentProject = null;
let currentTemplate = null;
let configDefinitions = null;          // 项目配置定义
let templateConfigDefinitions = null;  // 模板配置定义
let elFixedMenu = null;                // EL表达式固定菜单

// Toast 通知计数器
let toastCounter = 0;

/**
 * 显示 Toast 通知
 * @param {string} message - 通知消息
 * @param {string} type - 通知类型：'success', 'error', 'warning', 'info'
 * @param {number} duration - 显示时长（毫秒），默认5000
 */
function showToast(message, type = 'info', duration = 5000) {
    const container = document.getElementById('toastContainer');
    
    // 创建 Toast 元素
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.id = `toast-${++toastCounter}`;
    
    // 根据类型选择图标
    let icon = '';
    switch (type) {
        case 'success':
            icon = '✓';
            break;
        case 'error':
            icon = '✕';
            break;
        case 'warning':
            icon = '⚠';
            break;
        default:
            icon = 'ℹ';
    }
    
    toast.innerHTML = `
        <span class="toast-icon">${icon}</span>
        <span class="toast-message">${escapeHtml(message)}</span>
        <button class="toast-close" onclick="closeToast('${toast.id}')">&times;</button>
    `;
    
    container.appendChild(toast);
    
    // 触发动画
    setTimeout(() => toast.classList.add('toast-show'), 10);
    
    // 自动关闭
    setTimeout(() => closeToast(toast.id), duration);
}

/**
 * 关闭 Toast 通知
 * @param {string} toastId - Toast 元素ID
 */
function closeToast(toastId) {
    const toast = document.getElementById(toastId);
    if (toast) {
        toast.classList.remove('toast-show');
        toast.classList.add('toast-hide');
        setTimeout(() => toast.remove(), 300);
    }
}

/**
 * 处理API响应错误，检查是否为表达式配置检查错误
 * @param {object} result - API响应结果
 * @returns {boolean} - 是否已处理错误（true表示是表达式配置错误并已显示弹窗）
 */
function handleApiError(result) {
    if (result.code === 400 && result.data && result.data.elConfigEnumName) {
        // 表达式配置检查失败，显示详细错误弹窗
        showElConfigErrorModal(result.data);
        return true;
    }
    return false;
}

/**
 * 显示表达式配置检查错误弹窗（不自动消失）
 * @param {object} errorData - 错误详细数据
 */
function showElConfigErrorModal(errorData) {
    // 使用HTML中预定义的弹窗
    document.getElementById('elErrorConfigSource').textContent = errorData.configSource || '-';
    document.getElementById('elErrorConfigEnumName').textContent = errorData.elConfigEnumName || '-';
    document.getElementById('elErrorConfigFileName').textContent = errorData.configFileName || '-';
    document.getElementById('elErrorConfigDescription').textContent = errorData.configDescription || '-';
    document.getElementById('elErrorElText').textContent = errorData.elText || '-';
    document.getElementById('elErrorErrorMessage').textContent = errorData.errorMessage || '-';
    
    document.getElementById('elConfigErrorModal').classList.remove('hidden');
}

/**
 * 关闭表达式配置检查错误弹窗
 */
function closeElConfigErrorModal() {
    document.getElementById('elConfigErrorModal').classList.add('hidden');
}

/**
 * HTML转义
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML.replace(/'/g, "\\'").replace(/"/g, '\\"');
}

/**
 * 格式化执行耗时为可读字符串
 */
function formatDuration(durationMs) {
    const seconds = Math.floor(durationMs / 1000);
    if (seconds < 60) {
        return seconds + '秒';
    }
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    if (remainingSeconds === 0) {
        return minutes + '分钟';
    }
    return minutes + '分' + remainingSeconds + '秒';
}

/**
 * 格式化日期时间
 */
function formatDateTime(timestamp) {
    if (!timestamp) return '-';
    const date = new Date(timestamp);
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
}

/**
 * 获取状态文本
 */
function getStatusText(status) {
    const statusMap = {
        'pending': '等待中',
        'running': '执行中',
        'completed': '已完成',
        'failed': '失败'
    };
    return statusMap[status] || status;
}

/**
 * 加载输出根目录
 */
async function loadOutputRootPath() {
    try {
        const response = await fetch(`${API_BASE}/system/output-root-path`);
        const result = await response.json();
        if (result.code === 200) {
            document.getElementById('outputRootPath').textContent = result.data.outputRootPath;
        }
    } catch (error) {
        console.error('加载输出根目录失败', error);
    }
}

/**
 * 打开目录（内部方法，仅用于后台已验证的路径）
 * @param {string} directoryPath - 目录路径
 * @deprecated 请使用 openAppRootDirectory、openProjectLogDirectory、openExecutionOutputDirectory 等安全方法
 */
async function openDirectory(directoryPath) {
    try {
        const response = await fetch(`${API_BASE}/system/open-directory`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ directoryPath: directoryPath })
        });
        const result = await response.json();
        if (result.code === 200) {
            showToast('目录已打开', 'success');
        } else {
            showToast(result.message || '打开目录失败', 'error');
        }
    } catch (error) {
        console.error('打开目录失败', error);
        showToast('打开目录失败', 'error');
    }
}

/**
 * 打开应用根目录（安全方式，后端处理路径）
 */
async function openAppRootDirectory() {
    try {
        const response = await fetch(`${API_BASE}/system/open-app-directory`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            }
        });
        const result = await response.json();
        if (result.code === 200) {
            showToast('目录已打开', 'success');
        } else {
            showToast(result.message || '打开目录失败', 'error');
        }
    } catch (error) {
        console.error('打开应用根目录失败', error);
        showToast('打开应用根目录失败', 'error');
    }
}

/**
 * 打开当前项目的日志目录（安全方式，后端处理路径）
 */
async function openProjectLogDirectory() {
    if (!currentProject) {
        showToast('请先选择项目', 'warning');
        return;
    }
    
    try {
        const response = await fetch(`${API_BASE}/system/open-project-log-directory`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ projectId: currentProject.projectId })
        });
        const result = await response.json();
        if (result.code === 200) {
            showToast('目录已打开', 'success');
        } else {
            showToast(result.message || '打开目录失败', 'error');
        }
    } catch (error) {
        console.error('打开项目日志目录失败', error);
        showToast('打开项目日志目录失败', 'error');
    }
}

/**
 * 加载配置参数定义（项目配置和模板配置）
 */
async function loadConfigDefinitions() {
    try {
        // 并行加载项目配置定义和模板配置定义
        const [projectResponse, templateResponse] = await Promise.all([
            fetch(`${API_BASE}/config/definitions/project`),
            fetch(`${API_BASE}/config/definitions/template`)
        ]);
        
        const projectResult = await projectResponse.json();
        const templateResult = await templateResponse.json();
        
        if (projectResult.code === 200) {
            configDefinitions = projectResult.data;
        }
        if (templateResult.code === 200) {
            templateConfigDefinitions = templateResult.data;
        }
    } catch (error) {
        console.error('加载配置参数定义失败', error);
    }
}

// 页面加载完成后初始化
document.addEventListener('DOMContentLoaded', function() {
    loadOutputRootPath();
    loadProjects();
    loadConfigDefinitions();
    loadElFixedMenu();
    initElContextMenu();
});