-- 建议先手动创建 PostgreSQL 数据库：
-- CREATE DATABASE nexus_agent_pgvector ENCODING 'UTF8';
-- 然后连接到 nexus_agent_pgvector 库后执行本文件。

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE IF NOT EXISTS public.nexus_agent_document_embedding (
    id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    task_id BIGINT NOT NULL,
    plan_id BIGINT,
    parent_block_id BIGINT NOT NULL,
    chunk_no INTEGER NOT NULL,
    source_type SMALLINT DEFAULT 1,
    section_path VARCHAR(1000),
    structure_node_id BIGINT,
    structure_node_type SMALLINT,
    canonical_path VARCHAR(1000),
    item_index INTEGER,
    chunk_text TEXT NOT NULL,
    char_count INTEGER DEFAULT 0,
    token_count INTEGER DEFAULT 0,
    embedding_model VARCHAR(128),
    metadata_json JSONB DEFAULT '{}'::jsonb,
    embedding VECTOR NOT NULL,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    edit_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    status SMALLINT DEFAULT 1,
    PRIMARY KEY (id)
);

COMMENT ON TABLE public.nexus_agent_document_embedding IS '文档切块向量表';
COMMENT ON COLUMN public.nexus_agent_document_embedding.id IS '主键id，直接复用 MySQL chunk 主键';
COMMENT ON COLUMN public.nexus_agent_document_embedding.document_id IS '文档id';
COMMENT ON COLUMN public.nexus_agent_document_embedding.task_id IS '索引任务id';
COMMENT ON COLUMN public.nexus_agent_document_embedding.plan_id IS '策略方案id';
COMMENT ON COLUMN public.nexus_agent_document_embedding.parent_block_id IS '所属父块id';
COMMENT ON COLUMN public.nexus_agent_document_embedding.chunk_no IS '切块序号';
COMMENT ON COLUMN public.nexus_agent_document_embedding.source_type IS '内容来源 1:原文切块 2:后处理补全文本';
COMMENT ON COLUMN public.nexus_agent_document_embedding.section_path IS '章节路径';
COMMENT ON COLUMN public.nexus_agent_document_embedding.structure_node_id IS '关联的结构节点id';
COMMENT ON COLUMN public.nexus_agent_document_embedding.structure_node_type IS '关联的结构节点类型';
COMMENT ON COLUMN public.nexus_agent_document_embedding.canonical_path IS '结构节点稳定路径';
COMMENT ON COLUMN public.nexus_agent_document_embedding.item_index IS '列表项/步骤项序号';
COMMENT ON COLUMN public.nexus_agent_document_embedding.chunk_text IS '切块文本内容';
COMMENT ON COLUMN public.nexus_agent_document_embedding.char_count IS '字符数';
COMMENT ON COLUMN public.nexus_agent_document_embedding.token_count IS 'token数';
COMMENT ON COLUMN public.nexus_agent_document_embedding.embedding_model IS 'embedding 模型名称';
COMMENT ON COLUMN public.nexus_agent_document_embedding.metadata_json IS '向量检索附带元数据';
COMMENT ON COLUMN public.nexus_agent_document_embedding.embedding IS '向量值';
COMMENT ON COLUMN public.nexus_agent_document_embedding.create_time IS '创建时间';
COMMENT ON COLUMN public.nexus_agent_document_embedding.edit_time IS '编辑时间';
COMMENT ON COLUMN public.nexus_agent_document_embedding.status IS '1:正常 0:删除';

CREATE INDEX IF NOT EXISTS idx_nexus_agent_document_embedding_document_id
    ON public.nexus_agent_document_embedding (document_id);

CREATE INDEX IF NOT EXISTS idx_nexus_agent_document_embedding_task_id
    ON public.nexus_agent_document_embedding (task_id);

CREATE INDEX IF NOT EXISTS idx_nexus_agent_document_embedding_plan_id
    ON public.nexus_agent_document_embedding (plan_id);

CREATE INDEX IF NOT EXISTS idx_nexus_agent_document_embedding_parent_block_id
    ON public.nexus_agent_document_embedding (parent_block_id);

CREATE INDEX IF NOT EXISTS idx_nexus_agent_document_embedding_status
    ON public.nexus_agent_document_embedding (status);

-- 当前第一期为了兼容不同 embedding 模型的维度变化，embedding 字段使用未固定维度的 VECTOR 类型。
-- 如果后续固定模型与维度，例如 1024 或 1536，
-- 可以把字段改成 VECTOR(1024/1536) 并补充 HNSW 或 IVF_FLAT 向量索引。
