//package com.example.friend.controller.question.file;
//
//
//import com.example.commom.core.controller.BaseController;
//import com.example.friend.service.file.IFileService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestBody;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//@RestController
//@RequestMapping("/file")
//public class FileController extends BaseController {
//
//    @Autowired
//    private IFileService fileService;
//
//    @PostMapping("/upload")
//    public R<OSSResult> upload(@RequestBody MultipartFile file) {
//        return R.ok(fileService.upload(file));
//    }
//}
