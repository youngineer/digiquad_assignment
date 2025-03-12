package com.digiquad.backend.controllers;


import com.digiquad.backend.services.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@RestController
@RequestMapping("/upload")
public class FileController {
    private final FileService fileService = new FileService();

    @PostMapping("/parseFile")
    public ResponseEntity<List<List<String>>> parseFile(@RequestParam("file")MultipartFile file,
                                                  @RequestParam(value = "startRow") Integer startRow) {
        return fileService.parseFile(file, startRow);
    }


    @PostMapping("/generateJSON")
    public String handleXmlFile(@RequestParam("file")MultipartFile file){
        return fileService.generateJSON(file);
    }

}
