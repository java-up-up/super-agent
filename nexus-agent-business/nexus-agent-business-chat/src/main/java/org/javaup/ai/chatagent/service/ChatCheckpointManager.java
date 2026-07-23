package org.javaup.ai.chatagent.service;

import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.checkpoint.Checkpoint;
import com.alibaba.cloud.ai.graph.checkpoint.savers.mysql.MysqlSaver;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.javaup.ai.chatagent.data.GraphCheckpoint;
import org.javaup.ai.chatagent.data.GraphThread;
import org.javaup.ai.chatagent.mapper.GraphCheckpointMapper;
import org.javaup.ai.chatagent.mapper.GraphThreadMapper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * @program: 企业级别深度设计 AI Agent。添加 阿星不是程序员 微信，添加时备注 super 来获取项目的完整资料
 * @description: 服务层
 * @author: 阿星不是程序员
 **/

@Component
public class ChatCheckpointManager {

    private final MysqlSaver checkpointSaver;
    private final GraphCheckpointMapper graphCheckpointMapper;
    private final GraphThreadMapper graphThreadMapper;

    public ChatCheckpointManager(MysqlSaver checkpointSaver,
                                 GraphCheckpointMapper graphCheckpointMapper,
                                 GraphThreadMapper graphThreadMapper) {
        this.checkpointSaver = checkpointSaver;
        this.graphCheckpointMapper = graphCheckpointMapper;
        this.graphThreadMapper = graphThreadMapper;
    }

    public Optional<Checkpoint> get(RunnableConfig runnableConfig) {

        return checkpointSaver.get(runnableConfig);
    }

    public Collection<Checkpoint> list(RunnableConfig runnableConfig) {

        return checkpointSaver.list(runnableConfig);
    }

    @Transactional
    public int clearThread(String threadId) {
        List<GraphThread> threads = graphThreadMapper.selectList(
            new LambdaQueryWrapper<GraphThread>()
                .eq(GraphThread::getThreadName, threadId)
        );
        if (threads == null || threads.isEmpty()) {
            return 0;
        }

        List<String> graphThreadIds = threads.stream()
            .map(GraphThread::getThreadId)
            .toList();

        int checkpointCount = toInt(graphCheckpointMapper.selectCount(
            new LambdaQueryWrapper<GraphCheckpoint>()
                .in(GraphCheckpoint::getThreadId, graphThreadIds)
        ));

        if (checkpointCount > 0) {

            graphCheckpointMapper.delete(
                new LambdaQueryWrapper<GraphCheckpoint>()
                    .in(GraphCheckpoint::getThreadId, graphThreadIds)
            );
        }
        graphThreadMapper.delete(
            new LambdaQueryWrapper<GraphThread>()
                .eq(GraphThread::getThreadName, threadId)
        );
        return checkpointCount;
    }

    private int toInt(Long count) {

        return count == null ? 0 : count.intValue();
    }
}
