package com.hutech.tama.controller.api;

import com.hutech.tama.dto.request.LabelCreateRequest;
import com.hutech.tama.dto.request.LabelUpdateRequest;
import com.hutech.tama.dto.response.LabelResponse;
import com.hutech.tama.service.LabelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/labels")
public class LabelController {

    private final LabelService labelService;

    public LabelController(LabelService labelService) {
        this.labelService = labelService;
    }

    @GetMapping
    public List<LabelResponse> getLabels() {
        return labelService.getLabels();
    }

    @GetMapping("/{labelId}")
    public LabelResponse getLabel(@PathVariable Long labelId) {
        return labelService.getLabel(labelId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public LabelResponse createLabel(@Valid @RequestBody LabelCreateRequest request) {
        return labelService.createLabel(request);
    }

    @PutMapping("/{labelId}")
    public LabelResponse updateLabel( @PathVariable Long labelId, @Valid @RequestBody LabelUpdateRequest request ) {
        return labelService.updateLabel(labelId, request);
    }

    @DeleteMapping("/{labelId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteLabel(@PathVariable Long labelId) {
        labelService.deleteLabel(labelId);
    }
}
