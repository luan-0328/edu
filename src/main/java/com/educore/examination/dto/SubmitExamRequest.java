package com.educore.examination.dto;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
public record SubmitExamRequest(@NotNull Map<Long, JsonNode> answers) { }
