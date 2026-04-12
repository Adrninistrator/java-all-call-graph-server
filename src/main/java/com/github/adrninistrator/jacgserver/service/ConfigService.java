package com.github.adrninistrator.jacgserver.service;

import com.github.adrninistrator.jacgserver.enums.ConfigSceneEnum;
import com.github.adrninistrator.jacgserver.model.vo.ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.ElMenuCategoryVO;
import com.github.adrninistrator.jacgserver.model.vo.ElMenuItemVO;

import java.util.List;
import java.util.Map;

/**
 * 配置服务接口
 *
 * @author adrninistrator
 * @since 1.0.0
 */
public interface ConfigService {

    /**
     * 获取配置参数定义（项目配置场景）
     *
     * @return 配置参数定义
     */
    ConfigDefinitionVO getConfigDefinitions();

    /**
     * 获取配置参数定义（指定场景）
     *
     * @param scene 配置场景（PROJECT/TEMPLATE）
     * @return 配置参数定义
     */
    ConfigDefinitionVO getConfigDefinitions(ConfigSceneEnum scene);

    /**
     * 获取输出根目录
     *
     * @return 输出根目录
     */
    String getOutputRootPath();

    /**
     * 获取项目配置目录路径
     *
     * @return 项目配置目录路径
     */
    String getProjectConfDir();

    /**
     * 获取EL表达式配置示例文件内容
     *
     * @param type 配置类型（javacg2/jacg）
     * @param enumName 枚举名称
     * @return 示例文件内容，如果不存在返回null
     */
    String getElExampleContent(String type, String enumName);

    /**
     * 获取EL表达式变量菜单
     *
     * @param type 配置类型（javacg2/jacg）
     * @param enumName 枚举名称
     * @return 变量菜单项列表
     */
    List<ElMenuItemVO> getElVariableMenu(String type, String enumName);

    /**
     * 获取EL表达式固定菜单
     *
     * @return 固定菜单分类列表
     */
    List<ElMenuCategoryVO> getElFixedMenu();

    /**
     * 获取指定配置参数的描述信息
     *
     * @param configType 配置类型（javacg2-list/javacg2-set/jacg-list/jacg-set）
     * @param enumName   枚举名称
     * @return 描述信息数组，如果不存在返回null
     */
    List<String> getConfigDescription(String configType, String enumName);

    /**
     * 获取指定配置参数的详细信息（包括参数名称、配置文件名和描述）
     *
     * @param configType 配置类型（javacg2-list/javacg2-set/jacg-list/jacg-set）
     * @param enumName   枚举名称
     * @return 包含 paramName、fileName、descriptions 的Map，如果不存在返回null
     */
    Map<String, Object> getConfigDetail(String configType, String enumName);

    /**
     * 根据方向获取入口类/方法配置参数的详细信息
     *
     * @param direction 调用链方向（caller-向下/callee-向上）
     * @return 包含 paramName、fileName、descriptions 的Map
     */
    Map<String, Object> getEntryPointConfigDetail(String direction);

    /**
     * 根据方向获取关键字配置参数的详细信息
     *
     * @param direction 调用链方向（caller-向下/callee-向上）
     * @return 包含 paramName、fileName、descriptions 的Map
     */
    Map<String, Object> getFindStackKeywordConfigDetail(String direction);
}
