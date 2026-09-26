package app.deliveryhero.api.admin;

import app.deliveryhero.content.RunPlanService;
import app.deliveryhero.content.RunPlanSummary;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The run plan endpoints (API section 7.6). The list comes first, for the New game screen (PC-09); the rest with US-57. */
@RestController
@RequestMapping("/api/admin/run-plans")
class RunPlanController {

    private final RunPlanService plans;

    RunPlanController(RunPlanService plans) {
        this.plans = plans;
    }

    @GetMapping
    List<RunPlanSummary> list() {
        return plans.summaries();
    }
}
