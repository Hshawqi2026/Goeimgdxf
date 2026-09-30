package com.example.data

import android.content.Context
import com.example.model.ProjectData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class ProjectRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val projectDao = database.projectDao()

    val allProjects: Flow<List<ProjectEntity>> = projectDao.getAllProjects()

    suspend fun saveProject(project: ProjectData): String = withContext(Dispatchers.IO) {
        val projectsDir = File(context.filesDir, "projects")
        if (!projectsDir.exists()) projectsDir.mkdirs()

        val projectFile = File(projectsDir, "${project.id}.geo2dxf")
        projectFile.writeText(project.toJson())

        val entity = ProjectEntity(
            id = project.id,
            name = project.name,
            createdAt = project.createdAt,
            updatedAt = System.currentTimeMillis(),
            entityCount = project.entities.size,
            layerCount = project.layers.size,
            imagePath = project.imagePath,
            projectFilePath = projectFile.absolutePath
        )
        projectDao.insertProject(entity)
        projectFile.absolutePath
    }

    suspend fun loadProject(projectId: String): ProjectData? = withContext(Dispatchers.IO) {
        val entity = projectDao.getProjectById(projectId) ?: return@withContext null
        val file = File(entity.projectFilePath)
        if (!file.exists()) return@withContext null
        val jsonStr = file.readText()
        ProjectData.fromJson(jsonStr)
    }

    suspend fun deleteProject(projectId: String) = withContext(Dispatchers.IO) {
        val entity = projectDao.getProjectById(projectId)
        if (entity != null) {
            val file = File(entity.projectFilePath)
            if (file.exists()) file.delete()
            projectDao.deleteById(projectId)
        }
    }
}
