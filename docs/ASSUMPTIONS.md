# Assignment Assumptions & Ambiguity Disclosures

This document records the confirmed interpretations, API contracts, objective precedence, and disclosures of discrepancies identified in the FreightFox assignment brief and reference fixtures.

---

## 1. Optimization Objective & Precedence

### Background on Brief Ambiguity
The original assignment PDF lists cost minimization before transporter utilization in its problem overview, but leaves the strict lexicographic hierarchy ambiguous. In consultation with the assignment requirements, the strict lexicographic precedence is documented as follows:

1. **Priority 1 (Primary)**: Maximize the number of distinct transporters actually used, up to the user-supplied `maxTransporters` ceiling.
2. **Priority 2 (Secondary)**: Among all feasible solutions achieving that maximal distinct transporter count, minimize the total transportation cost.

### Inviolable Hard Constraints
- **Exact Lane Coverage**: Every lane must receive exactly one transporter assignment.
- **Transporter Utilization Integrity**: Every transporter listed in `selectedTransporters` must be assigned to at least one lane.
- **Quote Grounding**: Transporter $T$ is eligible for Lane $L$ if and only if an explicit, valid quote $(L, T)$ was submitted in the active dataset snapshot. Never fabricate, interpolate, or default missing quotes.
- **Selected Transporter Exact Match**: The `selectedTransporters` array in the response must match the exact set of distinct transporter IDs present in the final lane assignments.
- **Infeasible Handling**: If no complete assignment can be formed (e.g. some lane has no eligible quotes, or total available transporters cannot fulfill the coverage), the system must reject or return an explicit infeasible status rather than generating partial or corrupted assignments.

---

## 2. API Contract Specification

### Input Endpoint
- **Route**: `POST /api/v1/transporters/input`
- **Request Format**:
  ```json
  {
    "lanes": [
      {
        "id": 1,
        "origin": "Mumbai",
        "destination": "Delhi"
      }
    ],
    "transporters": [
      {
        "id": 1,
        "name": "Transporter T1",
        "laneQuotes": [
          {
            "laneId": 1,
            "quote": 5000
          }
        ]
      }
    ]
  }
  ```
- **Success Response**:
  ```json
  {
    "status": "success",
    "message": "Input data saved successfully."
  }
  ```

### Assignment Endpoint
- **Route**: `POST /api/v1/transporters/assignment`
- **Request Format**:
  ```json
  {
    "maxTransporters": 3
  }
  ```
- **Response Format**:
  ```json
  {
    "status": "success",
    "totalCost": 134876,
    "selectedTransporters": [1, 4, 6],
    "assignments": [
      { "laneId": 1, "transporterId": 4 },
      { "laneId": 2, "transporterId": 1 }
    ]
  }
  ```
- **Field Conventions**:
  - `status`: String (`"success"` or failure indicator).
  - `totalCost`: Numeric sum of quoted amounts across all assigned lanes.
  - `selectedTransporters`: List of transporter IDs (`Long`/`Integer`).
  - `assignments`: List of objects containing `laneId` and `transporterId`. Per-assignment cost fields are excluded to strictly honor the base assignment contract.

---

## 3. Discrepancies in the Original Assignment PDF

We formally disclose the following issues discovered in the original assignment PDF samples:

1. **Test Case 1 Discrepancies**:
   - **Published totalCost**: `75000`
   - **Sum of displayed assignments**: `69001` (arithmetic mismatch of `5999`).
   - **Published `selectedTransporters`**: `[4, 6, 3]`
   - **Contradiction**: Lane 3 is assigned to Transporter `2`, which is completely absent from `selectedTransporters` (violating utilization consistency).
   - **Independent Mathematical Optimum**: For the 5-lane sample, our verified solver under our agreed objective yields total cost `58139` with transporters `T1, T4, T5` (Lane 1 -> T4: 11000, Lane 2 -> T1: 10000, Lane 3 -> T4: 13000, Lane 4 -> T5: 11139, Lane 5 -> T1: 13000).

2. **Initial API Contract Example Discrepancies**:
   - In the initial API sample in the PDF, assignments are listed for lane-transporter pairs where no quote was submitted in the corresponding input data.
   - **Resolution**: Under no circumstances will our engine fabricate quotes or relax quote grounding. Assignments without submitted quotes are strictly invalid.

---

## 4. Verified 9-Lane Benchmark Fixture

A 9-lane, 7-transporter quote matrix fixture is established in `src/test/resources/fixtures/nine_lane_matrix.json` (and `.csv`):

- **Lanes**: 9 lanes (IDs 1 through 9).
- **Transporters**: 7 transporters (IDs 1 through 7, representing T1 through T7).
- **Expected Optimum for `maxTransporters = 3`**:
  - **Total Cost**: `134876`
  - **Selected Transporters**: `[1, 4, 6]` (T1, T4, T6)
  - **Lane Assignments**:
    - Lane 1 $\rightarrow$ T4 (14400)
    - Lane 2 $\rightarrow$ T1 (10512)
    - Lane 3 $\rightarrow$ T4 (14316)
    - Lane 4 $\rightarrow$ T4 (10678)
    - Lane 5 $\rightarrow$ T4 (13032)
    - Lane 6 $\rightarrow$ T1 (13567)
    - Lane 7 $\rightarrow$ T1 (10015)
    - Lane 8 $\rightarrow$ T6 (10492)
    - Lane 9 $\rightarrow$ T6 (37864)
  - **Sum of Assigned Quotes**:
    $14400 + 10512 + 14316 + 10678 + 13032 + 13567 + 10015 + 10492 + 37864 = 134876$.
    Every selected transporter (T1, T4, T6) is used at least once, exactly 3 transporters are used, all 9 lanes are assigned, and the cost is minimal.

---

## 5. Persistence & Concurrency Assumptions

- **Snapshot Scope**: Each valid `POST /api/v1/transporters/input` creates an immutable `DatasetSnapshot`.
- **Atomic Activation**: Snapshot activation occurs inside a database transaction (`active = true`, previous active set to `false`).
- **Consistent Reads**: Assignment requests read the currently active snapshot within a repeatable/read-committed transaction to avoid mixing data across concurrent updates.
- **Monetary Precision**: Quotes are non-negative numeric quantities. Calculations in the engine use `BigDecimal` or exact 64-bit integers (`long` minor units) to avoid floating-point errors.
