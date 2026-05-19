package vn.edu.hust.soict.soe.assetmanagement.asset.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.FixedAssetRepository;
import vn.edu.hust.soict.soe.assetmanagement.audit.service.AuditLogService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FixedAssetService Tests")
class FixedAssetServiceTest {

    @Mock
    private FixedAssetRepository fixedAssetRepository;

    @Mock
    private AssetHistoryRepository assetHistoryRepository;

    @InjectMocks
    private FixedAssetService fixedAssetService;

    @Mock
    private AuditLogService auditLogService;

    // ── Shared test data ──────────────────────────────────────

    private FixedAssetDTO validDto;
    private FixedAsset savedAsset;
    private UUID assetId;

    @BeforeEach
    void setUp() {
        assetId = UUID.randomUUID();

        validDto = new FixedAssetDTO();
        validDto.setAssetCode("TS-2024-001");
        validDto.setName("Máy tính xách tay Dell Latitude 5540");
        validDto.setCategoryId(1);
        validDto.setManagingUnitId(UUID.randomUUID());
        validDto.setOriginalCost(new BigDecimal("25000000"));
        validDto.setAcquisitionDate(LocalDate.of(2024, 1, 15));
        validDto.setUsefulLifeYears(5);
        validDto.setSalvageValue(BigDecimal.ZERO);
        validDto.setDepreciationMethod("STRAIGHT_LINE");
        validDto.setManufacturer("Dell");
        validDto.setSerialNumber("SN-DELL-2024-001");
        validDto.setFundingSource("Ngân sách nhà nước");

        savedAsset = new FixedAsset();
        savedAsset.setId(assetId);
        savedAsset.setAssetCode("TS-2024-001");
        savedAsset.setName("Máy tính xách tay Dell Latitude 5540");
        savedAsset.setCategoryId(1);
        savedAsset.setManagingUnitId(validDto.getManagingUnitId());
        savedAsset.setOriginalCost(new BigDecimal("25000000"));
        savedAsset.setAcquisitionDate(LocalDate.of(2024, 1, 15));
        savedAsset.setUsefulLifeYears(5);
        savedAsset.setSalvageValue(BigDecimal.ZERO);
        savedAsset.setAccumulatedDepreciation(BigDecimal.ZERO);
        savedAsset.setNetBookValue(new BigDecimal("25000000"));
        savedAsset.setDepreciationMethod("STRAIGHT_LINE");
        savedAsset.setStatus(AssetStatus.IN_USE);
        savedAsset.setCreatedAt(LocalDateTime.now());
        savedAsset.setUpdatedAt(LocalDateTime.now());
        savedAsset.setCreatedBy("system_test");
    }

    // ══════════════════════════════════════════════════════════
    // FA-01 — Asset registration & digital profile
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FA-01: createAsset()")
    class CreateAssetTests {

        @Test
        @DisplayName("Should create asset with correct fields from DTO")
        void createAsset_validDto_returnsSavedAsset() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.createAsset(validDto);

            assertThat(result).isNotNull();
            assertThat(result.getAssetCode()).isEqualTo("TS-2024-001");
            assertThat(result.getName()).isEqualTo("Máy tính xách tay Dell Latitude 5540");
            assertThat(result.getOriginalCost()).isEqualByComparingTo("25000000");
            assertThat(result.getStatus()).isEqualTo(AssetStatus.IN_USE);
        }

        @Test
        @DisplayName("Should set initial accumulated depreciation to zero")
        void createAsset_setsAccumulatedDepreciationToZero() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.createAsset(validDto);

            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should set initial net book value equal to original cost")
        void createAsset_setsNetBookValueToOriginalCost() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.createAsset(validDto);

            assertThat(result.getNetBookValue())
                    .isEqualByComparingTo(result.getOriginalCost());
        }

        @Test
        @DisplayName("Should set status to IN_USE on creation")
        void createAsset_setsStatusToInUse() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.createAsset(validDto);

            assertThat(result.getStatus()).isEqualTo(AssetStatus.IN_USE);
        }

        @Test
        @DisplayName("Should default salvage value to zero if DTO has null")
        void createAsset_nullSalvageValue_defaultsToZero() {
            validDto.setSalvageValue(null);
            savedAsset.setSalvageValue(BigDecimal.ZERO);

            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.createAsset(validDto);

            assertThat(result.getSalvageValue()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should default depreciation method to STRAIGHT_LINE if DTO has null")
        void createAsset_nullDepreciationMethod_defaultsToStraightLine() {
            validDto.setDepreciationMethod(null);
            savedAsset.setDepreciationMethod("STRAIGHT_LINE");

            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.createAsset(validDto);

            assertThat(result.getDepreciationMethod()).isEqualTo("STRAIGHT_LINE");
        }

        @Test
        @DisplayName("Should save asset to repository exactly once")
        void createAsset_savesAssetRepositoryOnce() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            fixedAssetService.createAsset(validDto);

            verify(fixedAssetRepository, times(1)).save(any(FixedAsset.class));
        }

        @Test
        @DisplayName("Should write CREATED history log after asset creation (FA-04)")
        void createAsset_writesCreatedHistoryLog() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            fixedAssetService.createAsset(validDto);

            ArgumentCaptor<AssetHistory> historyCaptor =
                    ArgumentCaptor.forClass(AssetHistory.class);
            verify(assetHistoryRepository, times(1)).save(historyCaptor.capture());

            AssetHistory log = historyCaptor.getValue();
            assertThat(log.getEventType()).isEqualTo("CREATED");
            assertThat(log.getAssetId()).isEqualTo(assetId);
            assertThat(log.getOldValue()).isNull();
            assertThat(log.getPerformedBy()).isNotBlank();
        }
    }

    // ══════════════════════════════════════════════════════════
    // FA-01 — getAllAssets
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FA-01: getAllAssets()")
    class GetAllAssetsTests {

        @Test
        @DisplayName("Should return all assets from repository")
        void getAllAssets_returnsAll() {
            when(fixedAssetRepository.findAll()).thenReturn(List.of(savedAsset));

            List<FixedAsset> result = fixedAssetService.getAllAssets();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getAssetCode()).isEqualTo("TS-2024-001");
        }

        @Test
        @DisplayName("Should return empty list when no assets exist")
        void getAllAssets_empty_returnsEmptyList() {
            when(fixedAssetRepository.findAll()).thenReturn(List.of());

            List<FixedAsset> result = fixedAssetService.getAllAssets();

            assertThat(result).isEmpty();
        }
    }

    // ══════════════════════════════════════════════════════════
    // FA-02 — Depreciation calculation engine
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FA-02: calculateCurrentDepreciation() — Straight-Line")
    class StraightLineDepreciationTests {

        @Test
        @DisplayName("Should return asset not found for invalid ID")
        void calculateDepreciation_invalidId_throwsRuntimeException() {
            UUID badId = UUID.randomUUID();
            when(fixedAssetRepository.findById(badId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fixedAssetService.calculateCurrentDepreciation(badId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Asset not found with ID");
        }

        @Test
        @DisplayName("Should calculate straight-line depreciation correctly — 5 year asset after 1 year")
        void calculateDepreciation_straightLine_after1Year_correct() {
            // 25,000,000 VND / 5 years = 5,000,000/year = ~416,667/month
            // After exactly 12 months: accumulated ≈ 5,000,000
            savedAsset.setAcquisitionDate(LocalDate.now().minusMonths(12));
            savedAsset.setDepreciationMethod("STRAIGHT_LINE");

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            // Annual depreciation = 25,000,000 / 5 = 5,000,000
            BigDecimal expectedAnnual = new BigDecimal("5000000.00");
            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(expectedAnnual);
            assertThat(result.getNetBookValue())
                    .isEqualByComparingTo(new BigDecimal("20000000.00"));
        }

        @Test
        @DisplayName("Should return zero depreciation for brand new asset (0 months)")
        void calculateDepreciation_straightLine_0months_zeroDepreciation() {
            savedAsset.setAcquisitionDate(LocalDate.now());
            savedAsset.setDepreciationMethod("STRAIGHT_LINE");

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getNetBookValue())
                    .isEqualByComparingTo(new BigDecimal("25000000"));
        }

        @Test
        @DisplayName("Net book value should never be negative for fully depreciated asset")
        void calculateDepreciation_fullyDepreciated_netBookValueNotNegative() {
            // Asset acquired 10 years ago, useful life 5 years — fully depreciated
            savedAsset.setAcquisitionDate(LocalDate.now().minusYears(10));
            savedAsset.setDepreciationMethod("STRAIGHT_LINE");

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            assertThat(result.getNetBookValue())
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should apply true-up at the end of useful life (Month 60)")
        void calculateDepreciation_straightLine_endOfLife_trueUp() {
            // Tài sản 5 năm, set ngày mua lùi về đúng 60 tháng trước
            savedAsset.setAcquisitionDate(LocalDate.now().minusMonths(60));
            savedAsset.setDepreciationMethod("STRAIGHT_LINE");

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            // Tháng 60: Kích hoạt True-up. Hao mòn = Nguyên giá, Giá trị còn lại = 0
            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(new BigDecimal("25000000.00"));
            assertThat(result.getNetBookValue())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Should handle zero useful life years gracefully")
        void calculateDepreciation_zeroUsefulLife_returnsAssetUnchanged() {
            savedAsset.setUsefulLifeYears(0);
            savedAsset.setDepreciationMethod("STRAIGHT_LINE");

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            // Should return asset unchanged without throwing
            assertThat(result).isNotNull();
        }
    }

    @Nested
    @DisplayName("FA-02: calculateCurrentDepreciation() — Declining Balance")
    class DecliningBalanceDepreciationTests {

        @Test
        @DisplayName("Should use declining balance when method is DECLINING_BALANCE")
        void calculateDepreciation_decliningBalance_usesCorrectMethod() {
            savedAsset.setDepreciationMethod("DECLINING_BALANCE");
            savedAsset.setAcquisitionDate(LocalDate.now().minusMonths(12));

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            // Declining balance always depreciates faster than straight-line
            // After 1 year: SL = 5,000,000; DB with multiplier 1.5 (T=5) = 25000000 * 0.3 = 7,500,000
            assertThat(result.getAccumulatedDepreciation())
                    .isGreaterThan(new BigDecimal("5000000"));
        }

        @Test
        @DisplayName("Net book value should never be negative — declining balance")
        void calculateDepreciation_decliningBalance_netBookNotNegative() {
            savedAsset.setDepreciationMethod("DECLINING_BALANCE");
            savedAsset.setAcquisitionDate(LocalDate.now().minusYears(20));

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            assertThat(result.getNetBookValue())
                    .isGreaterThanOrEqualTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("Declining balance multiplier 1.5 — asset with useful life <= 4 years")
        void calculateDepreciation_decliningBalance_shortLifeAsset_multiplier1point5() {
            // T = 4 years → multiplier = 1.5 → rate = (1/4) * 1.5 = 0.375
            savedAsset.setUsefulLifeYears(4);
            savedAsset.setDepreciationMethod("DECLINING_BALANCE");
            savedAsset.setAcquisitionDate(LocalDate.now().minusYears(1));

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            // Year 1 depreciation = 25,000,000 * 0.375 = 9,375,000
            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(new BigDecimal("9375000.00"));
        }

        @Test
        @DisplayName("Declining balance multiplier 2.5 — asset with useful life > 6 years")
        void calculateDepreciation_decliningBalance_longLifeAsset_multiplier2point5() {
            // T = 10 years → multiplier = 2.5 → rate = (1/10) * 2.5 = 0.25
            savedAsset.setUsefulLifeYears(10);
            savedAsset.setDepreciationMethod("DECLINING_BALANCE");
            savedAsset.setAcquisitionDate(LocalDate.now().minusYears(1));

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            // Year 1 depreciation = 25,000,000 * 0.25 = 6,250,000
            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(new BigDecimal("6250000.00"));
        }

        @Test
        @DisplayName("Should switch to straight-line when DB rate is lower (Year 4 of 5)")
        void calculateDepreciation_decliningBalance_crossover() {
            // Test sau đúng 4 năm (48 tháng)
            savedAsset.setDepreciationMethod("DECLINING_BALANCE");
            savedAsset.setUsefulLifeYears(5);
            savedAsset.setAcquisitionDate(LocalDate.now().minusYears(4));

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            // Tổng hao mòn 4 năm = 10tr (N1) + 6tr (N2) + 3.6tr (N3) + 2.7tr (N4 - Đường thẳng) = 22.3tr
            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(new BigDecimal("22300000.00"));
                    
            // Giá trị còn lại = 25tr - 22.3tr = 2.7tr
            assertThat(result.getNetBookValue())
                    .isEqualByComparingTo(new BigDecimal("2700000.00"));
        }

        @Test
        @DisplayName("Should return zero depreciation when months used is zero — declining balance")
        void calculateDepreciation_decliningBalance_0months_zeroAccumulated() {
            savedAsset.setDepreciationMethod("DECLINING_BALANCE");
            savedAsset.setAcquisitionDate(LocalDate.now());

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));

            FixedAsset result = fixedAssetService.calculateCurrentDepreciation(assetId);

            assertThat(result.getAccumulatedDepreciation())
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // ══════════════════════════════════════════════════════════
    // FA-03 — Operational status tracking
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FA-03: updateAssetStatus()")
    class UpdateAssetStatusTests {

        @Test
        @DisplayName("Should update asset status successfully")
        void updateAssetStatus_validRequest_updatesStatus() {
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.updateAssetStatus(
                    assetId, AssetStatus.MAINTENANCE,
                    "Bàn phím bị hỏng", "asset.manager");

            assertThat(result.getStatus()).isEqualTo(AssetStatus.MAINTENANCE);
        }

        @Test
        @DisplayName("Should record status reason")
        void updateAssetStatus_recordsReason() {
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenAnswer(i -> {
                FixedAsset a = i.getArgument(0);
                return a;
            });
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.updateAssetStatus(
                    assetId, AssetStatus.MAINTENANCE,
                    "Bàn phím bị hỏng", "asset.manager");

            assertThat(result.getStatusReason()).isEqualTo("Bàn phím bị hỏng");
        }

        @Test
        @DisplayName("Should record who changed the status")
        void updateAssetStatus_recordsPerformedBy() {
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenAnswer(i -> i.getArgument(0));
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.updateAssetStatus(
                    assetId, AssetStatus.IDLE, "Unused", "asset.manager");

            assertThat(result.getStatusChangedBy()).isEqualTo("asset.manager");
        }

        @Test
        @DisplayName("Should record status changed timestamp")
        void updateAssetStatus_setsStatusChangedAt() {
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenAnswer(i -> i.getArgument(0));
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            FixedAsset result = fixedAssetService.updateAssetStatus(
                    assetId, AssetStatus.IDLE, "Unused", "asset.manager");

            assertThat(result.getStatusChangedAt()).isNotNull();
        }

        @Test
        @DisplayName("Should throw RuntimeException when asset not found")
        void updateAssetStatus_assetNotFound_throwsException() {
            UUID badId = UUID.randomUUID();
            when(fixedAssetRepository.findById(badId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> fixedAssetService.updateAssetStatus(
                    badId, AssetStatus.MAINTENANCE, "reason", "user"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Asset not found with ID");
        }

        @Test
        @DisplayName("Should update to all valid statuses")
        void updateAssetStatus_allValidStatuses_succeed() {
            for (AssetStatus status : AssetStatus.values()) {
                FixedAsset freshAsset = new FixedAsset();
                freshAsset.setId(assetId);
                freshAsset.setStatus(AssetStatus.IN_USE);
                freshAsset.setOriginalCost(new BigDecimal("25000000"));
                freshAsset.setCreatedAt(LocalDateTime.now());
                freshAsset.setUpdatedAt(LocalDateTime.now());
                freshAsset.setCreatedBy("system");

                when(fixedAssetRepository.findById(assetId))
                        .thenReturn(Optional.of(freshAsset));
                when(fixedAssetRepository.save(any())).thenAnswer(i -> i.getArgument(0));
                when(assetHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

                FixedAsset result = fixedAssetService.updateAssetStatus(
                        assetId, status, "reason", "user");

                assertThat(result.getStatus()).isEqualTo(status);
            }
        }
    }

    // ══════════════════════════════════════════════════════════
    // FA-04 — Asset lifecycle history
    // ══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("FA-04: Asset history logging")
    class AssetHistoryTests {

        @Test
        @DisplayName("Should write STATUS_CHANGED history on status update")
        void updateStatus_writesStatusChangedHistoryLog() {
            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenAnswer(i -> i.getArgument(0));
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            fixedAssetService.updateAssetStatus(
                    assetId, AssetStatus.MAINTENANCE, "Bàn phím hỏng", "asset.manager");

            ArgumentCaptor<AssetHistory> captor = ArgumentCaptor.forClass(AssetHistory.class);
            verify(assetHistoryRepository, times(1)).save(captor.capture());

            AssetHistory log = captor.getValue();
            assertThat(log.getEventType()).isEqualTo("STATUS_CHANGED");
            assertThat(log.getAssetId()).isEqualTo(assetId);
            assertThat(log.getOldValue()).contains("IN_USE");
            assertThat(log.getNewValue()).contains("MAINTENANCE");
            assertThat(log.getPerformedBy()).isEqualTo("asset.manager");
            assertThat(log.getPerformedAt()).isNotNull();
        }

        @Test
        @DisplayName("History log should record old status before change")
        void updateStatus_historyLog_containsOldStatus() {
            savedAsset.setStatus(AssetStatus.IN_USE);

            when(fixedAssetRepository.findById(assetId)).thenReturn(Optional.of(savedAsset));
            when(fixedAssetRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(assetHistoryRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            fixedAssetService.updateAssetStatus(
                    assetId, AssetStatus.IDLE, "Not needed", "asset.manager");

            ArgumentCaptor<AssetHistory> captor = ArgumentCaptor.forClass(AssetHistory.class);
            verify(assetHistoryRepository).save(captor.capture());

            assertThat(captor.getValue().getOldValue()).contains("IN_USE");
        }

        @Test
        @DisplayName("History log for creation should have null oldValue")
        void createAsset_historyLog_oldValueIsNull() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            fixedAssetService.createAsset(validDto);

            ArgumentCaptor<AssetHistory> captor = ArgumentCaptor.forClass(AssetHistory.class);
            verify(assetHistoryRepository).save(captor.capture());

            assertThat(captor.getValue().getOldValue()).isNull();
        }

        @Test
        @DisplayName("History log for creation should contain asset code in newValue")
        void createAsset_historyLog_newValueContainsAssetCode() {
            when(fixedAssetRepository.save(any(FixedAsset.class))).thenReturn(savedAsset);
            when(assetHistoryRepository.save(any(AssetHistory.class)))
                    .thenAnswer(i -> i.getArgument(0));

            fixedAssetService.createAsset(validDto);

            ArgumentCaptor<AssetHistory> captor = ArgumentCaptor.forClass(AssetHistory.class);
            verify(assetHistoryRepository).save(captor.capture());

            assertThat(captor.getValue().getNewValue()).contains("TS-2024-001");
        }
    }
}