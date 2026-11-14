package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DefaultPresets
import com.example.data.WatchFaceEntity
import com.example.data.WatchFaceJsonHelper
import com.example.data.WatchFaceRepository
import com.example.generator.ValidationReport
import com.example.generator.WffValidator
import com.example.model.WatchFaceConfig
import com.example.previewer.PreviewSimulationState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class WatchFaceViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = WatchFaceRepository(application)

    // Current design being worked on
    private val _activeConfig = MutableStateFlow(DefaultPresets.presets[0])
    val activeConfig: StateFlow<WatchFaceConfig> = _activeConfig.asStateFlow()

    private val _activeEntityId = MutableStateFlow<Long?>(null)
    val activeEntityId: StateFlow<Long?> = _activeEntityId.asStateFlow()

    // Preview state
    private val _simulationState = MutableStateFlow(PreviewSimulationState())
    val simulationState: StateFlow<PreviewSimulationState> = _simulationState.asStateFlow()

    // Validation Report
    private val _validationReport = MutableStateFlow(WffValidator.validate(_activeConfig.value))
    val validationReport: StateFlow<ValidationReport> = _validationReport.asStateFlow()

    // Saved projects from Room
    val savedProjects: StateFlow<List<WatchFaceEntity>> = repository.allWatchFaces
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize default presets in DB if empty
        viewModelScope.launch {
            repository.initializePresetsIfEmpty()
        }

        // Live clock loop
        viewModelScope.launch {
            while (true) {
                if (_simulationState.value.isLiveTime) {
                    val cal = Calendar.getInstance()
                    _simulationState.value = _simulationState.value.copy(
                        simulatedHour = cal.get(Calendar.HOUR_OF_DAY),
                        simulatedMinute = cal.get(Calendar.MINUTE),
                        simulatedSecond = cal.get(Calendar.SECOND) + (cal.get(Calendar.MILLISECOND) / 1000f)
                    )
                }
                delay(60) // ~16 fps for smooth hand sweep in preview
            }
        }
    }

    fun updateConfig(config: WatchFaceConfig) {
        _activeConfig.value = config
        _validationReport.value = WffValidator.validate(config)
        com.example.generator.WatchFacePackageExporter.saveActiveWatchFaceForService(getApplication(), config)
    }

    fun updateSimulationState(state: PreviewSimulationState) {
        _simulationState.value = state
    }

    fun saveCurrentProject(onSaved: (() -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveWatchFace(_activeConfig.value, _activeEntityId.value)
            _activeEntityId.value = id
            com.example.generator.WatchFacePackageExporter.saveActiveWatchFaceForService(getApplication(), _activeConfig.value)
            onSaved?.invoke()
        }
    }

    fun loadProject(config: WatchFaceConfig, entityId: Long) {
        _activeConfig.value = config
        _activeEntityId.value = if (entityId > 0) entityId else null
        _validationReport.value = WffValidator.validate(config)
        com.example.generator.WatchFacePackageExporter.saveActiveWatchFaceForService(getApplication(), config)
    }

    fun createNewProject() {
        val blank = WatchFaceConfig(
            id = "wff_custom_${System.currentTimeMillis()}",
            name = "New Custom Face",
            targetWearOsVersion = 7
        )
        _activeConfig.value = blank
        _activeEntityId.value = null
        _validationReport.value = WffValidator.validate(blank)
    }

    fun duplicateProject(entity: WatchFaceEntity) {
        viewModelScope.launch {
            val parsed = WatchFaceJsonHelper.fromJson(entity.configJson)
            val dup = parsed.copy(
                id = "wff_dup_${System.currentTimeMillis()}",
                name = "${parsed.name} (Copy)"
            )
            repository.saveWatchFace(dup, null)
        }
    }

    fun deleteProject(entityId: Long) {
        viewModelScope.launch {
            repository.deleteWatchFace(entityId)
            if (_activeEntityId.value == entityId) {
                _activeEntityId.value = null
            }
        }
    }

    fun toggleFavorite(entityId: Long, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(entityId, isFavorite)
        }
    }
}
