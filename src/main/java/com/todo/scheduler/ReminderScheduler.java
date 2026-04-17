package com.todo.scheduler;

import com.todo.entity.Task;
import com.todo.repository.TaskRepository;
import com.todo.service.EmailService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class ReminderScheduler {

    private final TaskRepository taskRepository;
    private final EmailService emailService;

    public ReminderScheduler(TaskRepository taskRepository, EmailService emailService) {
        this.taskRepository = taskRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedRate = 60000) // every 1 minute
    public void checkReminders() {
        LocalDateTime now = LocalDateTime.now();
        List<Task> dueTasks = taskRepository.findByReminderDue(now);

        for (Task task : dueTasks) {
            if (task.getUser() == null || task.getCompleted()) continue;

            String email = task.getUser().getEmail();
            if (email != null && !email.isBlank()) {
                String deadline = task.getDeadline() != null
                    ? task.getDeadline().format(DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy"))
                    : null;
                emailService.sendReminderEmail(
                    email,
                    task.getTitle(),
                    task.getPriority(),
                    deadline
                );
            }
            task.setReminderSent(true);
            taskRepository.save(task);
        }
    }
}
