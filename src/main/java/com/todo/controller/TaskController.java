package com.todo.controller;

import com.todo.entity.Task;
import com.todo.entity.User;
import com.todo.repository.UserRepository;
import com.todo.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    @Autowired
    private TaskService taskService;

    @Autowired
    private UserRepository userRepository;

    private User getCurrentUser(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return null;
        }
        Optional<User> user = userRepository.findById(userId);
        return user.orElse(null);
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(401).body(Map.of("error", "Unauthorized", "message", "Please login to access tasks"));
    }

    @GetMapping
    public ResponseEntity<?> getAllTasks(HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(taskService.getTasksByUser(user));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        Task task = taskService.getTaskById(id);
        if (task == null || !task.getUser().getId().equals(user.getId())) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(task);
    }

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        task.setUser(user);
        return ResponseEntity.ok(taskService.createTask(task));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @RequestBody Task task, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        Task existingTask = taskService.getTaskByIdForUser(id, user);
        if (existingTask == null) {
            return ResponseEntity.notFound().build();
        }
        task.setId(id);
        task.setUser(user);
        return ResponseEntity.ok(taskService.updateTask(id, task, user));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        Task existingTask = taskService.getTaskByIdForUser(id, user);
        if (existingTask == null) {
            return ResponseEntity.notFound().build();
        }
        taskService.deleteTask(id, user);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle")
    public ResponseEntity<?> toggleComplete(@PathVariable Long id, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        Task existingTask = taskService.getTaskByIdForUser(id, user);
        if (existingTask == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(taskService.toggleComplete(id, user));
    }

    @GetMapping("/filter/{filter}")
    public ResponseEntity<?> getTasksByFilter(@PathVariable String filter, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(taskService.getTasksByFilter(filter, user));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getTasksByCategory(@PathVariable String category, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(taskService.getTasksByCategory(category, user));
    }

    @GetMapping("/priority/{priority}")
    public ResponseEntity<?> getTasksByPriority(@PathVariable String priority, HttpSession session) {
        User user = getCurrentUser(session);
        if (user == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(taskService.getTasksByPriority(priority, user));
    }
}
