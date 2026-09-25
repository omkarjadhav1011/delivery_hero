package app.deliveryhero.api.ops;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The deploy-lock status that the deploy script reads on the machine (FR-090, document 11 section 7.10). Nginx never
 * forwards {@code /api/ops/}.
 */
@RestController
public class DeployLockController {

    @GetMapping("/api/ops/deploy-lock")
    public DeployLockResponse deployLock() {
        // TODO(US-68): ask DeployLockService whether a game is between LOBBY and REVEAL
        return DeployLockResponse.UNLOCKED;
    }
}
