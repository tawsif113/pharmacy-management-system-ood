# Supplier, Customer, and Prescription Master Data Implementation Plan

> **For Hermes:** Implement this plan task-by-task with TDD and keep the controllers thin.

**Goal:** Add the next backend feature group: supplier management, customer management, and prescription management, all as clean master-data APIs that the sales and procurement flows can depend on later.

**Architecture:**
We will keep the same backend shape already used by the product catalog feature: domain entities remain the source of business meaning, services perform validation/orchestration, repositories persist, and controllers only translate HTTP to service calls. Because this feature group is not a single workflow but three related master-data slices, we will implement them as three small resource modules rather than one large service. Prescription will be slightly richer than supplier/customer because it has a foreign key to customer and a verification workflow.

The design goal is simple: create stable master records now so procurement, sales, and prescription enforcement can build on top of them later without refactoring the foundations again.

**Tech Stack:** Spring Boot, Spring Web, Spring Data JPA, Hibernate, Jakarta Validation, JUnit 5, Mockito.

---

## System design decisions to lock in before coding

1. Keep one resource per aggregate-ish concept.
   - Supplier resource
   - Customer resource
   - Prescription resource

2. Keep the API surface aligned with the existing spec in `docs/api-endpoints.md`.
   - Supplier: list, create, get by id, patch
   - Customer: list, create, get by id, patch
   - Prescription: list, create, get by id, patch, verify, reject

3. Use thin controllers and explicit command/response records.
   - Requests go into command records.
   - Responses come out as immutable DTO records.
   - No controller should contain validation or business rules.

4. Keep validation in the service layer where it affects business meaning.
   - Supplier: require name, trim optional text fields, prevent blank names.
   - Customer: require name, trim optional fields, allow optional DOB.
   - Prescription: require customer, doctor name, issue date, expiry date, and a valid status transition.

5. Do not add frontend code.
   - This is backend-only work.

6. Do not over-engineer.
   - No generic CRUD framework.
   - No abstract master-data base service.
   - No “future-proof” layers unless they solve a concrete problem now.

---

## Task 1: Inspect and lock the current model shape

**Objective:** Confirm the existing supplier/customer/prescription entities and repositories match the intended API behavior before writing service code.

**Files:**
- Read: `src/main/java/com/arima/pms/domain/entity/Supplier.java`
- Read: `src/main/java/com/arima/pms/domain/entity/Customer.java`
- Read: `src/main/java/com/arima/pms/domain/entity/Prescription.java`
- Read: `src/main/java/com/arima/pms/repository/SupplierRepository.java`
- Read: `src/main/java/com/arima/pms/repository/CustomerRepository.java`
- Read: `src/main/java/com/arima/pms/repository/PrescriptionRepository.java`
- Read: `docs/api-endpoints.md`
- Read: `docs/context.md`

**Checks to make:**
- Supplier has `name`, `phone`, `email`, `address`, `active`.
- Customer has `name`, `phone`, `address`, `dateOfBirth`, `notes`.
- Prescription has `customer`, `doctorName`, `doctorRegistrationNo`, `issueDate`, `expiryDate`, `fileUrl`, `verificationStatus`.
- The API spec includes the exact endpoints we need.

**Outcome:** A locked mental model for the service layer so we do not invent fields or behavior that the schema does not support.

---

## Task 2: Add supplier repository search support if needed

**Objective:** Make the supplier list endpoint able to search by name, phone, and email without manual filtering in controllers.

**Files:**
- Modify: `src/main/java/com/arima/pms/repository/SupplierRepository.java`
- Test: `src/test/java/com/arima/pms/service/SupplierServiceTest.java`

**Design:**
Use `JpaSpecificationExecutor<Supplier>` or a small equivalent query strategy so the service can compose search conditions cleanly.

**Behavior to support:**
- `GET /suppliers?search=...&active=...`
- Search should match name, phone, or email.
- If no search term is provided, return the normal paged list.

**Verification:**
- Create tests that prove search finds a supplier by name and active filtering works.
- Run the supplier service tests only first, then the full suite.

---

## Task 3: Implement supplier master data service and controller

**Objective:** Add supplier CRUD-lite APIs: list, create, get, update.

**Files to create:**
- `src/main/java/com/arima/pms/service/command/CreateSupplierCommand.java`
- `src/main/java/com/arima/pms/service/command/UpdateSupplierCommand.java`
- `src/main/java/com/arima/pms/web/dto/SupplierResponse.java`
- `src/main/java/com/arima/pms/service/SupplierService.java`
- `src/main/java/com/arima/pms/web/controller/SupplierController.java`
- `src/test/java/com/arima/pms/service/SupplierServiceTest.java`
- `src/test/java/com/arima/pms/web/controller/SupplierControllerTest.java`

**Behavior to implement:**
- `POST /suppliers` creates a supplier.
- `GET /suppliers` returns a page of suppliers.
- `GET /suppliers/{id}` returns one supplier.
- `PATCH /suppliers/{id}` updates editable fields.
- No hard delete.
- `active` is the deactivation switch.

**Validation rules:**
- `name` is required on create.
- `name` cannot be blank on update.
- Optional strings should be trimmed and blank strings normalized to null where appropriate.

**Testing plan:**
- Create success test.
- Create validation test for blank name.
- Update success test.
- Get-by-id not found test.
- Controller delegation test for each endpoint.

**Verification command:**
- `./gradlew test --tests '*Supplier*'`
- then `./gradlew test`

---

## Task 4: Add customer repository search support if needed

**Objective:** Make the customer list endpoint searchable without bloating the controller.

**Files:**
- Modify: `src/main/java/com/arima/pms/repository/CustomerRepository.java`
- Test: `src/test/java/com/arima/pms/service/CustomerServiceTest.java`

**Design:**
Support search by customer name and phone, and optionally DOB filtering if the API layer exposes it.

**Behavior to support:**
- `GET /customers?search=...&phone=...&dateOfBirth=...`
- Search should behave predictably for partial name/phone matches.

**Verification:**
- Add at least one repository-backed or service-backed test proving the search path works.

---

## Task 5: Implement customer master data service and controller

**Objective:** Add customer CRUD-lite APIs for patient profiles.

**Files to create:**
- `src/main/java/com/arima/pms/service/command/CreateCustomerCommand.java`
- `src/main/java/com/arima/pms/service/command/UpdateCustomerCommand.java`
- `src/main/java/com/arima/pms/web/dto/CustomerResponse.java`
- `src/main/java/com/arima/pms/service/CustomerService.java`
- `src/main/java/com/arima/pms/web/controller/CustomerController.java`
- `src/test/java/com/arima/pms/service/CustomerServiceTest.java`
- `src/test/java/com/arima/pms/web/controller/CustomerControllerTest.java`

**Behavior to implement:**
- `POST /customers` creates a customer profile.
- `GET /customers` lists/searches customers.
- `GET /customers/{id}` returns customer detail.
- `PATCH /customers/{id}` updates editable fields.
- No hard delete.

**Validation rules:**
- `name` is required on create.
- `dateOfBirth` is optional.
- Optional string fields should be trimmed.
- Do not invent lifecycle states here; customers are simple master data.

**Testing plan:**
- Create/list/get/update tests.
- Blank-name validation test.
- Not-found test.
- Controller delegation test.

**Verification command:**
- `./gradlew test --tests '*Customer*'`
- then `./gradlew test`

---

## Task 6: Add prescription repository search support if needed

**Objective:** Prepare the prescription list endpoint for filtering by customer and verification state.

**Files:**
- Modify: `src/main/java/com/arima/pms/repository/PrescriptionRepository.java`
- Test: `src/test/java/com/arima/pms/service/PrescriptionServiceTest.java`

**Design:**
Use composable query support so the service can filter by customerId and verificationStatus cleanly.

**Behavior to support:**
- `GET /prescriptions?customerId=...&verificationStatus=...&expiryBefore=...&expiryAfter=...`

**Verification:**
- Tests proving at least one filter path works.

---

## Task 7: Implement prescription master data service and controller

**Objective:** Add the prescription lifecycle needed for sale validation.

**Files to create:**
- `src/main/java/com/arima/pms/service/command/CreatePrescriptionCommand.java`
- `src/main/java/com/arima/pms/service/command/UpdatePrescriptionCommand.java`
- `src/main/java/com/arima/pms/web/dto/PrescriptionResponse.java`
- `src/main/java/com/arima/pms/service/PrescriptionService.java`
- `src/main/java/com/arima/pms/web/controller/PrescriptionController.java`
- `src/test/java/com/arima/pms/service/PrescriptionServiceTest.java`
- `src/test/java/com/arima/pms/web/controller/PrescriptionControllerTest.java`

**Behavior to implement:**
- `POST /prescriptions` creates a prescription.
- `GET /prescriptions` lists/searches prescriptions.
- `GET /prescriptions/{id}` returns detail.
- `PATCH /prescriptions/{id}` updates metadata.
- `POST /prescriptions/{id}/verify` marks a prescription verified.
- `POST /prescriptions/{id}/reject` marks a prescription rejected.

**Validation rules:**
- The referenced customer must exist.
- `doctorName` is required.
- `issueDate` and `expiryDate` are required.
- `expiryDate` must be on or after `issueDate`.
- Default verification status should be pending unless explicitly set.
- Only valid state transitions are allowed for verify/reject.

**Testing plan:**
- Create success test with real customer lookup mocked.
- Reject invalid date range.
- Verify transition updates status.
- Reject transition updates status.
- Not-found tests.
- Controller delegation tests.

**Verification command:**
- `./gradlew test --tests '*Prescription*'`
- then `./gradlew test`

---

## Task 8: Document the feature group

**Objective:** Write one feature-level doc that explains what supplier/customer/prescription master data does in the system.

**Files to create:**
- `docs/features/master-data-suppliers-customers-prescriptions.md`

**Doc should cover:**
- What each resource is for
- What endpoints were added
- What business rules are enforced
- How this feature group supports procurement and sales later
- Any key design decisions

**Verification:**
- Ensure the doc matches the implemented API and business rules exactly.

---

## Execution order

1. Confirm model shape
2. Add repository search support where needed
3. Build supplier service/controller/tests
4. Build customer service/controller/tests
5. Build prescription service/controller/tests
6. Add the grouped feature doc
7. Run the full test suite
8. Smoke-test the endpoints in Postman or curl
9. Commit and push to GitHub

---

## Definition of done

This feature group is done when:
- supplier endpoints work
- customer endpoints work
- prescription endpoints work
- tests pass
- the feature doc exists
- the code is still thin-controller / service-orchestrated / repository-persisted
- no frontend code has been started yet

---

## Verification checklist

- [ ] `./gradlew test` passes
- [ ] supplier create/list/get/update works
- [ ] customer create/list/get/update works
- [ ] prescription create/list/get/update/verify/reject works
- [ ] invalid date range is rejected for prescriptions
- [ ] missing required fields are rejected
- [ ] endpoint behavior matches `docs/api-endpoints.md`
- [ ] feature doc is written
- [ ] changes are committed and pushed
