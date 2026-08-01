package com.hutech.tama.controller.api;

import com.hutech.tama.dto.response.BoardBackgroundUploadResponse;
import com.hutech.tama.service.BoardBackgroundService;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/board-backgrounds")
public class BoardBackgroundController {

    private final BoardBackgroundService boardBackgroundService;

    public BoardBackgroundController(BoardBackgroundService boardBackgroundService) {
        this.boardBackgroundService = boardBackgroundService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BoardBackgroundUploadResponse upload(@RequestParam("file") MultipartFile file) {
        return boardBackgroundService.upload(file);
    }

    @GetMapping("/files/{ownerId}/{fileName:.+}")
    public ResponseEntity<?> getBackground(
            @PathVariable Long ownerId,
            @PathVariable String fileName
    ) {
        BoardBackgroundService.StoredBoardBackground background =
                boardBackgroundService.load(ownerId, fileName);
        return ResponseEntity.ok()
                .contentType(background.mediaType())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                .body(background.resource());
    }

    @DeleteMapping("/files/{fileName:.+}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUnused(@PathVariable String fileName) {
        boardBackgroundService.deleteUnused(fileName);
    }
}
