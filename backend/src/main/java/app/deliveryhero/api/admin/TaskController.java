package app.deliveryhero.api.admin;

import app.deliveryhero.content.PublicTaskView;
import app.deliveryhero.content.TaskDetail;
import app.deliveryhero.content.TaskInput;
import app.deliveryhero.content.TaskService;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The task editor's endpoints (API section 7.4). */
@RestController
@RequestMapping("/api/admin/tasks")
class TaskController {

    private final TaskService tasks;

    TaskController(TaskService tasks) {
        this.tasks = tasks;
    }

    @GetMapping("/{id}")
    TaskDetail get(@PathVariable UUID id) {
        return tasks.get(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    TaskDetail create(@RequestBody TaskInput input) {
        return tasks.create(input);
    }

    @PutMapping("/{id}")
    TaskDetail update(@PathVariable UUID id, @RequestBody TaskInput input) {
        return tasks.update(id, input);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id, @RequestParam int version) {
        tasks.delete(id, version);
    }

    @PostMapping("/public-view")
    PublicTaskView publicView(@RequestBody TaskInput input) {
        return tasks.publicView(input);
    }
}
