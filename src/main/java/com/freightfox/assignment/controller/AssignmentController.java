package com.freightfox.assignment.controller;

import com.freightfox.assignment.dto.AssignmentRequestDto;
import com.freightfox.assignment.dto.AssignmentResponseDto;
import com.freightfox.assignment.service.TransporterAssignmentService;
import com.freightfox.common.exception.ErrorResponse;
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
 * REST controller for solving and retrieving optimal transporter assignments on shipping lanes.
 */
@RestController
@RequestMapping("/api/v1/transporters")
@Tag(name = "Transporters Assignment API", description = "Endpoints for computing optimal lane-to-transporter allocations")
public class AssignmentController {

    private final TransporterAssignmentService assignmentService;

    public AssignmentController(TransporterAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @PostMapping("/assignment")
    @Operation(
            summary = "Compute optimal transporter assignment",
            description = "Reads the active dataset snapshot and applies lexicographic optimization: " +
                    "maximizes distinct transporters used (capped at maxTransporters), and among maximal solutions, " +
                    "minimizes total transportation cost. Every lane is assigned to exactly one transporter, and every " +
                    "selected transporter is used at least once."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Optimal assignment computed successfully",
            content = @Content(schema = @Schema(implementation = AssignmentResponseDto.class))
    )
    @ApiResponse(
            responseCode = "400",
            description = "Invalid request payload or maxTransporters < 1",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "404",
            description = "No active dataset found (dataset input has not been submitted yet)",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    @ApiResponse(
            responseCode = "422",
            description = "Assignment is infeasible with submitted quotes",
            content = @Content(schema = @Schema(implementation = ErrorResponse.class))
    )
    public ResponseEntity<AssignmentResponseDto> getOptimizedAssignment(@Valid @RequestBody AssignmentRequestDto request) {
        AssignmentResponseDto response = assignmentService.optimizeAssignment(request.maxTransporters());
        return ResponseEntity.ok(response);
    }
}
