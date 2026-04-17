package com.todo.service;

import com.todo.entity.Task;
import com.todo.entity.User;
import com.todo.repository.TaskRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaskService {

    @Autowired
    private TaskRepository taskRepository;

    public List<Task> getTasksByUser(User user) {
        return taskRepository.findByUserOrderByCreatedAtDesc(user);
    }

    public Task getTaskById(Long id) {
        return taskRepository.findById(id).orElse(null);
    }

    @Transactional
    public Task createTask(Task task) {
        task.setCreatedAt(LocalDateTime.now());
        return taskRepository.save(task);
    }

    @Transactional
    public Task updateTask(Long id, Task taskDetails) {
        Task task = getTaskById(id);
        if (task == null) {
            throw new RuntimeException("Task not found with id: " + id);
        }
        task.setTitle(taskDetails.getTitle());
        task.setDescription(taskDetails.getDescription());
        task.setCategory(taskDetails.getCategory());
        task.setPriority(taskDetails.getPriority());
        task.setDeadline(taskDetails.getDeadline());
        task.setReminder(taskDetails.getReminder());
        task.setCompleted(taskDetails.getCompleted());
        task.setSubtasks(taskDetails.getSubtasks());
        return taskRepository.save(task);
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.deleteById(id);
    }

    @Transactional
    public Task toggleComplete(Long id) {
        Task task = getTaskById(id);
        if (task == null) {
            throw new RuntimeException("Task not found with id: " + id);
        }
        task.setCompleted(!task.getCompleted());
        return taskRepository.save(task);
    }

    public List<Task> getTasksByFilter(String filter, User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = startOfToday.plusDays(1);

        List<Task> userTasks = getTasksByUser(user);

        return switch (filter) {
            case "today" -> userTasks.stream()
                    .filter(t -> t.getDeadline() != null &&
                            t.getDeadline().isAfter(startOfToday) &&
                            t.getDeadline().isBefore(endOfToday))
                    .toList();
            case "upcoming" -> userTasks.stream()
                    .filter(t -> !t.getCompleted() && (t.getDeadline() == null || t.getDeadline().isAfter(now)))
                    .toList();
            case "overdue" -> userTasks.stream()
                    .filter(t -> t.getDeadline() != null && t.getDeadline().isBefore(now) && !t.getCompleted())
                    .toList();
            case "active" -> userTasks.stream()
                    .filter(t -> !t.getCompleted())
                    .toList();
            default -> userTasks;
        };
    }

    public List<Task> getTasksByCategory(String category, User user) {
        return taskRepository.findByUserAndCategory(user, category);
    }

    public List<Task> getTasksByPriority(String priority, User user) {
        return taskRepository.findByUserAndPriority(user, priority);
    }
}
