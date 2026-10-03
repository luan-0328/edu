package com.educore.attendance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BatchAttendanceRequest(@NotEmpty @Size(max = 200) List<@Valid AttendanceItemRequest> records) { }
