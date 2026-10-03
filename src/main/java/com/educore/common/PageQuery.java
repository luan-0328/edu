package com.educore.common;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record PageQuery(@Min(1) long page, @Min(1) @Max(100) long size) {
    public PageQuery() { this(1, 20); }
}
