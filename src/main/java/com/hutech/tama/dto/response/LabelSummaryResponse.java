package com.hutech.tama.dto.response;

import com.hutech.tama.entity.Label;

public record LabelSummaryResponse(
        Long id,
        String name,
        String color
) {
    public static LabelSummaryResponse from(Label label) {
        return new LabelSummaryResponse(label.getId(), label.getName(), label.getColor());
    }
}
