/*
 * Tencent is pleased to support the open source community by making BK-CI 蓝鲸持续集成平台 available.
 *
 * Copyright (C) 2019 THL A29 Limited, a Tencent company.  All rights reserved.
 *
 * BK-CI 蓝鲸持续集成平台 is licensed under the MIT license.
 *
 * A copy of the MIT License is included in this file.
 *
 *
 * Terms of the MIT License:
 * ---------------------------------------------------
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 * LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 * NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.tencent.devops.process.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.pipeline.pojo.element.ElementAdditionalOptions
import com.tencent.devops.common.redis.RedisOperation
import com.tencent.devops.log.utils.LogUtils
import com.tencent.devops.process.dao.PipelineTaskDao
import com.tencent.devops.process.engine.control.ControlUtils
import com.tencent.devops.process.engine.dao.PipelineModelTaskDao
import com.tencent.devops.process.engine.pojo.PipelineModelTask
import com.tencent.devops.process.engine.service.PipelineRuntimeService
import com.tencent.devops.process.pojo.PipelineProjectRel
import org.jooq.DSLContext
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class PipelineTaskService @Autowired constructor(
    val dslContext: DSLContext,
    val redisOperation: RedisOperation,
    val objectMapper: ObjectMapper,
    val pipelineTaskDao: PipelineTaskDao,
    val pipelineModelTaskDao: PipelineModelTaskDao,
    private val rabbitTemplate: RabbitTemplate,
    private val pipelineRuntimeService: PipelineRuntimeService
) {

    fun list(projectId: String, pipelineIds: Collection<String>): Map<String, List<PipelineModelTask>> {
        return pipelineTaskDao.list(dslContext, projectId, pipelineIds)?.map {
            PipelineModelTask(
                projectId = it.projectId,
                pipelineId = it.pipelineId,
                stageId = it.stageId,
                containerId = it.containerId,
                taskId = it.taskId,
                taskSeq = it.taskSeq,
                taskName = it.taskName,
                atomCode = it.atomCode,
                classType = it.classType,
                taskAtom = it.taskAtom,
                taskParams = objectMapper.readValue(it.taskParams),
                additionalOptions = if (it.additionalOptions.isNullOrBlank())
                    null
                else objectMapper.readValue(it.additionalOptions, ElementAdditionalOptions::class.java),
                os = it.os
            )
        }?.groupBy { it.pipelineId } ?: mapOf()
    }

    /**
     * 根据插件标识，获取使用插件的流水线详情
     */
    fun listPipelinesByAtomCode(
        atomCode: String,
        projectCode: String?,
        page: Int?,
        pageSize: Int?
    ): Page<PipelineProjectRel> {
        val pageNotNull = page ?: 1
        val pageSizeNotNull = pageSize ?: 100

        val count = pipelineModelTaskDao.getPipelineCountByAtomCode(dslContext, atomCode, projectCode).toLong()
        val pipelines =
            pipelineModelTaskDao.listByAtomCode(dslContext, atomCode, projectCode, pageNotNull, pageSizeNotNull)

        val records = if (pipelines == null) {
            listOf<PipelineProjectRel>()
        } else {
            pipelines.map {
                PipelineProjectRel(
                    pipelineId = it["pipelineId"] as String,
                    pipelineName = it["pipelineName"] as String,
                    projectCode = it["projectCode"] as String
                )
            }
        }

        return Page(pageNotNull, pageSizeNotNull, count, records)
    }

    fun isRetryWhenFail(taskId: String, buildId: String): Boolean {
        val taskRecord = pipelineRuntimeService.getBuildTask(buildId, taskId)
        val retryCount = redisOperation.get(getRedisKey(taskRecord!!.buildId, taskRecord.taskId))?.toInt() ?: 0
        val isRry = ControlUtils.retryWhenFailure(taskRecord!!.additionalOptions, retryCount)
        if (isRry) {
            logger.info("retry task [$buildId]|stageId=${taskRecord.stageId}|container=${taskRecord.containerId}|taskId=$taskId|retryCount=$retryCount |vm atom will retry, even the task is failure")
            val nextCount = retryCount + 1
            redisOperation.set(getRedisKey(taskRecord!!.buildId, taskRecord.taskId), nextCount.toString())
            LogUtils.addYellowLine(
                rabbitTemplate = rabbitTemplate,
                buildId = buildId,
                message = "插件${taskRecord.taskName}执行失败, 5s后开始执行第${nextCount}次重试",
                tag = taskRecord.taskId,
                jobId = taskRecord.containerId,
                executeCount = 1
            )
        }
        return isRry
    }


    fun removeRetryCache(buildId: String, taskId: String) {
        // 清除该原子内的重试记录
        redisOperation.delete(getRedisKey(buildId, taskId))
    }

    private fun getRedisKey(buildId: String, taskId: String): String {
        return "$retryCountRedisKey$buildId:$taskId"
    }

    companion object {
        val logger = LoggerFactory.getLogger(this::class.java)
        private const val retryCountRedisKey = "process:task:failRetry:count:"
    }
}