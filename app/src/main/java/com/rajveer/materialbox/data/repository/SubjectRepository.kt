package com.rajveer.materialbox.data.repository

import com.rajveer.materialbox.data.dao.SubjectDao
import com.rajveer.materialbox.data.entity.Subject
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao
) {
    fun getAllSubjects(): Flow<List<Subject>> = subjectDao.getAllSubjects()

    suspend fun getSubjectById(id: Long): Subject? = subjectDao.getSubjectById(id)

    suspend fun insertSubject(subject: Subject): Long = subjectDao.insertSubject(subject)

    suspend fun updateSubject(subject: Subject) = subjectDao.updateSubject(subject)

    suspend fun deleteSubject(subject: Subject) = subjectDao.deleteSubject(subject)
} 