/**
 * Java All Call Graph Server 前端应用 - 常量定义
 *
 * 定义与后端枚举字段名对应的常量，避免在前端代码中硬编码枚举字段名字符串。
 * 后端使用枚举的name()方法获取字段名作为Map的key，前端使用这些常量访问对应的key。
 * 当后端枚举字段名变更时，只需修改此文件中的常量值即可。
 */

// java-callgraph2 枚举常量
const Javacg2Enum = {
    // JavaCG2OtherConfigFileUseListEnum
    OCFULE_JAR_DIR: 'OCFULE_JAR_DIR'
};

// java-all-call-graph 枚举常量
const JacgEnum = {
    // OtherConfigFileUseSetEnum
    OCFUSE_METHOD_CLASS_4CALLER: 'OCFUSE_METHOD_CLASS_4CALLER',
    OCFUSE_METHOD_CLASS_4CALLEE: 'OCFUSE_METHOD_CLASS_4CALLEE',

    // OtherConfigFileUseListEnum
    OCFULE_FIND_STACK_KEYWORD_4ER: 'OCFULE_FIND_STACK_KEYWORD_4ER',
    OCFULE_FIND_STACK_KEYWORD_4EE: 'OCFULE_FIND_STACK_KEYWORD_4EE',

    // ConfigKeyEnum
    CKE_APP_NAME: 'CKE_APP_NAME'
};
