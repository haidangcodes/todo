package com.todo.repository;

import com.todo.entity.Task;
import com.todo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserOrderByCreatedAtDesc(User user);
    List<Task> findByUserAndCompletedFalseOrderByCreatedAtDesc(User user);
    List<Task> findByUserAndCategory(User user, String category);
    List<Task> findByUserAndPriority(User user, String priority);
    List<Task> findByUserAndDeadlineBetween(User user, java.time.LocalDateTime start, java.time.LocalDateTime end);

    @Query("SELECT t FROM Task t WHERE t.reminder IS NOT NULL " +
           "AND t.reminder <= :now AND t.reminderSent = false AND t.completed = false")
    List<Task> findByReminderDue(@Param("now") LocalDateTime now);
}
