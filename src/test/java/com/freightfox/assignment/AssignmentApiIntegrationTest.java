package com.freightfox.assignment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.assignment.dto.AssignmentRequestDto;
import com.freightfox.dataset.dto.DatasetInputRequest;
import com.freightfox.dataset.dto.LaneInputDto;
import com.freightfox.dataset.dto.LaneQuoteInputDto;
import com.freightfox.dataset.dto.TransporterInputDto;
import com.freightfox.dataset.repository.DatasetSnapshotRepository;
import com.freightfox.dataset.repository.LaneQuoteRepository;
import com.freightfox.dataset.repository.LaneRepository;
import com.freightfox.dataset.repository.TransporterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class AssignmentApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DatasetSnapshotRepository snapshotRepository;

    @Autowired
    private LaneRepository laneRepository;

    @Autowired
    private TransporterRepository transporterRepository;

    @Autowired
    private LaneQuoteRepository quoteRepository;

    @BeforeEach
    void cleanUp() {
        quoteRepository.deleteAll();
        laneRepository.deleteAll();
        transporterRepository.deleteAll();
        snapshotRepository.deleteAll();
    }

    @Test
    @DisplayName("REQ-15: Returns HTTP 404 when no active dataset has been ingested yet")
    void testAssignmentFailsWhenNoActiveDataset() throws Exception {
        AssignmentRequestDto request = new AssignmentRequestDto(3);

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", is("No active dataset snapshot found. Please submit dataset input first.")));
    }

    @Test
    @DisplayName("REQ-11 & REQ-15: Verified 9-Lane Benchmark Fixture via REST API (maxTransporters=3 -> 134876, T1, T4, T6)")
    void testNineLaneBenchmarkViaRestApi() throws Exception {
        // 1. Ingest official 9-lane matrix fixture
        ClassPathResource fixtureResource = new ClassPathResource("fixtures/nine_lane_matrix.json");
        DatasetInputRequest fixtureRequest = objectMapper.readValue(fixtureResource.getInputStream(), DatasetInputRequest.class);

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(fixtureRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")));

        // 2. Call assignment API with maxTransporters = 3
        AssignmentRequestDto assignmentRequest = new AssignmentRequestDto(3);

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(assignmentRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.totalCost", is(134876.0)))
                .andExpect(jsonPath("$.selectedTransporters", contains(1, 4, 6)))
                .andExpect(jsonPath("$.assignments", hasSize(9)))
                // Verify lane assignments: Lane 1->4, Lane 2->1, Lane 3->4, Lane 4->4, Lane 5->4, Lane 6->1, Lane 7->1, Lane 8->6, Lane 9->6
                .andExpect(jsonPath("$.assignments[0].laneId", is(1)))
                .andExpect(jsonPath("$.assignments[0].transporterId", is(4)))
                .andExpect(jsonPath("$.assignments[1].laneId", is(2)))
                .andExpect(jsonPath("$.assignments[1].transporterId", is(1)))
                .andExpect(jsonPath("$.assignments[2].laneId", is(3)))
                .andExpect(jsonPath("$.assignments[2].transporterId", is(4)))
                .andExpect(jsonPath("$.assignments[3].laneId", is(4)))
                .andExpect(jsonPath("$.assignments[3].transporterId", is(4)))
                .andExpect(jsonPath("$.assignments[4].laneId", is(5)))
                .andExpect(jsonPath("$.assignments[4].transporterId", is(4)))
                .andExpect(jsonPath("$.assignments[5].laneId", is(6)))
                .andExpect(jsonPath("$.assignments[5].transporterId", is(1)))
                .andExpect(jsonPath("$.assignments[6].laneId", is(7)))
                .andExpect(jsonPath("$.assignments[6].transporterId", is(1)))
                .andExpect(jsonPath("$.assignments[7].laneId", is(8)))
                .andExpect(jsonPath("$.assignments[7].transporterId", is(6)))
                .andExpect(jsonPath("$.assignments[8].laneId", is(9)))
                .andExpect(jsonPath("$.assignments[8].transporterId", is(6)));
    }

    @Test
    @DisplayName("REQ-06: Infeasible assignment returns HTTP 422 Unprocessable Entity")
    void testInfeasibleAssignmentReturns422() throws Exception {
        // T1 only quotes Lane 1, T2 only quotes Lane 2
        // maxTransporters = 1 -> Cannot cover both lanes with 1 transporter
        DatasetInputRequest request = new DatasetInputRequest(
                List.of(new LaneInputDto(1L, "A", "B"), new LaneInputDto(2L, "C", "D")),
                List.of(
                        new TransporterInputDto(1L, "T1", List.of(new LaneQuoteInputDto(1L, new BigDecimal("1000")))),
                        new TransporterInputDto(2L, "T2", List.of(new LaneQuoteInputDto(2L, new BigDecimal("2000"))))
                )
        );

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AssignmentRequestDto(1))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("Reject invalid maxTransporters < 1 with HTTP 400")
    void testInvalidMaxTransportersRejected() throws Exception {
        mockMvc.perform(post("/api/v1/transporters/assignment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"maxTransporters\": 0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }
}
