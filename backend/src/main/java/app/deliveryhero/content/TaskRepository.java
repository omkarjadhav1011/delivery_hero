package app.deliveryhero.content;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<TaskEntity, UUID> {

    Optional<TaskEntity> findByTaskKey(String taskKey);

    boolean existsByTaskKey(String taskKey);
}
