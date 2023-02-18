package com.partner.boot.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.partner.boot.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;

/**
 * 文件处理相关接口
 */
@RestController
@RequestMapping("/file")
@Slf4j
public class FileController {
    private static final String FILES_DIR = "/files/";

    @Value("${file.upload.path:}")
    private String uploadPath;
    @Value("${server.port:9090}")
    private String port;
    @Value("${file.download.ip:localhost}")
    private String downloadIp;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    public Result upload( MultipartFile file) {
//        String fileDir = System.getProperty("user.dir");
        String originalFilename = file.getOriginalFilename();
        String fileName = FileUtil.mainName(originalFilename);    //文件主名称
        String extName = FileUtil.extName(originalFilename);      //文件后缀名
        String uniFileFlag = IdUtil.fastSimpleUUID();
        String fileFullName = uniFileFlag + StrUtil.DOT + extName;
        String fileUploadPath = getFileUploadPath(fileFullName);
        long size = file.getSize();
//        byte[] bytes = file.getBytes();
        String name = file.getName();
//        log.info("{}, {}, {}", originalFilename, size, name);

        try {
            File uploadFile = new File(fileUploadPath);
            File parentFile = uploadFile.getParentFile();
            if(!parentFile.exists()) {
                parentFile.mkdirs();
            }
            file.transferTo(uploadFile);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败");
        }
        return Result.success("http://"+ downloadIp + ":" + port + "/file/download/" + fileFullName);
    }

    /**
     * 文件下载
     * @param fileFullName
     * @param response
     * @throws IOException
     */
    @GetMapping("/download/{fileFullName}")
    public void downloadFile(@PathVariable String fileFullName, @RequestParam(required = false) String loginId, @RequestParam(required = false) String token, HttpServletResponse response) throws IOException {
        //token校验
//        List<String> tokenList = StpUtil.getTokenValueListByLoginId(loginId);
//        if(CollUtil.isEmpty(tokenList) || !tokenList.contains(token)) {
//            return;
//        }
        String extName = FileUtil.extName(fileFullName);
        String fileUploadPath = getFileUploadPath(fileFullName);
        byte[] bytes = FileUtil.readBytes(fileUploadPath);
        //解决乱码
        response.addHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(fileFullName, "UTF-8"));
        List<String> attachmentFileExtNames = CollUtil.newArrayList("docx", "doc", "xlsx", "xls", "mp4", "mpm3");
        if(attachmentFileExtNames.contains(extName)) {
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileFullName, "UTF-8"));
        }
        OutputStream os = response.getOutputStream();
        os.write(bytes);
        os.flush();
        os.close();
    }

    /**
     * 获取文件完整路径
     * @param fileFullName
     * @return
     */
    private String getFileUploadPath(String fileFullName) {
        if(StrUtil.isBlank(uploadPath)) {
            uploadPath = System.getProperty("user.dir");
        }
        return uploadPath + FILES_DIR + fileFullName; //完整文件路径
    }
}
