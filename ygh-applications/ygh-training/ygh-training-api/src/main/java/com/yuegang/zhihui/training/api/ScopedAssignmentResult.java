package com.yuegang.zhihui.training.api;

import java.util.List;

public record ScopedAssignmentResult(String targetType, String targetId, int assignedCount,
                                     List<AssignmentView> assignments) {
}
