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

package com.tencent.devops.process.api.builds

import com.tencent.devops.common.api.exception.ParamBlankException
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.engine.service.PipelineRepositoryService
import com.tencent.devops.process.permission.PipelinePermissionService
import com.tencent.devops.process.pojo.PipelineBuildMaterial
import com.tencent.devops.process.service.PipelineBuildMaterialService
import org.apache.commons.lang.StringUtils
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class BuildRepositoryResourceImpl @Autowired constructor(
    private val pipelineBuildMaterialService: PipelineBuildMaterialService,
    private val pipelinePermissionService: PipelinePermissionService,
    private val pipelineRepositoryService: PipelineRepositoryService
) : BuildRepositoryResource {
    override fun saveBuildMaterial(
        buildId: String,
        projectId: String,
        pipelineId: String,
        pipelineBuildMaterials: List<PipelineBuildMaterial>
    ): Result<Int> {
        checkParam(buildId = buildId, projectId = projectId, pipelineId = pipelineId)
        checkPermission(projectId = projectId, pipelineId = pipelineId)
        return Result(pipelineBuildMaterialService.saveBuildMaterial(buildId, projectId, pipelineId, pipelineBuildMaterials))
    }

    fun checkPermission(projectId: String, pipelineId: String) {
        val userId = pipelineRepositoryService.getPipelineInfo(projectId, pipelineId)?.lastModifyUser ?: ""
        if (!pipelinePermissionService.checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = AuthPermission.EXECUTE))
            throw PermissionForbiddenException("用户${userId}无权获取此流水线构建信息")
    }

    fun checkParam(buildId: String, projectId: String, pipelineId: String) {
        if (StringUtils.isBlank(buildId))
            throw ParamBlankException("build Id is null or blank")
        if (StringUtils.isBlank(pipelineId))
            throw ParamBlankException("pipeline Id is null or blank")
    }
}