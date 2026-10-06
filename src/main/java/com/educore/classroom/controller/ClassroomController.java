package com.educore.classroom.controller;
import com.educore.classroom.entity.dto.ClassroomStatusRequest;
import com.educore.classroom.entity.dto.SaveClassroomRequest;
import com.educore.classroom.service.ClassroomService;
import com.educore.classroom.entity.vo.ClassroomView;
import com.educore.common.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @Validated @RequestMapping("/api/admin/classrooms") public class ClassroomController {
 private final ClassroomService service;public ClassroomController(ClassroomService service){this.service=service;}
 @PostMapping public Result<ClassroomView> create(@Valid @RequestBody SaveClassroomRequest req,HttpServletRequest r){return Result.success(service.create(req),rid(r));}
 @PutMapping("/{id}") public Result<ClassroomView> update(@PathVariable @Min(1) Long id,@Valid @RequestBody SaveClassroomRequest req,HttpServletRequest r){return Result.success(service.update(id,req),rid(r));}
 @PatchMapping("/{id}/status") public Result<ClassroomView> status(@PathVariable @Min(1) Long id,@Valid @RequestBody ClassroomStatusRequest req,HttpServletRequest r){return Result.success(service.updateStatus(id,req.status()),rid(r));}
 @GetMapping public Result<List<ClassroomView>> list(HttpServletRequest r){return Result.success(service.list(),rid(r));}
 private String rid(HttpServletRequest r){return String.valueOf(r.getAttribute("requestId"));}
}
