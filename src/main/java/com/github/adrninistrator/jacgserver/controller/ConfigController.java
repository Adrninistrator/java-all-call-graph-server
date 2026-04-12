package com.github.adrninistrator.jacgserver.controller;

import com.github.adrninistrator.jacgserver.enums.ConfigSceneEnum;
import com.github.adrninistrator.jacgserver.model.vo.ConfigDefinitionVO;
import com.github.adrninistrator.jacgserver.model.vo.ElMenuCategoryVO;
import com.github.adrninistrator.jacgserver.model.vo.ElMenuItemVO;
import com.github.adrninistrator.jacgserver.model.vo.ResponseResult;
import com.github.adrninistrator.jacgserver.service.ConfigService;
import com.github.adrninistrator.jacgserver.util.ResponseUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配置控制器
 *
 * @author adrninistrator
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/v1/config")
public class ConfigController {

    @Autowired
    private ConfigService configService;

    /**
     * 获取配置参数定义（项目配置场景）
     */
    @GetMapping("/definitions")
    public ResponseResult getConfigDefinitions() {
        return getConfigDefinitionsByScene(ConfigSceneEnum.PROJECT.getCode());
    }

    /**
     * 获取配置参数定义（指定场景）
     * 
     * @param scene 场景代码（project/template）
     */
    @GetMapping("/definitions/{scene}")
    public ResponseResult getConfigDefinitionsByScene(@PathVariable String scene) {
        ConfigSceneEnum sceneEnum = ConfigSceneEnum.fromCode(scene);
        if (sceneEnum == null) {
            return ResponseUtil.error(400, "无效的场景参数: " + scene);
        }
        ConfigDefinitionVO definitions = configService.getConfigDefinitions(sceneEnum);
        return ResponseUtil.success(definitions);
    }

    /**
     * 获取EL表达式配置示例文件内容
     * 
     * @param type 配置类型（javacg2/jacg）
     * @param enumName 枚举名称
     */
    @GetMapping("/el-example/{type}/{enumName}")
    public ResponseResult getElExampleContent(@PathVariable String type, @PathVariable String enumName) {
        String content = configService.getElExampleContent(type, enumName);
        if (content == null) {
            return ResponseUtil.error(404, "未找到示例文件: " + type + "/" + enumName);
        }
        Map<String, String> result = new HashMap<>();
        result.put("content", content);
        return ResponseUtil.success(result);
    }

    /**
     * 获取EL表达式变量菜单
     * 
     * @param type 配置类型（javacg2/jacg）
     * @param enumName 枚举名称
     */
    @GetMapping("/el-menu/variables/{type}/{enumName}")
    public ResponseResult getElVariableMenu(@PathVariable String type, @PathVariable String enumName) {
        List<ElMenuItemVO> variables = configService.getElVariableMenu(type, enumName);
        Map<String, List<ElMenuItemVO>> result = new HashMap<>();
        result.put("variables", variables);
        return ResponseUtil.success(result);
    }

    /**
     * 获取EL表达式固定菜单
     */
    @GetMapping("/el-menu/fixed")
    public ResponseResult getElFixedMenu() {
        List<ElMenuCategoryVO> categories = configService.getElFixedMenu();
        Map<String, List<ElMenuCategoryVO>> result = new HashMap<>();
        result.put("categories", categories);
        return ResponseUtil.success(result);
    }

    /**
     * 获取指定配置参数的描述信息
     * 
     * @param configType 配置类型（javacg2-list/javacg2-set/jacg-list/jacg-set）
     * @param enumName   枚举名称
     */
    @GetMapping("/description/{configType}/{enumName}")
    public ResponseResult getConfigDescription(@PathVariable String configType, @PathVariable String enumName) {
        Map<String, Object> detail = configService.getConfigDetail(configType, enumName);
        if (detail == null) {
            return ResponseUtil.error(404, "未找到配置参数: " + configType + "/" + enumName);
        }
        return ResponseUtil.success(detail);
    }

    /**
     * 根据方向获取入口类/方法配置参数的描述信息
     * 
     * @param direction 调用链方向（caller-向下/callee-向上）
     */
    @GetMapping("/description/entry-point/{direction}")
    public ResponseResult getEntryPointDescription(@PathVariable String direction) {
        Map<String, Object> detail = configService.getEntryPointConfigDetail(direction);
        return ResponseUtil.success(detail);
    }

    /**
     * 根据方向获取关键字配置参数的描述信息
     * 
     * @param direction 调用链方向（caller-向下/callee-向上）
     */
    @GetMapping("/description/find-stack-keyword/{direction}")
    public ResponseResult getFindStackKeywordDescription(@PathVariable String direction) {
        Map<String, Object> detail = configService.getFindStackKeywordConfigDetail(direction);
        return ResponseUtil.success(detail);
    }
}
