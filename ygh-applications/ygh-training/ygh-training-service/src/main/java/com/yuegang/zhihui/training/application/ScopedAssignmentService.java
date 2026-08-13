package com.yuegang.zhihui.training.application;

import com.yuegang.zhihui.training.api.AssignmentView;
import com.yuegang.zhihui.training.api.CreateAssignmentRequest;
import com.yuegang.zhihui.training.api.CreateScopedAssignmentRequest;
import com.yuegang.zhihui.training.api.ScopedAssignmentResult;
import com.yuegang.zhihui.training.infrastructure.OrganizationTargetClient;

import java.util.ArrayList;
import java.util.List;

public final class ScopedAssignmentService {
    private final TrainingAssignmentService assignments;
    private final OrganizationTargetClient targets;

    public ScopedAssignmentService(TrainingAssignmentService a, OrganizationTargetClient t) {
        assignments = a;
        targets = t;
    }

    public ScopedAssignmentResult assign(long operator, CreateScopedAssignmentRequest c) {
        List<AssignmentView> created = new ArrayList<>();
        for (String user : targets.resolve(c.targetType(), c.targetId()))
            created.add(assignments.assign(operator, new CreateAssignmentRequest(user, c.pathId(), c.courseId(), c.dueAt())));
        return new ScopedAssignmentResult(c.targetType(), c.targetId(), created.size(), List.copyOf(created));
    }
}
