# MarketHub Security Specification (Phase 0 TDD)

## 1. Data Invariants
1. **Authentication Gate**: Every read and write operation across all collections (`/admins`, `/users`, `/markets`, `/categories`, `/shops`, `/products`, `/offers`, `/inquiries`, `/reports`) requires `request.auth != null`.
2. **PII Isolation**:
   - `/users/{userId}` contains `email` and `phone` and is strictly readable only by `request.auth.uid == userId || isAdmin()`.
   - `/inquiries/{inquiryId}` is strictly readable only by `resource.data.customerId == request.auth.uid || resource.data.shopOwnerId == request.auth.uid || isAdmin()`.
   - `/reports/{reportId}` is strictly readable only by `resource.data.reporterId == request.auth.uid || isAdmin()`.
3. **Shop Approval & Self-Approval Prevention**:
   - Non-admin shopkeepers creating a `/shops/{shopId}` document MUST set `ownerId == request.auth.uid`, `status == 'pending'`, `isVerified == false`, and `isFeatured == false`.
   - Shopkeepers cannot update `status`, `isVerified`, or `isFeatured` on their shop. Only `isAdmin()` can approve/reject/suspend a shop or toggle `isVerified` / `isFeatured`.
4. **Parent Gate on Products & Offers**:
   - Creating a `/products/{productId}` or `/offers/{offerId}` requires that `/shops/$(incoming().shopId)` exists, belongs to `request.auth.uid`, and has `status == 'approved'` (unless `isAdmin()`).
   - Shopkeepers can only edit or delete their own products (`resource.data.ownerId == request.auth.uid`).
5. **Query Enforcer (No Blanket List Reads)**:
   - `/markets`: `resource.data.isActive == true || isAdmin()`
   - `/categories`: `resource.data.isActive == true || isAdmin()`
   - `/shops`: `resource.data.status == 'approved' || resource.data.ownerId == request.auth.uid || isAdmin()`
   - `/products`: `resource.data.status == 'active' || resource.data.ownerId == request.auth.uid || isAdmin()`
   - `/offers`: `resource.data.isActive == true || resource.data.ownerId == request.auth.uid || isAdmin()`

## 2. The "Dirty Dozen" Adversarial Payloads
1. **Unauthenticated Read**: `unauthDb.collection("products").where("status", "==", "active").get()` -> Reject (`PERMISSION_DENIED`).
2. **PII Leak on User Profile**: `bobDb.collection("users").doc("alice_uid").get()` -> Reject (`PERMISSION_DENIED`).
3. **Privilege Escalation on Registration**: `aliceDb.collection("users").doc("alice_uid").set({ ..., role: "admin" })` -> Reject (`PERMISSION_DENIED`).
4. **Self-Approving Shop on Creation**: `shopkeeperDb.collection("shops").doc("shop_1").set({ ..., status: "approved", isVerified: true })` -> Reject (`PERMISSION_DENIED`).
5. **Self-Approving Shop on Update**: `shopkeeperDb.collection("shops").doc("shop_1").update({ status: "approved" })` -> Reject (`PERMISSION_DENIED`).
6. **Shadow Update / Ghost Field Injection**: `shopkeeperDb.collection("products").doc("prod_1").update({ ghostAdminFlag: true })` -> Reject (`PERMISSION_DENIED` via `hasOnly`).
7. **Orphaned / Unapproved Shop Product Creation**: `shopkeeperDb.collection("products").doc("prod_1").set({ shopId: "pending_shop_id", ... })` -> Reject (`PERMISSION_DENIED` via `get()` parent gate).
8. **Cross-Shopkeeper Product Tampering**: `bobShopkeeperDb.collection("products").doc("alice_product").update({ pricePkr: 1 })` -> Reject (`PERMISSION_DENIED`).
9. **Email Spoofing Admin Bypass**: Token with `email: "awan11118@gmail.com"` and `email_verified: false` attempting to approve a shop -> Reject (`PERMISSION_DENIED`).
10. **Unfiltered List Scraping**: `aliceDb.collection("shops").get()` (without `status == 'approved'` or `ownerId == alice_uid`) -> Reject (`PERMISSION_DENIED`).
11. **Value Poisoning on Price**: `shopkeeperDb.collection("products").doc("prod_1").update({ pricePkr: -500 })` -> Reject (`PERMISSION_DENIED`).
12. **Terminal State Unlocking on Closed Inquiry**: `shopkeeperDb.collection("inquiries").doc("closed_inq").update({ status: "replied" })` -> Reject (`PERMISSION_DENIED`).
