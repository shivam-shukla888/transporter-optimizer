package com.freightfox.dataset;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.freightfox.dataset.dto.DatasetInputRequest;
import com.freightfox.dataset.dto.LaneInputDto;
import com.freightfox.dataset.dto.LaneQuoteInputDto;
import com.freightfox.dataset.dto.TransporterInputDto;
import com.freightfox.dataset.entity.DatasetSnapshotEntity;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class DatasetInputApiIntegrationTest {

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
    @DisplayName("REQ-14: POST /api/v1/transporters/input successfully persists and activates snapshot")
    void testValidInputDatasetSubmission() throws Exception {
        DatasetInputRequest request = new DatasetInputRequest(
                List.of(
                        new LaneInputDto(1L, "Mumbai", "Delhi"),
                        new LaneInputDto(2L, "Chennai", "Kolkata")
                ),
                List.of(
                        new TransporterInputDto(1L, "Transporter T1", List.of(
                                new LaneQuoteInputDto(1L, new BigDecimal("5000")),
                                new LaneQuoteInputDto(2L, new BigDecimal("7000"))
                        )),
                        new TransporterInputDto(2L, "Transporter T2", List.of(
                                new LaneQuoteInputDto(1L, new BigDecimal("4800")),
                                new LaneQuoteInputDto(2L, new BigDecimal("7200"))
                        ))
                )
        );

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("success")))
                .andExpect(jsonPath("$.message", is("Input data saved successfully.")));

        Optional<DatasetSnapshotEntity> activeSnapshotOpt = snapshotRepository.findByActiveTrue();
        assertThat(activeSnapshotOpt).isPresent();
        DatasetSnapshotEntity snapshot = activeSnapshotOpt.get();
        assertThat(snapshot.getTotalLanes()).isEqualTo(2);
        assertThat(snapshot.getTotalTransporters()).isEqualTo(2);
        assertThat(snapshot.getTotalQuotes()).isEqualTo(4);

        assertThat(laneRepository.findBySnapshotIdOrderByLaneIdAsc(snapshot.getId())).hasSize(2);
        assertThat(transporterRepository.findBySnapshotIdOrderByTransporterIdAsc(snapshot.getId())).hasSize(2);
        assertThat(quoteRepository.findBySnapshotId(snapshot.getId())).hasSize(4);
    }

    @Test
    @DisplayName("Reject input submission with duplicate lane IDs")
    void testDuplicateLaneIdRejected() throws Exception {
        DatasetInputRequest request = new DatasetInputRequest(
                List.of(
                        new LaneInputDto(1L, "Mumbai", "Delhi"),
                        new LaneInputDto(1L, "Mumbai", "Kolkata") // duplicate
                ),
                List.of(
                        new TransporterInputDto(1L, "T1", List.of(new LaneQuoteInputDto(1L, new BigDecimal("5000"))))
                )
        );

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Duplicate lane ID encountered: 1")));
    }

    @Test
    @DisplayName("Reject input submission with duplicate transporter IDs")
    void testDuplicateTransporterIdRejected() throws Exception {
        DatasetInputRequest request = new DatasetInputRequest(
                List.of(new LaneInputDto(1L, "Mumbai", "Delhi")),
                List.of(
                        new TransporterInputDto(1L, "T1", List.of(new LaneQuoteInputDto(1L, new BigDecimal("5000")))),
                        new TransporterInputDto(1L, "T1 duplicate", List.of(new LaneQuoteInputDto(1L, new BigDecimal("6000"))))
                )
        );

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("Duplicate transporter ID encountered: 1")));
    }

    @Test
    @DisplayName("Reject input submission where quote references non-existent lane")
    void testQuoteForNonExistentLaneRejected() throws Exception {
        DatasetInputRequest request = new DatasetInputRequest(
                List.of(new LaneInputDto(1L, "Mumbai", "Delhi")),
                List.of(
                        new TransporterInputDto(1L, "T1", List.of(
                                new LaneQuoteInputDto(999L, new BigDecimal("5000")) // non-existent lane 999
                        ))
                )
        );

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")))
                .andExpect(jsonPath("$.message", containsString("submitted quote for non-existent lane ID: 999")));
    }

    @Test
    @DisplayName("Reject input submission with negative quote amount")
    void testNegativeQuoteRejected() throws Exception {
        DatasetInputRequest request = new DatasetInputRequest(
                List.of(new LaneInputDto(1L, "Mumbai", "Delhi")),
                List.of(
                        new TransporterInputDto(1L, "T1", List.of(
                                new LaneQuoteInputDto(1L, new BigDecimal("-100.00"))
                        ))
                )
        );

        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status", is("error")));
    }

    @Test
    @DisplayName("Atomic replacement: New successful dataset activates atomically and deactivates previous")
    void testAtomicDatasetReplacement() throws Exception {
        // 1. Submit initial dataset
        DatasetInputRequest initial = new DatasetInputRequest(
                List.of(new LaneInputDto(1L, "Mumbai", "Delhi")),
                List.of(new TransporterInputDto(1L, "T1", List.of(new LaneQuoteInputDto(1L, new BigDecimal("5000")))))
        );
        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isOk());

        DatasetSnapshotEntity snapshot1 = snapshotRepository.findByActiveTrue().orElseThrow();
        assertThat(snapshot1.getTotalLanes()).isEqualTo(1);

        // 2. Submit replacement dataset with 3 lanes
        DatasetInputRequest replacement = new DatasetInputRequest(
                List.of(
                        new LaneInputDto(10L, "Pune", "Goa"),
                        new LaneInputDto(20L, "Ahmedabad", "Surat"),
                        new LaneInputDto(30L, "Jaipur", "Agra")
                ),
                List.of(new TransporterInputDto(1L, "T1", List.of(
                        new LaneQuoteInputDto(10L, new BigDecimal("2000")),
                        new LaneQuoteInputDto(20L, new BigDecimal("3000")),
                        new LaneQuoteInputDto(30L, new BigDecimal("4000"))
                )))
        );
        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replacement)))
                .andExpect(status().isOk());

        // Verify only replacement snapshot is active
        List<DatasetSnapshotEntity> activeSnapshots = snapshotRepository.findAll().stream()
                .filter(DatasetSnapshotEntity::isActive)
                .toList();
        assertThat(activeSnapshots).hasSize(1);
        DatasetSnapshotEntity snapshot2 = activeSnapshots.get(0);
        assertThat(snapshot2.getId()).isNotEqualTo(snapshot1.getId());
        assertThat(snapshot2.getTotalLanes()).isEqualTo(3);

        // Verify previous snapshot still exists in DB but is inactive
        DatasetSnapshotEntity oldSnapshot = snapshotRepository.findById(snapshot1.getId()).orElseThrow();
        assertThat(oldSnapshot.isActive()).isFalse();
    }

    @Test
    @DisplayName("Atomic replacement failure: Failed submission does not deactivate or corrupt active snapshot")
    void testFailedSubmissionPreservesActiveSnapshot() throws Exception {
        // 1. Submit valid dataset
        DatasetInputRequest initial = new DatasetInputRequest(
                List.of(new LaneInputDto(1L, "Mumbai", "Delhi")),
                List.of(new TransporterInputDto(1L, "T1", List.of(new LaneQuoteInputDto(1L, new BigDecimal("5000")))))
        );
        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(initial)))
                .andExpect(status().isOk());

        Long originalActiveId = snapshotRepository.findByActiveTrue().orElseThrow().getId();

        // 2. Submit invalid dataset (duplicate lanes)
        DatasetInputRequest invalid = new DatasetInputRequest(
                List.of(new LaneInputDto(2L, "A", "B"), new LaneInputDto(2L, "C", "D")),
                List.of(new TransporterInputDto(1L, "T1", List.of(new LaneQuoteInputDto(2L, new BigDecimal("1000")))))
        );
        mockMvc.perform(post("/api/v1/transporters/input")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        // 3. Verify original snapshot remains active and untouched
        Optional<DatasetSnapshotEntity> currentActive = snapshotRepository.findByActiveTrue();
        assertThat(currentActive).isPresent();
        assertThat(currentActive.get().getId()).isEqualTo(originalActiveId);
    }
}
