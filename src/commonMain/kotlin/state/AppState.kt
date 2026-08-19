package com.ominigifmaker.state

import com.ominigifmaker.model.GifMetaData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** 源文件元数据读取状态。 */
sealed interface MetaDataStatus {
    /** 未选择文件。 */
    data object Idle : MetaDataStatus

    /** 正在调用 ffprobe 读取。 */
    data object Loading : MetaDataStatus

    /** 读取成功。 */
    data class Ready(val data: GifMetaData) : MetaDataStatus

    /** 读取失败。 */
    data class Error(val message: String) : MetaDataStatus
}

/**
 * 全局应用状态（视作 ViewModel 层）。
 *
 * 持有当前选中的源 GIF 路径、基础元数据、当前 Tab，以及跨模块共享的任务状态，
 * 通过 [StateFlow] 暴露供 Compose UI 收集。
 */
class AppState(private val settings: KeyValueStore? = null) {

    /** Resize 模块状态。 */
    val resizeState = ResizeState(settings)

    /** Crop 模块状态。 */
    val cropState = CropState(settings)

    /** Rotate 模块状态。 */
    val rotateState = RotateState(settings)

    /** Reverse 模块状态。 */
    val reverseState = ReverseState(settings)

    /** Speed 模块状态。 */
    val speedState = SpeedState(settings)

    /** Optimize 模块状态。 */
    val optimizeState = OptimizeState(settings)

    /** Frames 模块状态。 */
    val framesState = FramesState(settings)

    /** 界面语言（持久化）。 */
    private val _language = MutableStateFlow(
        Language.fromCode(settings?.getString(KEY_LANGUAGE, "") ?: "") ?: Language.EN
    )
    val language: StateFlow<Language> = _language.asStateFlow()

    private val _selectedPage = MutableStateFlow<AppPage>(AppPage.Module(AppTab.Resize))
    val selectedPage: StateFlow<AppPage> = _selectedPage.asStateFlow()

    private val _sourceGifPath = MutableStateFlow<String?>(null)
    val sourceGifPath: StateFlow<String?> = _sourceGifPath.asStateFlow()

    private val _metaDataStatus = MutableStateFlow<MetaDataStatus>(MetaDataStatus.Idle)
    val metaDataStatus: StateFlow<MetaDataStatus> = _metaDataStatus.asStateFlow()

    private val _taskStatus = MutableStateFlow<TaskStatus>(TaskStatus.Idle)
    val taskStatus: StateFlow<TaskStatus> = _taskStatus.asStateFlow()

    /** 当前已成功读取的元数据（仅当处于 [MetaDataStatus.Ready] 时非空）。 */
    val metaData: GifMetaData? get() = (_metaDataStatus.value as? MetaDataStatus.Ready)?.data

    fun selectPage(page: AppPage) {
        _selectedPage.value = page
    }

    /** 设置源 GIF 路径；置空时同时清空元数据与任务状态。 */
    fun setSourceGif(path: String?) {
        _sourceGifPath.value = path
        if (path == null) {
            _metaDataStatus.value = MetaDataStatus.Idle
            _taskStatus.value = TaskStatus.Idle
        }
    }

    fun setMetaDataStatus(status: MetaDataStatus) {
        _metaDataStatus.value = status
    }

    fun setTaskStatus(status: TaskStatus) {
        _taskStatus.value = status
    }

    fun setLanguage(language: Language) {
        _language.value = language
        settings?.putString(KEY_LANGUAGE, language.code)
    }

    private companion object {
        const val KEY_LANGUAGE = "app.language"
    }
}
