const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const CUSTOMER_UID = "customer_1";
const SHOPKEEPER_1_UID = "shopkeeper_1";
const SHOPKEEPER_2_UID = "shopkeeper_2";
const ADMIN_UID = "admin_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function nowTs() {
  return new Date();
}

test("1. Unauthenticated caller cannot read shops or products", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("shops").where("status", "==", "approved").get());
  await assertFails(unauthDb.collection("products").where("status", "==", "active").get());
});

test("2. User profile PII isolation and role privilege escalation guard", async () => {
  const ts = nowTs();
  const aliceDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();

  // Cannot self-assign admin role on registration
  await assertFails(
    aliceDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Ayesha Khan",
      email: "ayesha@example.com",
      role: "admin",
      isSuspended: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Valid customer profile creation succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(CUSTOMER_UID).set({
      userId: CUSTOMER_UID,
      displayName: "Ayesha Khan",
      email: "ayesha@example.com",
      role: "customer",
      phone: "+923001112233",
      isSuspended: false,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Another user cannot read Ayesha's profile (PII isolation)
  const bobDb = testEnv.authenticatedContext(SHOPKEEPER_1_UID).firestore();
  await assertFails(bobDb.collection("users").doc(CUSTOMER_UID).get());
});

test("3. Shopkeeper can submit pending shop, cannot self-approve, Admin can approve", async () => {
  const ts = nowTs();
  const shopkeeperDb = testEnv.authenticatedContext(SHOPKEEPER_1_UID).firestore();

  const baseShop = {
    ownerId: SHOPKEEPER_1_UID,
    ownerName: "Tariq Mahmood",
    name: "Tariq Electronics & Mobile Hub",
    categoryId: "cat_mobile",
    categoryName: "Mobile phones and accessories",
    marketId: "mkt_hafeez",
    marketName: "Hafeez Center, Lahore",
    address: "Shop #42, First Floor, Hafeez Center, Gulberg III, Lahore",
    mapLocation: "31.5162, 74.3435",
    description: "Authorized dealer of smartphones, fast chargers, and accessories.",
    businessHours: "11:00 AM - 10:00 PM (Mon-Sat)",
    contactPhone: "+923214455667",
    whatsappNumber: "923214455667",
    logoUrl: "",
    coverUrl: "",
    status: "pending",
    isVerified: false,
    isFeatured: false,
    viewsCount: 0,
    inquiriesCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };

  // Self-approved shop creation fails
  await assertFails(
    shopkeeperDb.collection("shops").doc("shop_1").set({
      ...baseShop,
      status: "approved",
    })
  );

  // Pending shop creation succeeds
  await assertSucceeds(shopkeeperDb.collection("shops").doc("shop_1").set(baseShop));

  // Shopkeeper cannot self-approve via update
  await assertFails(
    shopkeeperDb.collection("shops").doc("shop_1").update({
      status: "approved",
      updatedAt: new Date(),
    })
  );

  // Email spoofing test: unverified admin email cannot approve
  const spoofAdminDb = testEnv
    .authenticatedContext("spoof_uid", { email: "awan11118@gmail.com", email_verified: false })
    .firestore();
  await assertFails(
    spoofAdminDb.collection("shops").doc("shop_1").update({
      status: "approved",
      updatedAt: new Date(),
    })
  );

  // Verified runtime admin email CAN approve and verify shop
  const realAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, { email: "awan11118@gmail.com", email_verified: true })
    .firestore();
  await assertSucceeds(
    realAdminDb.collection("shops").doc("shop_1").update({
      status: "approved",
      isVerified: true,
      isFeatured: true,
      updatedAt: new Date(),
    })
  );
});

test("4. Product creation requires approved shop, enforces cross-shopkeeper isolation and price validation", async () => {
  const ts = nowTs();
  // Seed one pending shop and one approved shop for SHOPKEEPER_1_UID
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("shops").doc("pending_shop").set({
      ownerId: SHOPKEEPER_1_UID,
      ownerName: "Tariq",
      name: "Pending Shop",
      categoryId: "cat_mobile",
      categoryName: "Mobile phones and accessories",
      marketId: "mkt_hafeez",
      marketName: "Hafeez Center",
      address: "Shop 10, Lahore",
      description: "Pending shop test",
      businessHours: "10am - 9pm",
      contactPhone: "+923001234567",
      whatsappNumber: "923001234567",
      status: "pending",
      isVerified: false,
      isFeatured: false,
      viewsCount: 0,
      inquiriesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("shops").doc("approved_shop").set({
      ownerId: SHOPKEEPER_1_UID,
      ownerName: "Tariq",
      name: "Approved Shop",
      categoryId: "cat_mobile",
      categoryName: "Mobile phones and accessories",
      marketId: "mkt_hafeez",
      marketName: "Hafeez Center",
      address: "Shop 12, Lahore",
      description: "Approved shop test",
      businessHours: "10am - 9pm",
      contactPhone: "+923001234567",
      whatsappNumber: "923001234567",
      status: "approved",
      isVerified: true,
      isFeatured: false,
      viewsCount: 0,
      inquiriesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const sk1Db = testEnv.authenticatedContext(SHOPKEEPER_1_UID).firestore();
  const sk2Db = testEnv.authenticatedContext(SHOPKEEPER_2_UID).firestore();

  const validProduct = {
    shopId: "approved_shop",
    shopName: "Approved Shop",
    marketName: "Hafeez Center",
    ownerId: SHOPKEEPER_1_UID,
    name: "Samsung Galaxy A55 5G 256GB",
    normalizedName: "samsung galaxy a55 5g 256gb",
    categoryId: "cat_mobile",
    categoryName: "Mobile phones and accessories",
    description: "Official PTA approved with 1 year warranty.",
    pricePkr: 134999,
    originalPricePkr: 139999,
    stockQuantity: 12,
    isAvailable: true,
    isNewArrival: true,
    isPopular: true,
    imageUrl: "",
    whatsappNumber: "923001234567",
    status: "active",
    viewsCount: 0,
    inquiriesCount: 0,
    createdAt: ts,
    updatedAt: ts,
  };

  // Cannot create product under unapproved/pending shop
  await assertFails(
    sk1Db.collection("products").doc("prod_pending").set({
      ...validProduct,
      shopId: "pending_shop",
    })
  );

  // Can create product under approved shop
  await assertSucceeds(sk1Db.collection("products").doc("prod_1").set(validProduct));

  // Other shopkeeper cannot edit SHOPKEEPER_1's product
  await assertFails(
    sk2Db.collection("products").doc("prod_1").update({
      pricePkr: 99999,
      updatedAt: new Date(),
    })
  );

  // Value poisoning (negative price) fails
  await assertFails(
    sk1Db.collection("products").doc("prod_1").update({
      pricePkr: -500,
      updatedAt: new Date(),
    })
  );

  // Shadow update (ghost field) fails
  await assertFails(
    sk1Db.collection("products").doc("prod_1").update({
      pricePkr: 132000,
      ghostField: "hacked",
      updatedAt: new Date(),
    })
  );

  // Customer can query active products with status filter
  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  await assertSucceeds(customerDb.collection("products").where("status", "==", "active").get());
});

test("5. Inquiries and Reports enforce ownership and terminal state locking", async () => {
  const ts = nowTs();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("shops").doc("approved_shop").set({
      ownerId: SHOPKEEPER_1_UID,
      ownerName: "Tariq",
      name: "Approved Shop",
      categoryId: "cat_mobile",
      categoryName: "Mobile phones and accessories",
      marketId: "mkt_hafeez",
      marketName: "Hafeez Center",
      address: "Shop 12, Lahore",
      description: "Approved shop test",
      businessHours: "10am - 9pm",
      contactPhone: "+923001234567",
      whatsappNumber: "923001234567",
      status: "approved",
      isVerified: true,
      isFeatured: false,
      viewsCount: 0,
      inquiriesCount: 0,
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const customerDb = testEnv.authenticatedContext(CUSTOMER_UID).firestore();
  const sk1Db = testEnv.authenticatedContext(SHOPKEEPER_1_UID).firestore();
  const sk2Db = testEnv.authenticatedContext(SHOPKEEPER_2_UID).firestore();

  await assertSucceeds(
    customerDb.collection("inquiries").doc("inq_1").set({
      customerId: CUSTOMER_UID,
      customerName: "Ayesha Khan",
      customerPhone: "+923001112233",
      shopId: "approved_shop",
      shopName: "Approved Shop",
      shopOwnerId: SHOPKEEPER_1_UID,
      productId: "prod_1",
      productName: "Samsung Galaxy A55 5G",
      displayedPricePkr: 134999,
      message: "Assalam-o-Alaikum, is Awesome Iceblue color available in stock?",
      channel: "whatsapp",
      status: "new",
      shopkeeperReply: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Unrelated user cannot read inquiry
  await assertFails(sk2Db.collection("inquiries").doc("inq_1").get());

  // Shop owner can reply and close inquiry
  await assertSucceeds(
    sk1Db.collection("inquiries").doc("inq_1").update({
      status: "closed",
      shopkeeperReply: "Walaikum Assalam! Yes, sealed box available at Rs. 134,999.",
      updatedAt: new Date(),
    })
  );

  // Once closed (terminal state), shop owner cannot reopen it
  await assertFails(
    sk1Db.collection("inquiries").doc("inq_1").update({
      status: "replied",
      updatedAt: new Date(),
    })
  );
});
