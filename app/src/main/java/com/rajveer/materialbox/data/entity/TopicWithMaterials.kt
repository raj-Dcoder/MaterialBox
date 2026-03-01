package com.rajveer.materialbox.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class TopicWithMaterials(
    @Embedded val topic: Topic,
    @Relation(
        parentColumn = "id",
        entityColumn = "topicId"
    )
    val materials: List<Material>
)
