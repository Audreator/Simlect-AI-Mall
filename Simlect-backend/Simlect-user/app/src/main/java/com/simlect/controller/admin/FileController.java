package com.simlect.controller.admin;

import com.simlect.constants.Constants;
import com.simlect.entity.config.AppConfig;
import com.simlect.entity.vo.ResponseVO;
import com.simlect.utils.FileUtils;
import com.simlect.utils.StringTools;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@RestController("adminFileController")
@RequestMapping("/admin/file")
@Validated
@Slf4j
public class FileController extends com.simlect.controller.admin.ABaseController{

    @Resource
    private AppConfig appConfig;

    @Resource
    private FileUtils fileUtils;

    @PostMapping("/uploadImage")
    public ResponseVO uploadImage(@NotNull MultipartFile file, Boolean createThumbnail) {
        String filePath = fileUtils.uploadImage(file, createThumbnail);
        return getSuccessResponseVO(filePath);
    }

    @GetMapping("/getResource")
    public void getResource(HttpServletResponse response, @NotNull String sourceName) throws IOException {
        if(!StringTools.pathIsOK(sourceName)){
            return;
        }
        String suffix = StringTools.getFileSuffix(sourceName);
        response.setContentType(resolveImageContentType(suffix));
        response.setHeader("Cache-Control", "max-age=100000");
        readFile(response,sourceName);
    }

    protected void readFile(HttpServletResponse response,String filePath){
        if(!StringTools.pathIsOK(filePath)){
            return;
        }
        File file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + filePath);
        if(!file.exists()){
            String fallbackPath = filePath.replace("_thumbnail", "");
            if (!fallbackPath.equals(filePath)) {
                file = new File(appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE + fallbackPath);
            }
        }
        if(!file.exists()){
            return;
        }
        try (OutputStream out = response.getOutputStream();
            FileInputStream in = new FileInputStream(file)) {
                byte[] byteData =  new byte[1024];
                int len = 0;
                while((len = in.read(byteData)) != -1){
                    out.write(byteData,0,len);
                }
                out.flush();
        } catch (Exception e) {
            log.error("读取文件异常");
        }
    }

    private static String resolveImageContentType(String suffix) {
        if (StringTools.isEmpty(suffix)) {
            return "image/jpeg";
        }
        return switch (suffix.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".webp" -> "image/webp";
            case ".bmp" -> "image/bmp";
            default -> "image/jpeg";
        };
    }
}
