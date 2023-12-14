package com.example.controller;

import com.example.mapping.DepreciationHistoryMapping;
import com.example.service.DepreciationHistoryService;
import com.example.tasks.DepreciationHistoryTask;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/depreciation/history")
@RequiredArgsConstructor
public class DepreciationHistoryController {
    private final DepreciationHistoryTask depreciationHistoryTask;
    //Tất cả thông tin khấu hao của phòng ban
    @PostMapping("/test")
    public ResponseEntity getTest(@RequestParam String text) throws ParseException {
        depreciationHistoryTask.calculateDepreciationPerMonthTest(text);
        return new ResponseEntity("Tính khấu hao thánh công",HttpStatus.OK);
    }
}
