package com.educore.examination.entity.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record SaveExamAnswersRequest(@NotNull Map<Long, JsonNode> answers) { }
