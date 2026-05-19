package vn.edu.hust.soict.soe.assetmanagement.asset.controller;

import vn.edu.hust.soict.soe.assetmanagement.asset.dto.FixedAssetDTO;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.AssetHistory;
import vn.edu.hust.soict.soe.assetmanagement.asset.entity.FixedAsset;
import vn.edu.hust.soict.soe.assetmanagement.asset.enums.AssetStatus;
import vn.edu.hust.soict.soe.assetmanagement.asset.service.FixedAssetService;
import vn.edu.hust.soict.soe.assetmanagement.asset.repository.AssetHistoryRepository; // Import thêm để lấy lịch sử

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;
import jakarta.validation.Valid;

import java.util.List;
import java.util.UUID;
/**
 * Fixed asset endpoints.
 * Handles GET, POST, PUT for /api/assets.
 */
@RestController
@RequestMapping("/api/assets")
@Tag(name = "Fixed Assets", description = "Fixed Asset Management Module (FA-01 -> FA-04)")
public class FixedAssetController {

    @Autowired
    private FixedAssetService fixedAssetService;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    // --- FA-01: Asset registration & digital profile ---
    
    @Operation(summary = "Get all assets", description = "Returns a list of all digital asset profiles")
    @GetMapping
    public ResponseEntity<List<FixedAsset>> getAllAssets() {
        return ResponseEntity.ok(fixedAssetService.getAllAssets());
    }

    @Operation(summary = "Get asset details", description = "Retrieve full technical parameters and current financial status by ID")
    @GetMapping("/{id}")
    public ResponseEntity<FixedAsset> getAssetById(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(fixedAssetService.calculateCurrentDepreciation(id));
    }

    @Operation(summary = "Create new asset profile", description = "Create a new asset profile with full parameters (FA-01)")
    @PostMapping
    public ResponseEntity<FixedAsset> createAsset(@Valid @RequestBody FixedAssetDTO assetDTO) {
        FixedAsset newAsset = fixedAssetService.createAsset(assetDTO);
        return new ResponseEntity<>(newAsset, HttpStatus.CREATED);
    }

    // --- FA-02: Depreciation calculation engine ---

    @Operation(summary = "Calculate depreciation", description = "Calculate accumulated depreciation and remaining book value per Circular 45/2013/TT-BTC")
    @GetMapping("/{id}/depreciation")
    public ResponseEntity<FixedAsset> calculateDepreciation(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(fixedAssetService.calculateCurrentDepreciation(id));
    }

    // --- FA-03 & FA-04: Operational status tracking & History ---

    @Operation(summary = "Update operational status", description = "Change asset status (maintenance, liquidation...) and save history log (FA-03)")
    @PatchMapping("/{id}/status")
    public ResponseEntity<FixedAsset> updateStatus(
            @PathVariable @NonNull UUID id, 
            @RequestParam AssetStatus newStatus, 
            @RequestParam String reason) {
        
        FixedAsset updated = fixedAssetService.updateAssetStatus(id, newStatus, reason, "current_user"); //đang tạm để current_user
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "Get asset lifecycle history", description = "Retrieve immutable log of status changes and reassignments (FA-04)")
    @GetMapping("/{id}/history")
    public ResponseEntity<List<AssetHistory>> getAssetHistory(@PathVariable @NonNull UUID id) {
        List<AssetHistory> history = assetHistoryRepository.findByAssetIdOrderByPerformedAtDesc(id);
        return ResponseEntity.ok(history);
    }
}
