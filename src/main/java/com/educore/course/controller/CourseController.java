package com.educore.course.controller;
import com.educore.common.*;
import com.educore.course.dto.CourseStatusRequest;
import com.educore.course.dto.SaveCourseRequest;
import com.educore.course.service.CourseService;
import com.educore.course.vo.CourseView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated
public class CourseController {
 private final CourseService service;public CourseController(CourseService service){this.service=service;}
 @PostMapping("/api/admin/courses") public Result<CourseView> create(@Valid @RequestBody SaveCourseRequest req,HttpServletRequest r){return Result.success(service.create(req),rid(r));}
 @PutMapping("/api/admin/courses/{id}") public Result<CourseView> update(@PathVariable @Min(1) Long id,@Valid @RequestBody SaveCourseRequest req,HttpServletRequest r){return Result.success(service.update(id,req),rid(r));}
 @PatchMapping("/api/admin/courses/{id}/status") public Result<CourseView> status(@PathVariable @Min(1) Long id,@Valid @RequestBody CourseStatusRequest req,HttpServletRequest r){return Result.success(service.updateStatus(id,req.status()),rid(r));}
 @GetMapping("/api/courses") public Result<PageView<CourseView>> list(@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size,HttpServletRequest r){return Result.success(service.listPublished(page,size),rid(r));}
 @GetMapping("/api/admin/courses") public Result<PageView<CourseView>> adminList(@RequestParam(defaultValue="1") @Min(1) long page,@RequestParam(defaultValue="20") @Min(1) @Max(100) long size,HttpServletRequest r){return Result.success(service.listAdmin(page,size),rid(r));}
 @GetMapping("/api/courses/{id}") public Result<CourseView> detail(@PathVariable @Min(1) Long id,HttpServletRequest r){return Result.success(service.detailPublished(id),rid(r));}
 private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
