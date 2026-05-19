# Database Schema - SOE Asset Management System
* Version: 1.0
* Database: PostgreSQL 15
* Migration tool: Flyway (V1 -> V6)

---

## Overview

The database consists of 13 tables across 4 functional domains.
All schema changes are managed exclusively through Flyway migration scripts
located at `backend/src/main/resources/db/migration/`.

**Never modify a migration file that has already been applied.**
Create a new versioned file (V7, V8...) for any schema changes.

---

## Conventions

| Convention | Rule |
|-----------|------|
| Primary keys | UUID (`gen_random_uuid()`) except lookup tables which use SERIAL |
| Timestamps | `TIMESTAMP NOT NULL DEFAULT NOW()` |
| Audit fields | `created_at`, `updated_at`, `created_by` on all operational tables |
| Append-only tables | No `updated_at` column - `asset_history`, `stock_transactions`, `audit_logs` |
| Money | `NUMERIC(18, 2)` - Vietnamese Đồng, no decimals in API responses |
| Quantities | `NUMERIC(12, 3)` - 3 decimal places for stock quantities |
| Soft delete | `is_active BOOLEAN` - no hard deletes on master data |
| Naming | `snake_case` for all table and column names |

---

## Migration Files

| File | Description |
|------|-------------|
| `V1__create_users_roles.sql` | Users, roles, managing units |
| `V2__create_assets.sql` | Fixed assets, asset history, asset categories |
| `V3__create_stock.sql` | Materials, stock transactions, storage locations, material categories |
| `V4__create_handover_liquidation.sql` | Handover requests, liquidation requests |
| `V5__create_audit_log.sql` | System-wide immutable audit log |
| `V6__seed_data.sql` | Development seed data - 5 users, 5 units, sample asset and material |
| `V7__fix_seed_password_hashes.sql` | Corrects BCrypt hashes for seed users |

---

## Domain 1 - Users & Access Control (M1)

### `managing_units`
Departments or subsidiary units within the SOE.
Assets and stock transactions are scoped to a managing unit.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE | e.g. `HQ`, `PHKT`, `PKHO` |
| `name` | VARCHAR(255) | NOT NULL | Vietnamese display name |
| `description` | TEXT | | |
| `parent_id` | UUID | FK -> managing_units(id) ON DELETE SET NULL | Self-referencing hierarchy |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `created_by` | VARCHAR(100) | | |

**Indexes:** `idx_units_parent` on `parent_id`

---

### `roles`
System roles - exactly 5 per Bảng 2.6 of the SRS.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | SERIAL | PK | |
| `code` | VARCHAR(50) | NOT NULL, UNIQUE | See role codes below |
| `name` | VARCHAR(100) | NOT NULL | |
| `description` | TEXT | | |

**Seeded role codes:**
| Code | Display name |
|------|-------------|
| `SYSTEM_ADMIN` | Quản trị viên |
| `ASSET_MANAGER` | Quản lý tài sản |
| `WAREHOUSE` | Thủ kho |
| `APPROVING_AUTH` | Người phê duyệt |
| `FINANCE_AUDIT` | Kế toán / Kiểm toán |

---

### `users`
System user accounts.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `username` | VARCHAR(100) | NOT NULL, UNIQUE | Login identifier |
| `password_hash` | VARCHAR(255) | NOT NULL | BCrypt(10) hash |
| `full_name` | VARCHAR(255) | NOT NULL | |
| `email` | VARCHAR(255) | UNIQUE | |
| `phone` | VARCHAR(20) | | |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | False = cannot login |
| `last_login_at` | TIMESTAMP | | Updated on each login |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `created_by` | VARCHAR(100) | | |

**Indexes:** `idx_users_username`, `idx_users_email`

---

### `user_roles`
Many-to-many: users ↔ roles.

| Column | Type | Constraints |
|--------|------|-------------|
| `user_id` | UUID | PK, FK -> users(id) ON DELETE CASCADE |
| `role_id` | INTEGER | PK, FK -> roles(id) ON DELETE CASCADE |

**Index:** `idx_user_roles_user` on `user_id`

---

### `user_units`
Many-to-many: users ↔ managing_units.
Defines which units a user can access data for.

| Column | Type | Constraints |
|--------|------|-------------|
| `user_id` | UUID | PK, FK -> users(id) ON DELETE CASCADE |
| `unit_id` | UUID | PK, FK -> managing_units(id) ON DELETE CASCADE |

**Index:** `idx_user_units_user` on `user_id`

---

## Domain 2 - Fixed Assets (M2)

### `asset_categories`
Lookup table - asset classifications per Thông tư 45/2013/TT-BTC.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | SERIAL | PK | |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE | e.g. `IT`, `MACHINE`, `VEHICLE` |
| `name` | VARCHAR(255) | NOT NULL | |
| `useful_life_min` | INTEGER | | Minimum years per TT45 |
| `useful_life_max` | INTEGER | | Maximum years per TT45 |
| `depreciation_method` | VARCHAR(20) | NOT NULL, DEFAULT 'STRAIGHT_LINE' | |
| `description` | TEXT | | |

**Seeded categories:** `MACHINE`, `VEHICLE`, `BUILDING`, `EQUIPMENT`, `IT`, `OTHER`

---

### `assets`
Core fixed asset registry - the most important table in the system.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `asset_code` | VARCHAR(50) | NOT NULL, UNIQUE | e.g. `TS-2024-001` |
| `name` | VARCHAR(255) | NOT NULL | |
| `category_id` | INTEGER | NOT NULL, FK -> asset_categories(id) | |
| `managing_unit_id` | UUID | NOT NULL, FK -> managing_units(id) | Current owner unit |
| `serial_number` | VARCHAR(100) | | |
| `manufacturer` | VARCHAR(255) | | |
| `model` | VARCHAR(255) | | |
| `country_of_origin` | VARCHAR(100) | | |
| `technical_specs` | TEXT | | Free-text technical parameters |
| `location` | VARCHAR(255) | | Physical location |
| `original_cost` | NUMERIC(18,2) | NOT NULL | Nguyên giá |
| `acquisition_date` | DATE | NOT NULL | |
| `funding_source` | VARCHAR(100) | | e.g. `Ngân sách nhà nước` |
| `useful_life_years` | INTEGER | NOT NULL | |
| `salvage_value` | NUMERIC(18,2) | NOT NULL, DEFAULT 0 | |
| `depreciation_method` | VARCHAR(20) | NOT NULL, DEFAULT 'STRAIGHT_LINE' | `STRAIGHT_LINE` or `DECLINING_BALANCE` |
| `accumulated_depreciation` | NUMERIC(18,2) | NOT NULL, DEFAULT 0 | Updated by DepreciationService |
| `net_book_value` | NUMERIC(18,2) | NOT NULL, DEFAULT 0 | = original_cost - accumulated_depreciation |
| `depreciation_start_date` | DATE | | |
| `depreciation_end_date` | DATE | | |
| `annual_depreciation_rate` | NUMERIC(8,4) | | Percentage e.g. 20.0000 |
| `status` | VARCHAR(30) | NOT NULL, DEFAULT 'IN_USE' | See status values below |
| `status_reason` | TEXT | | Reason for last status change |
| `status_changed_at` | TIMESTAMP | | |
| `status_changed_by` | VARCHAR(100) | | |
| `purchase_document_ref` | VARCHAR(255) | | Invoice / procurement order number |
| `notes` | TEXT | | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `created_by` | VARCHAR(100) | NOT NULL | |

**Status values:** `IN_USE` | `MAINTENANCE` | `IDLE` | `TRANSFERRED` | `LIQUIDATED`

**Indexes:** `idx_assets_code`, `idx_assets_unit`, `idx_assets_category`,
`idx_assets_status`, `idx_assets_acquisition`

**Business rules:**
- `net_book_value` = `original_cost` - `accumulated_depreciation`, never below 0
- `accumulated_depreciation` is always recomputed server-side - never trust client values
- Status `LIQUIDATED` can only be set by the liquidation workflow, not directly

---

### `asset_history`
Immutable lifecycle history for every asset. Append-only - no updates or deletes.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `asset_id` | UUID | NOT NULL, FK -> assets(id) ON DELETE CASCADE | |
| `event_type` | VARCHAR(50) | NOT NULL | See event types below |
| `description` | TEXT | NOT NULL | Human-readable summary |
| `old_value` | TEXT | | JSON of fields before change |
| `new_value` | TEXT | | JSON of fields after change |
| `performed_by` | VARCHAR(100) | NOT NULL | Username |
| `performed_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**No `updated_at` column** - this table is append-only by design.

**Event types:** `CREATED` | `STATUS_CHANGED` | `COST_UPDATED` | `REVALUED` |
`TRANSFERRED` | `DEPRECIATION_POSTED` | `LIQUIDATED`

**Indexes:** `idx_asset_history_asset`, `idx_asset_history_event`, `idx_asset_history_date`

---

## Domain 3 - Consumable Stock (M3)

### `material_categories`
Lookup table for material classifications.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | SERIAL | PK | |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE | e.g. `OFFICE`, `FUEL` |
| `name` | VARCHAR(255) | NOT NULL | |
| `description` | TEXT | | |

**Seeded categories:** `OFFICE`, `EQUIPMENT`, `CHEMICAL`, `FUEL`, `SPARE`, `OTHER`

---

### `materials`
Consumable material catalogue (CS-01).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `material_code` | VARCHAR(50) | NOT NULL, UNIQUE | e.g. `VT-2024-001` |
| `name` | VARCHAR(255) | NOT NULL | |
| `category_id` | INTEGER | NOT NULL, FK -> material_categories(id) | |
| `unit_of_measure` | VARCHAR(30) | NOT NULL | e.g. `Ream`, `Kg`, `Lít`, `Cái` |
| `technical_specs` | TEXT | | |
| `supplier_name` | VARCHAR(255) | | |
| `supplier_code` | VARCHAR(100) | | |
| `unit_price` | NUMERIC(18,2) | | Reference unit price |
| `minimum_stock` | NUMERIC(12,3) | NOT NULL, DEFAULT 0 | Reorder threshold |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| `notes` | TEXT | | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `created_by` | VARCHAR(100) | NOT NULL | |

**Indexes:** `idx_materials_code`, `idx_materials_category`

---

### `storage_locations`
Warehouse / storage locations that hold stock.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `code` | VARCHAR(20) | NOT NULL, UNIQUE | e.g. `KHO-01` |
| `name` | VARCHAR(255) | NOT NULL | |
| `unit_id` | UUID | NOT NULL, FK -> managing_units(id) | Which unit owns this warehouse |
| `is_active` | BOOLEAN | NOT NULL, DEFAULT TRUE | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

---

### `stock_transactions`
Every stock receipt and issue event (CS-02). Immutable - no updates or deletes.

**CS-03 note:** Current stock balance is NOT stored as a column. It is computed at
query time as:
```sql
SELECT
    material_id,
    storage_location_id,
    SUM(CASE WHEN transaction_type = 'RECEIPT' THEN quantity ELSE -quantity END)
        AS current_balance
FROM stock_transactions
GROUP BY material_id, storage_location_id;
```

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `material_id` | UUID | NOT NULL, FK -> materials(id) | |
| `storage_location_id` | UUID | NOT NULL, FK -> storage_locations(id) | |
| `transaction_type` | VARCHAR(10) | NOT NULL | `RECEIPT` or `ISSUE` |
| `quantity` | NUMERIC(12,3) | NOT NULL, CHECK (quantity > 0) | |
| `unit_of_measure` | VARCHAR(30) | NOT NULL | Copied from material at transaction time |
| `unit_price` | NUMERIC(18,2) | | Price at time of transaction |
| `total_value` | NUMERIC(18,2) | | quantity × unit_price |
| `requesting_department_id` | UUID | FK -> managing_units(id) | Required for ISSUE transactions (CS-04) |
| `requested_by` | VARCHAR(100) | | Name of staff who requested |
| `document_ref` | VARCHAR(255) | NOT NULL | Receipt/issue slip number |
| `document_date` | DATE | NOT NULL | |
| `notes` | TEXT | | |
| `approved_by` | VARCHAR(100) | | |
| `approved_at` | TIMESTAMP | | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `created_by` | VARCHAR(100) | NOT NULL | |

**No `updated_at` column** - transactions are immutable once created.

**Indexes:** `idx_stock_tx_material`, `idx_stock_tx_location`, `idx_stock_tx_type`,
`idx_stock_tx_dept`, `idx_stock_tx_date`

**Business rules:**
- An ISSUE transaction must not be permitted if the resulting balance would go negative
- The `total_value` is computed automatically: `quantity × unit_price`
- ISSUE transactions must have `requesting_department_id` - required for CS-04 reporting

---

## Domain 4 - Handover & Liquidation (M4)

### `handover_requests`
Asset transfer workflows between managing units (HL-01, HL-03).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `request_code` | VARCHAR(50) | NOT NULL, UNIQUE | e.g. `BG-2024-001` |
| `asset_id` | UUID | NOT NULL, FK -> assets(id) | |
| `from_unit_id` | UUID | NOT NULL, FK -> managing_units(id) | Transferring unit |
| `to_unit_id` | UUID | NOT NULL, FK -> managing_units(id) | Receiving unit |
| `initiated_by` | VARCHAR(100) | NOT NULL | Username of initiator |
| `status` | VARCHAR(30) | NOT NULL, DEFAULT 'DRAFT' | See workflow below |
| `reason` | TEXT | NOT NULL | |
| `handover_date` | DATE | | Planned or actual date |
| `asset_condition` | VARCHAR(50) | | `GOOD` \| `FAIR` \| `POOR` |
| `notes` | TEXT | | |
| `dept_approved_by` | VARCHAR(100) | | Step 1 approver |
| `dept_approved_at` | TIMESTAMP | | |
| `dept_approval_notes` | TEXT | | |
| `confirmed_by` | VARCHAR(100) | | Step 2 confirmer (receiving unit) |
| `confirmed_at` | TIMESTAMP | | |
| `confirmation_notes` | TEXT | | |
| `completed_by` | VARCHAR(100) | | Step 3 - system or admin |
| `completed_at` | TIMESTAMP | | |
| `rejected_by` | VARCHAR(100) | | |
| `rejected_at` | TIMESTAMP | | |
| `rejection_reason` | TEXT | | |
| `document_ref` | VARCHAR(255) | | Generated Biên bản bàn giao number |
| `document_generated_at` | TIMESTAMP | | |
| `document_signed` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `created_by` | VARCHAR(100) | NOT NULL | |

**Workflow status transitions:**
```
DRAFT -> PENDING_APPROVAL -> APPROVED -> CONFIRMED -> COMPLETED
                    ↓              ↓
                REJECTED       REJECTED
```

**Separation of duties:** `initiated_by` must not equal `dept_approved_by` - enforced at service layer.

**On COMPLETED:** asset `managing_unit_id` updated to `to_unit_id`, asset status set to `TRANSFERRED`,
history log written. All in one atomic transaction.

**Indexes:** `idx_handover_code`, `idx_handover_asset`, `idx_handover_status`,
`idx_handover_from_unit`, `idx_handover_to_unit`

---

### `liquidation_requests`
Asset disposal workflows (HL-02, HL-03).

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `request_code` | VARCHAR(50) | NOT NULL, UNIQUE | e.g. `TL-2024-001` |
| `asset_id` | UUID | NOT NULL, FK -> assets(id) | |
| `requesting_unit_id` | UUID | NOT NULL, FK -> managing_units(id) | |
| `initiated_by` | VARCHAR(100) | NOT NULL | |
| `status` | VARCHAR(30) | NOT NULL, DEFAULT 'DRAFT' | See workflow below |
| `justification` | TEXT | NOT NULL | |
| `asset_condition` | VARCHAR(50) | NOT NULL | `GOOD` \| `FAIR` \| `POOR` \| `DAMAGED` |
| `current_market_value` | NUMERIC(18,2) | | Estimated value at time of request |
| `disposal_method` | VARCHAR(30) | NOT NULL | `AUCTION` \| `SCRAP` \| `DONATION` |
| `disposal_notes` | TEXT | | |
| `manager_approved_by` | VARCHAR(100) | | Step 1 - asset manager level |
| `manager_approved_at` | TIMESTAMP | | |
| `manager_notes` | TEXT | | |
| `director_approved_by` | VARCHAR(100) | | Step 2 - director level |
| `director_approved_at` | TIMESTAMP | | |
| `director_notes` | TEXT | | |
| `completed_by` | VARCHAR(100) | | |
| `completed_at` | TIMESTAMP | | |
| `final_disposal_value` | NUMERIC(18,2) | | Actual value realised |
| `rejected_by` | VARCHAR(100) | | |
| `rejected_at` | TIMESTAMP | | |
| `rejection_reason` | TEXT | | |
| `document_ref` | VARCHAR(255) | | Generated Biên bản thanh lý number |
| `document_generated_at` | TIMESTAMP | | |
| `document_signed` | BOOLEAN | NOT NULL, DEFAULT FALSE | |
| `created_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `updated_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |
| `created_by` | VARCHAR(100) | NOT NULL | |

**Workflow status transitions:**
```
DRAFT -> PENDING_MANAGER -> PENDING_DIRECTOR -> APPROVED -> COMPLETED
                ↓                  ↓
            REJECTED           REJECTED
```

**On COMPLETED:** asset status set to `LIQUIDATED`, asset record becomes read-only,
history log written, audit log written. All in one atomic transaction.

**Indexes:** `idx_liquidation_code`, `idx_liquidation_asset`, `idx_liquidation_status`,
`idx_liquidation_unit`

---

## Domain 5 - Audit Trail (M4)

### `audit_logs`
Immutable system-wide event log (RP-01). Append-only - protected at database level.

| Column | Type | Constraints | Description |
|--------|------|-------------|-------------|
| `id` | UUID | PK, default gen_random_uuid() | |
| `module` | VARCHAR(50) | NOT NULL | `ASSET` \| `STOCK` \| `HANDOVER` \| `LIQUIDATION` \| `USER` \| `AUTH` |
| `action` | VARCHAR(50) | NOT NULL | `CREATE` \| `UPDATE` \| `DELETE` \| `STATUS_CHANGE` \| `APPROVE` \| `REJECT` \| `CONFIRM` \| `LOGIN` \| `LOGOUT` \| `EXPORT` |
| `record_id` | VARCHAR(255) | | UUID or code of affected record |
| `record_code` | VARCHAR(100) | | Human-readable code e.g. `TS-2024-001` |
| `performed_by` | VARCHAR(100) | NOT NULL | Username |
| `user_id` | UUID | FK -> users(id) ON DELETE SET NULL | |
| `ip_address` | VARCHAR(45) | | IPv4 or IPv6 |
| `old_value` | TEXT | | JSON of fields before action (null for CREATE) |
| `new_value` | TEXT | | JSON of fields after action (null for DELETE) |
| `description` | TEXT | | Human-readable summary |
| `performed_at` | TIMESTAMP | NOT NULL, DEFAULT NOW() | |

**No `updated_at` column - append-only by design.**

**Database-level protection (PostgreSQL rules):**
```sql
CREATE RULE no_update_audit_log AS ON UPDATE TO audit_logs DO INSTEAD NOTHING;
CREATE RULE no_delete_audit_log AS ON DELETE TO audit_logs DO INSTEAD NOTHING;
```

**Retention:** Minimum 10 years per Vietnamese state records regulations.

**Indexes:** `idx_audit_module`, `idx_audit_action`, `idx_audit_record_id`,
`idx_audit_user`, `idx_audit_performed_at` (DESC), `idx_audit_performed_by`

---

## Entity Relationship Summary

```
managing_units ─────────────────────────────────────────────────┐
     │                                                          │
     ├── user_units ──── users ──── user_roles ──── roles       │
     │                                                          │
     ├── assets ──── asset_categories                           │
     │      └── asset_history                                   │
     │                                                          │
     ├── storage_locations                                      │
     │                                                          │
     ├── stock_transactions ──── materials ──── material_categories
     │                                                          │
     ├── handover_requests ──── assets                          │
     │                                                          │
     └── liquidation_requests ──── assets                       │
                                                                │
audit_logs ──── users ──────────────────────────────────────────┘
```

---

## Seed Data (V6)

| Table | Seeded rows |
|-------|-------------|
| `managing_units` | 5 - HQ, PHKT, PKTCN, PKD, PKHO |
| `roles` | 5 - SYSTEM_ADMIN, ASSET_MANAGER, WAREHOUSE, APPROVING_AUTH, FINANCE_AUDIT |
| `users` | 5 - one per role, all with password `Password@123` |
| `user_roles` | 5 - one per user |
| `user_units` | 5 - one per user |
| `storage_locations` | 2 - KHO-01, KHO-02 |
| `assets` | 1 - TS-2024-001 (sample Dell laptop) |
| `materials` | 1 - VT-2024-001 (sample A4 paper) |

**Seed user credentials:**

| Username | Password | Role |
|----------|---------|------|
| `admin` | `Password@123` | SYSTEM_ADMIN |
| `asset.manager` | `Password@123` | ASSET_MANAGER |
| `warehouse` | `Password@123` | WAREHOUSE |
| `approver` | `Password@123` | APPROVING_AUTH |
| `finance.audit` | `Password@123` | FINANCE_AUDIT |

---

## Key Design Decisions

**1. UUID primary keys everywhere**
Prevents enumeration attacks. Supports future data migration between environments.
Exception: lookup tables (`roles`, `asset_categories`, `material_categories`) use SERIAL
since they have a small fixed number of rows and are never exposed directly in URLs.

**2. Stock balance is computed, not stored**
`stock_transactions` has no balance column. Balance is always computed as
`SUM(RECEIPT qty) - SUM(ISSUE qty)` at query time. This guarantees consistency
and prevents the balance from going out of sync with transaction history.

**3. Append-only tables for all financial evidence**
`asset_history`, `stock_transactions`, and `audit_logs` have no `updated_at` column
and no application-level update/delete endpoints. This is enforced at both the
application layer and the database layer (PostgreSQL rules on `audit_logs`).

**4. Workflow state in columns, not separate table**
Handover and liquidation approval steps are stored as columns
(`dept_approved_by`, `confirmed_by`, etc.) rather than a separate workflow_steps table.
This is simpler for a 2-3 step workflow and makes reporting queries easier.

**5. Foreign keys to managing_units use ID references only**
`stock_transactions.requesting_department_id` and `storage_locations.unit_id`
store UUIDs without a JPA `@ManyToOne` join to the `managing_units` table.
This avoids cross-module entity dependencies and keeps the stock module
independently deployable.