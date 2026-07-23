/*
  星联智服全渠道客服平台上线与运营管理手册.md
  XX-200智能网关产品手册.pdf
  知识路由初始化脚本

  使用顺序：
  1. 先在管理台上传这两份文档。
  2. 进入每份文档详情页，完成“确认策略方案”和“构建索引执行”。
  3. 回到文档列表或数据库中拿到两份文档的 document_id。
  4. 替换下面两个变量 @doc_customer_service_id / @doc_xx200_id。
  5. 执行本脚本。

  注意：
  - 本脚本会更新 nexus_agent_document 的知识域编码、名称、业务分类和标签。
  - 本脚本会写入知识范围、知识主题、文档画像、主题文档关联。
  - 本脚本使用 INSERT ... ON DUPLICATE KEY UPDATE，可重复执行。
  - 本脚本不会修改文档解析、策略方案、索引状态、chunk、向量库数据。
*/

START TRANSACTION;

/* =========================================================
   0. 请先替换这里的两个文档 ID
   ========================================================= */

SET @doc_customer_service_id = 0; -- TODO: 替换为“星联智服全渠道客服平台上线与运营管理手册.md”的 document_id
SET @doc_xx200_id = 0;           -- TODO: 替换为“XX-200智能网关产品手册.pdf”的 document_id

/*
  如果你不确定文档 ID，可以先执行下面的查询，再把查出来的 id 填到上面变量里：

  SELECT id, document_name, original_file_name, index_status, last_index_task_id
  FROM nexus_agent_document
  WHERE status = 1
    AND (
      document_name LIKE '%星联智服%'
      OR original_file_name LIKE '%星联智服%'
      OR document_name LIKE '%XX-200%'
      OR original_file_name LIKE '%XX-200%'
    )
  ORDER BY create_time DESC;
*/

/* =========================================================
   1. 固定配置编码
   ========================================================= */

SET @scope_customer_service_code = 'customer_service_platform_ops';
SET @scope_customer_service_name = '客服平台上线运营';
SET @scope_xx200_code = 'xx200_gateway_product';
SET @scope_xx200_name = 'XX-200智能网关';

/*
  这些 id 只用于新插入范围、主题、关系、画像时。
  如果你的数据库里极端情况下已经占用了这些 id，可以把 @base_id 改成其他未使用的大整数。
*/
SET @base_id = 8800041600000000000;

/* =========================================================
   2. 更新两份文档主表元数据
   ========================================================= */

UPDATE nexus_agent_document
SET
    document_name = '星联智服全渠道客服平台上线与运营管理手册',
    knowledge_scope_code = @scope_customer_service_code,
    knowledge_scope_name = @scope_customer_service_name,
    business_category = '平台运营手册',
    document_tags = '客服平台,上线,灰度发布,生产发布,回滚,知识治理,RAG,值班,故障应急,质量评估',
    edit_time = NOW()
WHERE id = @doc_customer_service_id
  AND status = 1;

UPDATE nexus_agent_document
SET
    document_name = 'XX-200智能网关产品技术手册',
    knowledge_scope_code = @scope_xx200_code,
    knowledge_scope_name = @scope_xx200_name,
    business_category = '产品技术手册',
    document_tags = 'XX-200,智能网关,边缘计算,安装部署,网络配置,协议配置,Modbus,日志查看,故障排查,工业物联网',
    edit_time = NOW()
WHERE id = @doc_xx200_id
  AND status = 1;

/* =========================================================
   3. 知识范围配置
   ========================================================= */

INSERT INTO nexus_agent_knowledge_scope_node (
    id, scope_code, scope_name, parent_scope_code, description, aliases, examples, sort_order,
    create_time, edit_time, status
)
VALUES
(
    @base_id + 1,
    @scope_customer_service_code,
    @scope_customer_service_name,
    NULL,
    '用于承接客服平台项目上线、知识治理、灰度发布、生产发布、值班观察、故障应急、质量评估等运营管理类问题。',
    '客服平台,全渠道客服平台,星联智服,上线运营,知识治理,灰度发布',
    '["平台上线总流程有哪几个阶段","灰度验证期间要看哪些指标","什么时候需要回滚","上线后要观察多久"]',
    10,
    NOW(), NOW(), 1
),
(
    @base_id + 2,
    @scope_xx200_code,
    @scope_xx200_name,
    NULL,
    '用于承接XX-200智能网关的产品规格、安装部署、网络配置、协议配置、日志查看与故障排查等产品技术问题。',
    'XX-200,智能网关,工业网关,边缘网关,网关产品,安装部署',
    '["XX-200支持哪些协议","XX-200怎么安装部署","默认登录地址和账号是什么","双WAN怎么配置","故障日志在哪里看"]',
    20,
    NOW(), NOW(), 1
)
ON DUPLICATE KEY UPDATE
    scope_name = VALUES(scope_name),
    parent_scope_code = VALUES(parent_scope_code),
    description = VALUES(description),
    aliases = VALUES(aliases),
    examples = VALUES(examples),
    sort_order = VALUES(sort_order),
    edit_time = NOW(),
    status = 1;

/* =========================================================
   4. 知识主题配置
   answer_shape 固定为 list / explain / steps
   execution_preference 固定为 retrieval / graph_assist
   ========================================================= */

INSERT INTO nexus_agent_knowledge_topic_node (
    id, topic_code, topic_name, scope_code, description, aliases, examples,
    answer_shape, execution_preference, sort_order,
    create_time, edit_time, status
)
VALUES
(
    @base_id + 101,
    'platform_go_live_process',
    '平台上线总流程',
    @scope_customer_service_code,
    '回答客服平台项目上线从立项、知识治理、灰度验证到生产发布的整体流程。',
    '上线流程,上线步骤,项目上线,上线里程碑',
    '["平台上线总流程有哪几个阶段","项目上线要经过哪些里程碑"]',
    'steps',
    'retrieval',
    10,
    NOW(), NOW(), 1
),
(
    @base_id + 102,
    'knowledge_governance',
    '知识采集与治理',
    @scope_customer_service_code,
    '回答知识来源分类、知识接入前检查、知识域划分和不适合接入知识库的内容。',
    '知识治理,知识接入,知识域,知识库治理',
    '["知识接入前要检查什么","知识域应该怎么划分","哪些内容不适合接入机器人知识库"]',
    'list',
    'retrieval',
    20,
    NOW(), NOW(), 1
),
(
    @base_id + 103,
    'gray_release_and_rollback',
    '灰度验证与回滚',
    @scope_customer_service_code,
    '回答灰度范围、灰度期指标、禁止事项，以及哪些情况要触发回滚评估。',
    '灰度验证,灰度发布,回滚,回滚条件,灰度指标',
    '["灰度期间必须看哪些指标","什么时候需要回滚","灰度期间有哪些禁止事项"]',
    'list',
    'retrieval',
    30,
    NOW(), NOW(), 1
),
(
    @base_id + 104,
    'post_launch_observation',
    '上线观察与值班',
    @scope_customer_service_code,
    '回答普通版本和高风险版本的观察时长、值班安排和观察日报要求。',
    '上线观察,观察时长,值班规则,观察日报',
    '["上线后要观察多久","值班安排怎么配","观察日报至少写什么"]',
    'list',
    'retrieval',
    40,
    NOW(), NOW(), 1
),
(
    @base_id + 105,
    'fault_response',
    '典型故障处理',
    @scope_customer_service_code,
    '回答平台在检索命中率下降、回答不完整、转人工率升高等场景下的处理方法。',
    '故障处理,检索命中率下降,人工转接率异常,回答口径不完整',
    '["检索命中率突然下降怎么排查","回答口径不完整怎么办","人工转接率异常升高怎么查"]',
    'steps',
    'retrieval',
    50,
    NOW(), NOW(), 1
),
(
    @base_id + 106,
    'quality_evaluation',
    '运营质量评估',
    @scope_customer_service_code,
    '回答质量评估层次、指标定义和每周每月每季度的评审节奏。',
    '质量评估,质量指标,运营指标,复盘节奏',
    '["运营质量指标怎么分层","质量评审节奏是怎样的","常见质量指标有哪些"]',
    'list',
    'retrieval',
    60,
    NOW(), NOW(), 1
),
(
    @base_id + 201,
    'product_overview_spec',
    '产品概述与技术规格',
    @scope_xx200_code,
    '回答XX-200的产品简介、核心特性、处理器、内存、网络接口、串口、电源和工作环境等规格。',
    '产品概述,技术规格,核心特性,参数规格',
    '["XX-200有哪些核心特性","XX-200的技术规格是什么","支持哪些协议和接口"]',
    'list',
    'retrieval',
    10,
    NOW(), NOW(), 1
),
(
    @base_id + 202,
    'installation_deployment',
    '安装部署',
    @scope_xx200_code,
    '回答安装前准备、DIN导轨安装、接电、连线和浏览器访问管理界面等步骤。',
    '安装部署,安装前准备,硬件安装,上电安装',
    '["安装前要准备什么","XX-200怎么安装","上电部署步骤是什么"]',
    'steps',
    'retrieval',
    20,
    NOW(), NOW(), 1
),
(
    @base_id + 203,
    'initial_access_login',
    '初始访问与首次登录',
    @scope_xx200_code,
    '回答LAN1默认IP、浏览器访问地址、默认账号密码和首次登录改密要求。',
    '默认IP,默认账号,首次登录,初始配置',
    '["默认登录地址是什么","默认账号密码是什么","首次登录后密码要求是什么"]',
    'steps',
    'retrieval',
    30,
    NOW(), NOW(), 1
),
(
    @base_id + 204,
    'network_configuration',
    '网络配置',
    @scope_xx200_code,
    '回答LAN/WAN使用方式、双WAN负载均衡示例、DNS、健康检查和故障切换。',
    '网络配置,双WAN,静态IP,DHCP,PPPoE,负载均衡',
    '["双WAN负载均衡怎么配置","WAN支持哪些接入方式","健康检查怎么设置"]',
    'steps',
    'retrieval',
    40,
    NOW(), NOW(), 1
),
(
    @base_id + 205,
    'protocol_configuration',
    '协议配置',
    @scope_xx200_code,
    '回答Modbus RTU采集的串口配置、设备模板、点位定义和采集周期建议。',
    '协议配置,Modbus,RS-485,设备模板,采集点位',
    '["Modbus RTU怎么配置","温湿度传感器点位怎么建","采集周期建议多少"]',
    'steps',
    'retrieval',
    50,
    NOW(), NOW(), 1
),
(
    @base_id + 206,
    'troubleshooting_and_logs',
    '故障排查与日志查看',
    @scope_xx200_code,
    '回答常见故障现象、可能原因、解决方案，以及系统日志、应用日志、审计日志和导出诊断包。',
    '故障排查,日志查看,诊断包,系统日志,应用日志,审计日志',
    '["PWR灯不亮怎么处理","无法访问管理界面怎么排查","日志在哪里看","怎么导出诊断包"]',
    'steps',
    'retrieval',
    60,
    NOW(), NOW(), 1
)
ON DUPLICATE KEY UPDATE
    topic_name = VALUES(topic_name),
    scope_code = VALUES(scope_code),
    description = VALUES(description),
    aliases = VALUES(aliases),
    examples = VALUES(examples),
    answer_shape = VALUES(answer_shape),
    execution_preference = VALUES(execution_preference),
    sort_order = VALUES(sort_order),
    edit_time = NOW(),
    status = 1;

/* =========================================================
   5. 文档画像配置
   说明：如果系统已自动生成画像，这里会覆盖为更适合演示的手工画像。
   ========================================================= */

INSERT INTO nexus_agent_document_profile (
    id, document_id, profile_version, document_summary, document_type, core_topics, example_questions,
    graph_friendly, supports_graph_outline, supports_item_lookup, supports_graph_assist,
    profile_source, profile_status, error_msg,
    create_time, edit_time, status
)
VALUES
(
    @base_id + 301,
    @doc_customer_service_id,
    1,
    '本手册用于规范星联智服全渠道客服平台从需求澄清、知识治理、机器人策略设计、灰度验证、生产发布、上线观察、故障应急到运营质量评估的全链路管理要求。',
    'manual',
    '["平台上线总流程","知识采集与治理","灰度验证与回滚","上线观察与值班","典型故障处理","运营质量评估"]',
    '["平台上线总流程有哪几个阶段","灰度验证期间必须看哪些指标","什么时候需要触发回滚评估","上线后要观察多久","检索命中率突然下降怎么排查"]',
    1, 1, 1, 1,
    'manual',
    2,
    NULL,
    NOW(), NOW(), 1
),
(
    @base_id + 302,
    @doc_xx200_id,
    1,
    '本手册介绍XX-200智能网关的产品概述、核心特性、技术规格、安装部署、初始访问、网络配置、协议配置、常见故障排查和日志查看方式。',
    'manual',
    '["产品概述与技术规格","安装部署","初始访问与首次登录","网络配置","协议配置","故障排查与日志查看"]',
    '["XX-200支持哪些工业协议","默认登录地址和账号密码是什么","安装前要准备什么","双WAN负载均衡怎么配置","Modbus RTU怎么配置","日志在哪里看"]',
    1, 1, 1, 1,
    'manual',
    2,
    NULL,
    NOW(), NOW(), 1
)
ON DUPLICATE KEY UPDATE
    profile_version = COALESCE(profile_version, 0) + 1,
    document_summary = VALUES(document_summary),
    document_type = VALUES(document_type),
    core_topics = VALUES(core_topics),
    example_questions = VALUES(example_questions),
    graph_friendly = VALUES(graph_friendly),
    supports_graph_outline = VALUES(supports_graph_outline),
    supports_item_lookup = VALUES(supports_item_lookup),
    supports_graph_assist = VALUES(supports_graph_assist),
    profile_source = VALUES(profile_source),
    profile_status = VALUES(profile_status),
    error_msg = VALUES(error_msg),
    edit_time = NOW(),
    status = 1;

/* =========================================================
   6. 主题文档关联配置
   说明：先清理这些主题下的旧跨文档关联，再写入目标关联。
   ========================================================= */

UPDATE nexus_agent_topic_document_relation
SET status = 0, edit_time = NOW()
WHERE topic_code IN (
    'platform_go_live_process',
    'knowledge_governance',
    'gray_release_and_rollback',
    'post_launch_observation',
    'fault_response',
    'quality_evaluation'
)
AND document_id <> @doc_customer_service_id;

UPDATE nexus_agent_topic_document_relation
SET status = 0, edit_time = NOW()
WHERE topic_code IN (
    'product_overview_spec',
    'installation_deployment',
    'initial_access_login',
    'network_configuration',
    'protocol_configuration',
    'troubleshooting_and_logs'
)
AND document_id <> @doc_xx200_id;

INSERT INTO nexus_agent_topic_document_relation (
    id, topic_code, document_id, relation_score, relation_source, reason,
    create_time, edit_time, status
)
VALUES
(
    @base_id + 401,
    'platform_go_live_process',
    @doc_customer_service_id,
    0.9800,
    'manual',
    '该手册完整描述了客服平台从立项到上线观察的全流程。',
    NOW(), NOW(), 1
),
(
    @base_id + 402,
    'knowledge_governance',
    @doc_customer_service_id,
    0.9700,
    'manual',
    '该手册包含知识来源分类、知识接入检查和知识域划分建议。',
    NOW(), NOW(), 1
),
(
    @base_id + 403,
    'gray_release_and_rollback',
    @doc_customer_service_id,
    0.9800,
    'manual',
    '该手册明确给出了灰度范围、灰度指标、禁止事项和回滚触发条件。',
    NOW(), NOW(), 1
),
(
    @base_id + 404,
    'post_launch_observation',
    @doc_customer_service_id,
    0.9700,
    'manual',
    '该手册包含上线观察时长、值班安排和观察日报模板。',
    NOW(), NOW(), 1
),
(
    @base_id + 405,
    'fault_response',
    @doc_customer_service_id,
    0.9600,
    'manual',
    '该手册给出了检索命中率下降、回答不完整和人工转接异常的处理方法。',
    NOW(), NOW(), 1
),
(
    @base_id + 406,
    'quality_evaluation',
    @doc_customer_service_id,
    0.9500,
    'manual',
    '该手册明确了质量评估层次、指标定义和评审节奏。',
    NOW(), NOW(), 1
),
(
    @base_id + 501,
    'product_overview_spec',
    @doc_xx200_id,
    0.9800,
    'manual',
    '该手册集中描述了XX-200的产品简介、核心特性和完整技术规格。',
    NOW(), NOW(), 1
),
(
    @base_id + 502,
    'installation_deployment',
    @doc_xx200_id,
    0.9800,
    'manual',
    '该手册包含安装前准备、硬件安装和上电部署步骤。',
    NOW(), NOW(), 1
),
(
    @base_id + 503,
    'initial_access_login',
    @doc_xx200_id,
    0.9700,
    'manual',
    '该手册明确给出了默认IP、访问地址、默认账号密码和首次登录改密要求。',
    NOW(), NOW(), 1
),
(
    @base_id + 504,
    'network_configuration',
    @doc_xx200_id,
    0.9800,
    'manual',
    '该手册包含双WAN、静态IP、DHCP、DNS、健康检查和故障切换配置。',
    NOW(), NOW(), 1
),
(
    @base_id + 505,
    'protocol_configuration',
    @doc_xx200_id,
    0.9700,
    'manual',
    '该手册包含Modbus RTU串口配置、设备模板和点位定义。',
    NOW(), NOW(), 1
),
(
    @base_id + 506,
    'troubleshooting_and_logs',
    @doc_xx200_id,
    0.9800,
    'manual',
    '该手册包含常见故障现象、可能原因、解决方案和日志查看方式。',
    NOW(), NOW(), 1
)
ON DUPLICATE KEY UPDATE
    relation_score = VALUES(relation_score),
    relation_source = VALUES(relation_source),
    reason = VALUES(reason),
    edit_time = NOW(),
    status = 1;

/* =========================================================
   7. 执行后检查
   ========================================================= */

SELECT
    id,
    document_name,
    knowledge_scope_code,
    knowledge_scope_name,
    business_category,
    document_tags,
    index_status,
    last_index_task_id
FROM nexus_agent_document
WHERE id IN (@doc_customer_service_id, @doc_xx200_id);

SELECT
    scope_code,
    scope_name,
    aliases,
    sort_order,
    status
FROM nexus_agent_knowledge_scope_node
WHERE scope_code IN (@scope_customer_service_code, @scope_xx200_code)
ORDER BY sort_order;

SELECT
    topic_code,
    topic_name,
    scope_code,
    answer_shape,
    execution_preference,
    sort_order,
    status
FROM nexus_agent_knowledge_topic_node
WHERE scope_code IN (@scope_customer_service_code, @scope_xx200_code)
ORDER BY scope_code, sort_order;

SELECT
    r.topic_code,
    t.topic_name,
    r.document_id,
    d.document_name,
    r.relation_score,
    r.relation_source,
    r.reason,
    r.status
FROM nexus_agent_topic_document_relation r
LEFT JOIN nexus_agent_knowledge_topic_node t ON t.topic_code = r.topic_code
LEFT JOIN nexus_agent_document d ON d.id = r.document_id
WHERE r.topic_code IN (
    'platform_go_live_process',
    'knowledge_governance',
    'gray_release_and_rollback',
    'post_launch_observation',
    'fault_response',
    'quality_evaluation',
    'product_overview_spec',
    'installation_deployment',
    'initial_access_login',
    'network_configuration',
    'protocol_configuration',
    'troubleshooting_and_logs'
)
ORDER BY t.scope_code, t.sort_order, r.relation_score DESC;

COMMIT;
