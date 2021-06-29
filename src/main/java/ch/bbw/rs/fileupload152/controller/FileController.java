package ch.bbw.rs.fileupload152.controller;

import ch.bbw.rs.fileupload152.model.File;
import ch.bbw.rs.fileupload152.response.ResponseFile;
import ch.bbw.rs.fileupload152.response.ResponseMessage;
import ch.bbw.rs.fileupload152.service.FileService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@CrossOrigin("http://localhost:3000")
public class FileController {
    private final FileService storageService;

    public FileController(FileService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/upload")
    public ResponseEntity<ResponseMessage> uploadFile(@RequestParam("file") MultipartFile[] files) {
        String message = "Uploaded the files successfully!";
        HttpStatus httpStatus = HttpStatus.OK;

        for (MultipartFile file : files) {
            try {
                storageService.store(file);
            } catch (Exception e) {
                message = "Could not upload all the files!";
                httpStatus = HttpStatus.EXPECTATION_FAILED;
            }
        }

        return ResponseEntity
                .status(httpStatus)
                .body(new ResponseMessage(message));
    }

    @GetMapping("/images")
    public ResponseEntity<List<ResponseFile>> getListFiles() {
        List<ResponseFile> files = storageService.getAllFiles().map(file -> {
            String fileDownloadUri = ServletUriComponentsBuilder
                    .fromCurrentContextPath()
                    .path("/images/")
                    .path(Integer.toString(file.getId()))
                    .toUriString();

            return new ResponseFile(
                    file.getName(),
                    fileDownloadUri,
                    file.getType(),
                    file.getData().length);
        }).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.OK).body(files);
    }

    @GetMapping("/images/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable int id) {
        File file = storageService.getFile(id);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getName() + "\"")
                .body(file.getData());
    }
}