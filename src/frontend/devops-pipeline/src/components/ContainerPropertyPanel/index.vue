<template>
    <bk-sideslider class="sodaci-property-panel" width="640" :is-show.sync="visible" :quick-close="true">
        <header class="container-panel-header" slot="header">
            {{ title }}
        </header>
        <section v-if="container" slot="content" :class="{ &quot;readonly&quot;: !editable }" class="container-property-panel bk-form bk-form-vertical">
            <form-field :required="true" :label="$t('name')" :is-error="errors.has(&quot;name&quot;)" :error-msg="errors.first(&quot;name&quot;)">
                <div class="container-resource-name">
                    <vuex-input :disabled="!editable" input-type="text" :placeholder="$t('nameInputTips')" name="name" v-validate.initial="&quot;required&quot;" :value="container.name" :handle-change="handleContainerChange" />
                    <atom-checkbox
                        v-if="isVmContainer(container)"
                        class="show-build-resource"
                        :value="container.showBuildResource"
                        :text="$t('editPage.showAliasName')"
                        name="showBuildResource"
                        :handle-change="handleContainerChange"
                    >
                    </atom-checkbox>
                </div>
            </form-field>

            <form v-if="isVmContainer(container)" v-bkloading="{ isLoading: !apps || !containerModalId || isLoadingImage }">
                <form-field :label="$t('editPage.resourceType')">
                    <selector
                        :disabled="!editable"
                        :handle-change="changeResourceType"
                        :list="buildResourceTypeList"
                        :value="buildResourceType"
                        :clearable="false"
                        setting-key="type"
                        name="buildType"
                    >
                        <template>
                            <div class="bk-selector-create-item cursor-pointer" @click.stop.prevent="addThridSlave">
                                <i class="bk-icon icon-plus-circle"></i>
                                <span class="text">{{ $t('editPage.addThirdSlave') }}</span>
                            </div>
                            <div v-if="container.baseOS === 'LINUX'" class="bk-selector-create-item cursor-pointer" @click.stop.prevent="addDockerImage">
                                <i class="bk-icon icon-plus-circle"></i>
                                <span class="text">{{ $t('editPage.addImage') }}</span>
                            </div>
                        </template>
                    </selector>
                    <span class="bk-form-help" v-if="isPublicResourceType">{{ $t('editPage.publicResTips') }}</span>
                </form-field>

                <form-field :label="$t('editPage.image')" v-if="['DOCKER', 'IDC', 'PUBLIC_DEVCLOUD'].includes(buildResourceType) && !isLoadingImage" :required="true" :is-error="errors.has(&quot;buildImageVersion&quot;) || errors.has(&quot;buildResource&quot;)" :error-msg="$t('editPage.imageErrMgs')">
                    <enum-input
                        name="imageType"
                        :list="imageTypeList"
                        :disabled="!editable"
                        :handle-change="changeBuildResource"
                        :value="buildImageType">
                    </enum-input>

                    <section v-if="buildImageType === 'BKSTORE'" class="bk-image">
                        <section class="image-name">
                            <span :class="[{ disable: !editable }, { 'not-recommend': imageRecommend === false }, 'image-named']" :title="imageRecommend === false ? $t('editPage.notRecomendImage') : buildImageName">{{buildImageName || $t('editPage.chooseImage')}}</span>
                            <bk-button theme="primary" @click.stop="chooseImage" :disabled="!editable">{{buildImageCode ? $t('editPage.reElection') : $t('editPage.select')}}</bk-button>
                        </section>
                        <bk-select @change="changeImageVersion" :value="buildImageVersion" searchable class="image-tag" :loading="isVersionLoading" :disabled="!editable" v-validate.initial="&quot;required&quot;" name="buildImageVersion">
                            <bk-option v-for="option in versionList"
                                :key="option.versionValue"
                                :id="option.versionValue"
                                :name="option.versionName"
                            >
                            </bk-option>
                        </bk-select>
                    </section>

                    <bk-input v-else @change="changeThirdImage" :value="buildResource" :disabled="!editable" class="bk-image" :placeholder="$t('editPage.thirdImageHolder')" v-validate.initial="&quot;required&quot;" name="buildResource"></bk-input>
                </form-field>

                <form-field :label="$t('editPage.assignResource')" v-if="buildResourceType !== 'MACOS' && !isPublicResourceType && containerModalId && !['DOCKER', 'IDC', 'PUBLIC_DEVCLOUD'].includes(buildResourceType)" :required="true" :is-error="errors.has(&quot;buildResource&quot;)" :error-msg="errors.first(&quot;buildResource&quot;)" :desc="buildResourceType === &quot;THIRD_PARTY_AGENT_ENV&quot; ? this.$t('editPage.thirdSlaveTips') : &quot;&quot;">
                    <container-env-node :disabled="!editable"
                        :os="container.baseOS"
                        :container-id="containerModalId"
                        :build-resource-type="buildResourceType"
                        :build-image-type="buildImageType"
                        :agent-type="buildAgentType"
                        :toggle-visible="toggleVisible"
                        :handle-change="changeBuildResource"
                        :add-thrid-slave="addThridSlave"
                        :value="buildResource"
                        :has-error="errors.has(&quot;buildResource&quot;)"
                        v-validate.initial="&quot;required&quot;"
                        name="buildResource"
                    />
                </form-field>

                <template v-if="buildResourceType === 'MACOS'">
                    <form-field :label="$t('editPage.macSystemVersion')" :required="true" :is-error="errors.has('systemVersion')" :error-msg="errors.first(`systemVersion`)">
                        <bk-select :disabled="!editable" :value="systemVersion" searchable :loading="isLoadingMac" name="systemVersion" v-validate.initial="'required'">
                            <bk-option v-for="item in systemVersionList"
                                :key="item"
                                :id="item"
                                :name="item"
                                @click.native="chooseMacSystem(item)">
                            </bk-option>
                        </bk-select>
                    </form-field>
                    <form-field :label="$t('editPage.xcodeVersion')" :required="true" :is-error="errors.has('xcodeVersion')" :error-msg="errors.first(`xcodeVersion`)">
                        <bk-select :disabled="!editable" :value="xcodeVersion" searchable :loading="isLoadingMac" name="xcodeVersion" v-validate.initial="'required'">
                            <bk-option v-for="item in xcodeVersionList"
                                :key="item"
                                :id="item"
                                :name="item"
                                @click.native="chooseXcode(item)">
                            </bk-option>
                        </bk-select>
                    </form-field>
                </template>

                <form-field :label="$t('editPage.imageTicket')" v-if="(buildResourceType === 'DOCKER') && buildImageType === 'THIRD'">
                    <request-selector v-bind="imageCredentialOption" :disabled="!editable" name="credentialId" :value="buildImageCreId" :handle-change="changeBuildResource"></request-selector>
                </form-field>

                <form-field :label="$t('editPage.workspace')" v-if="isThirdParty">
                    <vuex-input :disabled="!editable" name="workspace" :value="container.dispatchType.workspace" :handle-change="changeBuildResource" :placeholder="$t('editPage.workspaceTips')" />
                </form-field>
                <form-field class="container-app-field" v-if="showDependencies" :label="$t('editPage.envDependency')">
                    <container-app-selector :disabled="!editable" class="app-selector-item" v-if="!hasBuildEnv" app="" version=""
                        :handle-change="handleContainerAppChange"
                        :apps="apps"
                        :remove-container-app="removeContainerApp"
                        :add-container-app="containerAppList.length > 0 ? addContainerApp : null"
                    ></container-app-selector>
                    <container-app-selector :disabled="!editable" v-else class="app-selector-item" v-for="(version, app) in container.buildEnv"
                        :key="app"
                        :app="app"
                        :version="version"
                        :handle-change="handleContainerAppChange"
                        :envs="container.buildEnv"
                        :apps="apps"
                        :remove-container-app="removeContainerApp"
                        :add-container-app="containerAppList.length > 0 ? addContainerApp : null"
                    ></container-app-selector>
                </form-field>

                <div class="build-path-tips" v-if="hasBuildEnv">
                    <div class="tips-icon"><i class="bk-icon icon-info-circle-shape"></i></div>
                    <div class="tips-content">
                        <p class="tips-title">{{ $t('editPage.envDependencyTips') }}：</p>
                        <template v-for="(value, keys) in container.buildEnv">
                            <div class="tips-list" v-if="value" :key="keys">
                                <p class="tips-item">{{ appBinPath(value, keys) }}</p>
                                <p class="tips-item"
                                    v-for="(env, envIndex) in appEnvs[keys]" :key="envIndex">{{ appEnvPath(value, keys, env) }}</p>
                            </div>
                        </template>
                    </div>
                </div>
            </form>

            <section v-if="isTriggerContainer(container)">
                <version-config :disabled="!editable" :params="container.params" :build-no="container.buildNo" :set-parent-validate="setContainerValidate" :update-container-params="handleContainerChange"></version-config>
                <build-params key="params" :disabled="!editable" :params="container.params" :addition-params="container.templateParams" setting-key="params" :title="$t('template.pipelineVar')" :build-no="container.buildNo" :set-parent-validate="setContainerValidate" :update-container-params="handleContainerChange"></build-params>
                <build-params v-if="routeName === 'templateEdit'" key="templateParams" :disabled="!editable" :params="container.templateParams" :addition-params="container.params" setting-key="templateParams" :title="$t('template.templateConst')" :set-parent-validate="setContainerValidate" :update-container-params="handleContainerChange"></build-params>
            </section>

            <div>
                <div class="job-option">
                    <job-option
                        v-if="!isTriggerContainer(container)"
                        :job-option="container.jobControlOption"
                        :update-container-params="handleContainerChange"
                        :set-parent-validate="setContainerValidate"
                        @setKeyValueValidate="setContainerValidate"
                        :disabled="!editable"
                    >
                    </job-option>
                </div>
                <div class="job-mutual">
                    <job-mutual
                        v-if="!isTriggerContainer(container)"
                        :mutex-group="container.mutexGroup"
                        :update-container-params="handleContainerChange"
                        :set-parent-validate="setContainerValidate"
                        :disabled="!editable"
                    >
                    </job-mutual>
                </div>
            </div>

            <image-selector :is-show.sync="showImageSelector"
                v-if="['DOCKER', 'IDC', 'PUBLIC_DEVCLOUD'].includes(buildResourceType) && !isLoadingImage"
                :code="buildImageCode"
                :build-resource-type="buildResourceType"
                @choose="choose"
            ></image-selector>
        </section>
    </bk-sideslider>
</template>

<script>
    import { mapGetters, mapActions, mapState } from 'vuex'
    import Vue from 'vue'
    import RequestSelector from '@/components/atomFormField/RequestSelector'
    import EnumInput from '@/components/atomFormField/EnumInput'
    import VuexInput from '@/components/atomFormField/VuexInput'
    import Selector from '@/components/atomFormField/Selector'
    import FormField from '@/components/AtomPropertyPanel/FormField'
    import ContainerAppSelector from './ContainerAppSelector'
    import ContainerEnvNode from './ContainerEnvNode'
    import BuildParams from './BuildParams'
    import VersionConfig from './VersionConfig'
    import JobOption from './JobOption'
    import JobMutual from './JobMutual'
    import AtomCheckbox from '@/components/atomFormField/AtomCheckbox'
    import ImageSelector from '@/components/AtomSelector/imageSelector'

    export default {
        name: 'container-property-panel',
        components: {
            RequestSelector,
            EnumInput,
            VuexInput,
            FormField,
            ContainerAppSelector,
            BuildParams,
            VersionConfig,
            ContainerEnvNode,
            JobOption,
            JobMutual,
            Selector,
            AtomCheckbox,
            ImageSelector
        },
        props: {
            containerIndex: Number,
            stageIndex: Number,
            stages: Array,
            editable: Boolean,
            title: String
        },
        data () {
            return {
                showImageSelector: false,
                isVersionLoading: false,
                isLoadingImage: false,
                imageRecommend: true,
                isLoadingMac: false,
                xcodeVersionList: [],
                systemVersionList: []
            }
        },
        computed: {
            ...mapState('atom', [
                'execDetail'
            ]),
            ...mapGetters('atom', [
                'getAppEnvs',
                'getContainerApps',
                'getContainer',
                'getContainers',
                'getStage',
                'isVmContainer',
                'isTriggerContainer',
                'isNormalContainer',
                'getBuildResourceTypeList',
                'getContainerModalId',
                'isThirdPartyContainer',
                'isPublicResource',
                'isDockerBuildResource'
            ]),
            ...mapState('atom', [
                'isPropertyPanelVisible'
            ]),
            ...mapState('soda', [
                'tstackWhiteList'
            ]),
            visible: {
                get () {
                    return this.isPropertyPanelVisible
                },
                set (value) {
                    this.togglePropertyPanel({
                        isShow: value
                    })
                }
            },
            imageTypeList () {
                return [
                    { label: this.$t('editPage.fromList'), value: 'BKSTORE' },
                    { label: this.$t('editPage.fromHand'), value: 'THIRD', hidden: this.buildResourceType === 'PUBLIC_DEVCLOUD' }
                ]
            },
            appEnvs () {
                return this.getAppEnvs(this.container.baseOS)
            },
            routeName () {
                return this.$route.name
            },
            projectId () {
                return this.$route.params.projectId
            },
            pipelineId () {
                return this.$route.params.pipelineId
            },
            buildId () {
                return this.$route.params.buildNo
            },
            stage () {
                const { stageIndex, stages } = this
                return this.getStage(stages, stageIndex)
            },
            containers () {
                const { stage, getContainers } = this
                return getContainers(stage)
            },
            container () {
                const { containers, containerIndex } = this
                return this.getContainer(containers, containerIndex)
            },
            isPublicResourceType () {
                return this.isPublicResource(this.container)
            },
            isThirdParty () {
                return this.isThirdPartyContainer(this.container)
            },
            isDocker () {
                return this.isDockerBuildResource(this.container)
            },
            containerModalId () {
                const { container: { baseOS }, getContainerModalId } = this
                return getContainerModalId(baseOS)
            },
            hasBuildEnv () {
                return Object.keys(this.container.buildEnv).length > 0
            },
            buildResourceType () {
                try {
                    return this.container.dispatchType.buildType
                } catch (e) {
                    return ''
                }
            },
            xcodeVersion () {
                return this.container.dispatchType.xcodeVersion
            },
            systemVersion () {
                return this.container.dispatchType.systemVersion
            },
            buildResource () {
                return this.container.dispatchType.value
            },
            buildImageType () {
                return this.container.dispatchType.imageType
            },
            buildImageCode () {
                return this.container.dispatchType && this.container.dispatchType.imageCode
            },
            buildImageVersion () {
                return this.container.dispatchType.imageVersion
            },
            buildImageName () {
                return this.container.dispatchType && this.container.dispatchType.imageName
            },
            buildImageCreId () {
                return this.container.dispatchType.credentialId || ''
            },
            buildAgentType () {
                return this.container.dispatchType.agentType || 'ID'
            },
            apps () {
                const { container: { baseOS }, getContainerApps } = this
                return getContainerApps(baseOS)
            },
            showDependencies () {
                const buildType = this.buildResourceTypeList.find(bt => bt.type === this.buildResourceType)
                return this.apps && buildType && buildType.enableApp
            },
            buildResourceTypeList () {
                const { container: { baseOS }, getBuildResourceTypeList } = this
                return getBuildResourceTypeList(baseOS)
            },
            containerAppList () {
                const { apps, container: { buildEnv } } = this
                const selectedApps = Object.keys(buildEnv)
                return Object.keys(apps).filter(app => !selectedApps.includes(app))
            },
            imageCredentialOption () {
                return {
                    paramId: 'credentialId',
                    paramName: 'credentialId',
                    url: `/ticket/api/user/credentials/${this.projectId}/hasPermissionList?permission=USE&page=1&pageSize=1000&credentialTypes=USERNAME_PASSWORD`,
                    hasAddItem: true,
                    itemText: this.$t('editPage.addCredentials'),
                    itemTargetUrl: `/ticket/${this.projectId}/createCredential/USERNAME_PASSWORD/true`
                }
            }
        },
        watch: {
            errors: {
                deep: true,
                handler: function (errors, old) {
                    // this.setContainerValidate()
                    const isError = errors.any()
                    this.handleContainerChange('isError', isError)
                }
            }
        },
        created () {
            const { container } = this
            if (this.isTriggerContainer(container) && this.container.templateParams === undefined) {
                Vue.set(container, 'templateParams', [])
            }
            if (this.isTriggerContainer(container) && this.container.buildNo === undefined) {
                Vue.set(container, 'buildNo', null)
            }
            if (!this.isTriggerContainer(container) && this.container.jobControlOption === undefined) {
                Vue.set(container, 'jobControlOption', {})
            }
            if (!this.isTriggerContainer(container) && this.container.mutexGroup === undefined) {
                Vue.set(container, 'mutexGroup', {})
            }
            if (this.buildResourceType === 'THIRD_PARTY_AGENT_ID' && !this.container.dispatchType.agentType) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    agentType: 'ID'
                }))
            }
            if (['DOCKER', 'IDC', 'PUBLIC_DEVCLOUD'].includes(this.buildResourceType) && !this.buildImageCode && this.buildImageType !== 'THIRD') {
                if (/\$\{/.test(this.buildResource)) {
                    this.handleContainerChange('dispatchType', Object.assign({
                        ...this.container.dispatchType,
                        imageType: 'THIRD'
                    }))
                } else {
                    this.isLoadingImage = true
                    this.requestImageHistory({ agentType: this.buildResourceType, value: this.buildResource }).then((res) => {
                        const data = res.data || {}
                        this.handleContainerChange('dispatchType', Object.assign({
                            ...this.container.dispatchType,
                            imageType: 'BKSTORE'
                        }))
                        data.historyVersion = data.version
                        if (data.code) this.choose(data)
                    }).catch((err) => this.$showTips({ theme: 'error', message: err.message || err })).finally(() => (this.isLoadingImage = false))
                }
            }
            if (['DOCKER', 'IDC', 'PUBLIC_DEVCLOUD'].includes(this.buildResourceType) && this.buildImageCode) {
                this.isLoadingImage = true
                this.requestImageDetail({ code: this.buildImageCode }).then((res) => {
                    const data = res.data || {}
                    this.imageRecommend = data.recommendFlag
                }).catch((err) => this.$showTips({ theme: 'error', message: err.message || err })).finally(() => (this.isLoadingImage = false))
            }
            if (this.container.dispatchType && this.container.dispatchType.imageCode) {
                this.getVersionList(this.container.dispatchType.imageCode)
            }
            if (this.buildResourceType === 'MACOS') this.getMacOsData()
        },
        methods: {
            ...mapActions('atom', [
                'updateContainer',
                'togglePropertyPanel',
                'getMacSysVersion',
                'getMacXcodeVersion'
            ]),
            ...mapActions('pipelines', [
                'requestImageVersionlist',
                'requestImageHistory',
                'requestImageDetail'
            ]),

            changeResourceType (name, val) {
                this.imageRecommend = true
                const defaultAgentType = (name === 'buildType' && ['THIRD_PARTY_AGENT_ID', 'THIRD_PARTY_AGENT_ENV'].includes(val) && !this.agentType) ? { agentType: 'ID' } : {}
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    ...defaultAgentType,
                    imageVersion: '',
                    value: '',
                    imageCode: '',
                    imageName: '',
                    [name]: val
                }))
                if (val === 'MACOS') this.getMacOsData()
            },

            changeThirdImage (val) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    value: val
                }))
            },

            changeImageVersion (value) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    imageVersion: value,
                    value: this.buildImageCode
                }))
            },

            choose (card) {
                this.imageRecommend = card.recommendFlag
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    imageCode: card.code,
                    imageName: card.name
                }))
                return this.getVersionList(card.code).then(() => {
                    let chooseVersion = this.versionList[0] || {}
                    if (card.historyVersion) chooseVersion = this.versionList.find(x => x.versionValue === card.historyVersion) || {}
                    this.handleContainerChange('dispatchType', Object.assign({
                        ...this.container.dispatchType,
                        imageVersion: chooseVersion.versionValue,
                        value: card.code
                    }))
                })
            },

            getVersionList (imageCode) {
                this.isVersionLoading = true
                const data = {
                    projectCode: this.projectId,
                    imageCode
                }
                return this.requestImageVersionlist(data).then((res) => {
                    this.versionList = res.data || []
                }).catch((err) => this.$showTips({ theme: 'error', message: err.message || err })).finally(() => {
                    this.isVersionLoading = false
                })
            },

            chooseImage (event) {
                event.preventDefault()
                this.showImageSelector = !this.showImageSelector
            },

            getMacOsData () {
                this.isLoadingMac = true
                Promise.all([this.getMacSysVersion(), this.getMacXcodeVersion()]).then(([sysVersion, xcodeVersion]) => {
                    this.xcodeVersionList = xcodeVersion.data || []
                    this.systemVersionList = sysVersion.data || []
                }).catch((err) => {
                    this.$bkMessage({ message: (err.message || err), theme: 'error' })
                }).finally(() => (this.isLoadingMac = false))
            },
            chooseMacSystem (item) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    systemVersion: item,
                    value: `${this.systemVersion}:${this.xcodeVersion}`
                }))
            },
            chooseXcode (item) {
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    xcodeVersion: item,
                    value: `${this.systemVersion}:${this.xcodeVersion}`
                }))
            },
            setContainerValidate (addErrors, removeErrors) {
                const { errors } = this

                if (addErrors && addErrors.length) {
                    addErrors.map(e => {
                        if (errors && errors.items.every(err => err.field !== e.field)) {
                            errors.add({
                                field: e.field,
                                msg: e.msg
                            })
                        }
                    })
                }
                if (removeErrors && removeErrors.length) {
                    removeErrors.map(e => errors.remove(e.field))
                }
                const isError = !!errors.items.length
                this.handleContainerChange('isError', isError)
            },

            changeBuildResource (name, value) {
                const emptyValueObj = (name === 'imageType' || name === 'agentType') ? { value: '' } : {}
                this.handleContainerChange('dispatchType', Object.assign({
                    ...this.container.dispatchType,
                    [name]: value
                }, emptyValueObj))
                this.handleContainerChange('buildEnv', {}) // 清空依赖编译环境
            },
            handleContainerChange (name, value) {
                this.updateContainer({
                    container: this.container,
                    newParam: {
                        [name]: value
                    }
                })
            },
            addContainerApp () {
                const { container, containerAppList } = this
                const newAppName = containerAppList[0]
                this.handleContainerChange('buildEnv', {
                    ...container.buildEnv,
                    [newAppName]: ''
                })
            },
            removeContainerApp (app) {
                const { container } = this
                delete container.buildEnv[app]
                this.handleContainerChange('buildEnv', {
                    ...container.buildEnv
                })
            },
            handleContainerAppChange (preApp, curApp, version = '') {
                const { container: { buildEnv }, apps } = this
                if (preApp !== curApp && buildEnv.hasOwnProperty(preApp)) {
                    delete buildEnv[preApp]
                }
                buildEnv[curApp] = version || apps[curApp][0]
                this.handleContainerChange('buildEnv', {
                    ...buildEnv
                })
            },
            appBinPath (value, key) {
                const { container: { baseOS }, apps } = this
                const app = apps[key]
                if (app) {
                    return baseOS === 'LINUX' ? `export PATH=/data/bkdevops/apps/${key}/${value}/${app.binPath}:$PATH` : `export PATH=/data/soda/apps/${key}/${value}/${app.binPath}:$PATH`
                } else {
                    return ''
                }
            },
            appEnvPath (value, key, env) {
                const { container: { baseOS } } = this
                return baseOS === 'LINUX' ? `export ${env.name}=/data/bkdevops/apps/${key}/${value}/${env.path}` : `export ${env.name}=/data/soda/apps/${key}/${value}/${env.path}`
            },
            addThridSlave () {
                const url = `${WEB_URL_PIRFIX}/environment/${this.projectId}/nodeList?type=${this.container.baseOS}`
                window.open(url, '_blank')
            },
            addDockerImage () {
                const url = `${WEB_URL_PIRFIX}/artifactory/${this.projectId}/depot/project-image`
                window.open(url, '_blank')
            }
        }
    }
</script>

<style lang="scss">
    @import '../AtomPropertyPanel/propertyPanel';
    .container-panel-header {
        display: flex;
        margin-right: 20px;
        justify-content: space-between;
    }
    .container-property-panel {
        font-size: 14px;
        .bk-image {
            display: flex;
            align-items: center;
            margin-top: 15px;
            .image-name {
                width: 50%;
                display: flex;
                align-items: center;
                .not-recommend {
                    text-decoration: line-through;
                }
                .image-named {
                    border: 1px solid #c4c6cc;
                    flex: 1;
                    height: 32px;
                    line-height: 32px;
                    font-size: 12px;
                    color: $fontWeightColor;
                    line-height: 32px;
                    padding-left: 10px;
                    overflow: hidden;
                    text-overflow: ellipsis;
                    white-space: nowrap;
                    &.disable {
                        color: #c4c6cc;
                        cursor: not-allowed;
                    }
                }
            }
            .image-tag {
                width: 50%;
                margin-left: 10px;
            }
        }
        .container-resource-name {
            display: flex;
            align-items: center;
            > input {
                flex: 1;
            }
            .show-build-resource {
                margin-left: 10px;
            }
        }
        .control-bar {
            position: absolute;
            right: 34px;
            top: 12px;
        }
        .accordion-checkbox {
            margin-left: auto;
        }
        .bk-form-content span.bk-form-help {
            padding-top: 5px;
            display: inline-block;
            a {
                color: #3c96ff;
                &:hover {
                    color: #3c96ff;
                }
            }
        }
        form .bk-form-item {
            margin-top: 8px;
        }
    }
    .app-selector-item {
        margin: 10px 0;
        &:last-child {
            .bk-icon.icon-plus {
                display: block;
            }
        }
    }
    .build-path-tips {
        display: flex;
        min-height: 60px;
        margin-top: 8px;
        .tips-icon {
            display: flex;
            width: 44px;
            align-items: center;
            text-align: center;
            border: 1px solid #ffb400;
            background-color: #ffb400;
            i {
                display: inline-block;
                font-size: 18px;
                color: #fff;
                margin: 21px 13px;
            }
        }
        .tips-content {
            flex: 1;
            padding: 0 20px;
            border: 1px solid #e6e6e6;
            border-left: none;
            .tips-title {
                margin: 15px 0;
                font-weight: 600;
            }
            .tips-list {
                margin-bottom: 10px;
            }
        }
    }
</style>
