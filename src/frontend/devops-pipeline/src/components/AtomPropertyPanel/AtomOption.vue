<template>
    <accordion show-checkbox show-content key="otherChoice">
        <header class="var-header" slot="header">
            <span>{{ $t('editPage.atomOption') }}</span>
            <i class="bk-icon icon-angle-down" style="display:block"></i>
        </header>
        <div slot="content" class="bk-form bk-form-vertical">
            <template v-for="(obj, key) in optionModel">
                <form-field :key="key" v-if="(!isHidden(obj, element) && container['@type'] !== 'trigger') || key === 'enable'" :desc="obj.desc" :required="obj.required" :label="obj.label" :is-error="errors.has(key)" :error-msg="errors.first(key)">
                    <component :is="obj.component" :container="container" :element="element" :name="key" v-validate.initial="Object.assign({}, obj.rule, { required: !!obj.required })" :handle-change="handleUpdateElementOption" :value="atomOption[key]" v-bind="obj"></component>
                </form-field>
            </template>
        </div>
    </accordion>
</template>

<script>
    import Vue from 'vue'
    import { mapActions } from 'vuex'
    import atomMixin from './atomMixin'
    import validMixins from '../validMixins'
    import optionConfigMixin from '@/store/modules/soda/optionConfigMixin'
    // import {
    //     getAtomOptionDefault,
    //     ATOM_OPTION
    // } from '@/store/modules/soda/optionConfig'
    export default {
        name: 'atom-config',
        mixins: [atomMixin, validMixins, optionConfigMixin],
        computed: {
            atomOption () {
                return this.element.additionalOptions || {}
            },
            atomCode () {
                return this.element.atomCode
            },
            atomVersion () {
                return this.element.version
            },
            optionModel () {
                return this.ATOM_OPTION || {}
            }
        },
        watch: {
            atomCode () {
                this.initOptionConfig()
            },
            atomVersion () {
                this.initOptionConfig()
            }
        },
        created () {
            this.initOptionConfig()
        },
        methods: {
            ...mapActions('atom', [
                'setPipelineEditing'
            ]),
            // getAtomOptionDefault,
            handleUpdateElementOption (name, value) {
                if (this.element.additionalOptions && this.element.additionalOptions[name] === undefined) {
                    Vue.set(this.element.additionalOptions, name, value)
                }
                this.setPipelineEditing(true)
                this.handleUpdateElement('additionalOptions',
                                         Object.assign(this.element.additionalOptions || {}, { [name]: value })
                )
            },
            initOptionConfig () {
                if (this.element.additionalOptions === undefined) {
                    this.handleUpdateElement('additionalOptions', this.getAtomOptionDefault())
                }
            }
        }
    }
</script>
