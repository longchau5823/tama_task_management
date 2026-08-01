package com.hutech.tama.controller.api;

import com.hutech.tama.dto.request.PasswordChangeRequest;
import com.hutech.tama.dto.request.ProfileUpdateRequest;
import com.hutech.tama.dto.response.UserResponse;
import com.hutech.tama.service.ProfileAvatarService;
import com.hutech.tama.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ProfileAvatarService profileAvatarService;

    public ProfileController( ProfileService profileService, ProfileAvatarService profileAvatarService ) {
        this.profileService = profileService;
        this.profileAvatarService = profileAvatarService;
    }

    @PatchMapping
    public UserResponse updateProfile(@Valid @RequestBody ProfileUpdateRequest request) {
        return profileService.updateEmail(request);
    }

    @PatchMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@Valid @RequestBody PasswordChangeRequest request) {
        profileService.changePassword(request);
    }

    @PostMapping("/avatar")
    public UserResponse updateAvatar(@RequestParam("file") MultipartFile file) {
        return profileAvatarService.updateAvatar(file);
    }

    @DeleteMapping("/avatar")
    public UserResponse resetAvatar() {
        return profileAvatarService.resetAvatar();
    }

    @GetMapping("/avatar/files/{fileName:.+}")
    public ResponseEntity<?> getAvatar(@PathVariable String fileName) {
        ProfileAvatarService.StoredAvatar avatar = profileAvatarService.load(fileName);
        return ResponseEntity.ok()
                .contentType(avatar.mediaType())
                .cacheControl(CacheControl.maxAge(1, TimeUnit.DAYS).cachePrivate())
                .body(avatar.resource());
    }
}
