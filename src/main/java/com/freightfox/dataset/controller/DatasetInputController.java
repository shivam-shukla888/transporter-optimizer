package com.freightfox.dataset.controller;

import com.freightfox.common.exception.ErrorResponse;
import com.freightfox.dataset.dto.DatasetInputRequest;
import com.freightfox.dataset.dto.DatasetInputResponse;
import com.freightfox.dataset.service.DatasetSnapshotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for submitting and atomically activating complete dataset snapshots
 * containing lanes, transporters, and lane quotes.
 */
@RestController
@RequestMapping("/api/v1/transporters")
@Tag(name = "Transporters Input API", description = "Endpoints for ingesting lane and transporter quote datasets")
public class DatasetInputController {

    private final DatasetSnapshotService datasetSnapshotService;

    public DatasetInputController(DatasetSnapshotService datasetSnapshotService) {
        this.datasetSnapshotService = datasetSnapshotService;
    }

    @PostMapping("/input")
    @Operation(
            summary = "Submit and activate dataset snapshot",
            description = "Accepts a complete dataset of shipping lanes, transporters, and transporter-to-lane quotes. " +
                    "Validates the entire payload, atomically activates the new dataset snapshot in a database transaction, " +
                    "and deactivates any previous snapshot."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Dataset saved and activated successfully",
            content = @Content(schema = @Schema(implementation = DatasetInputResponse.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid payload, duplicate IDs, missing references, or negative quotes",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<DatasetInputResponse> submitInputData(@Valid @RequestBody DatasetInputRequest request) {
        DatasetInputResponse response = datasetSnapshotService.saveAndActivateDataset(request);
        return ResponseEntity.ok(response);
    }
}
