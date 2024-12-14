package org.example.taskmanagementsystem.repositories;

import org.example.taskmanagementsystem.models.Task;
import org.example.taskmanagementsystem.models.TaskPriority;
import org.example.taskmanagementsystem.models.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByAuthorId(Long authorId, Pageable pageable);

    Page<Task> findByAuthorIdAndPriority(Long authorId, TaskPriority priority, Pageable pageable);

    Page<Task> findByAuthorIdAndStatus(Long authorId, TaskStatus status, Pageable pageable);

    Page<Task> findByAuthorIdAndPriorityAndStatus(Long authorId, TaskPriority priority, TaskStatus status, Pageable pageable);
    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :assigneeId")
    Page<Task> findByAssigneeId(@Param("assigneeId") Long assigneeId, Pageable pageable);

    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :assigneeId AND t.priority = :priority")
    Page<Task> findByAssigneeIdAndPriority(@Param("assigneeId") Long assigneeId, @Param("priority") TaskPriority priority, Pageable pageable);

    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :assigneeId AND t.status = :status")
    Page<Task> findByAssigneeIdAndStatus(@Param("assigneeId") Long assigneeId, @Param("status") TaskStatus status, Pageable pageable);

    @Query("SELECT t FROM Task t JOIN t.assignees a WHERE a.id = :assigneeId AND t.priority = :priority AND t.status = :status")
    Page<Task> findByAssigneeIdAndPriorityAndStatus(
            @Param("assigneeId") Long assigneeId,
            @Param("priority") TaskPriority priority,
            @Param("status") TaskStatus status,
            Pageable pageable);
}
