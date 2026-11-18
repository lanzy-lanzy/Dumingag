const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
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

test("Unauthenticated user: denied access to profiles, itineraries, and reviews", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("itineraries")
      .get()
  );
  await assertFails(
    unauthDb.collection("reviews").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("reviews").doc("rev_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      attractionId: "san_isidro_church",
      attractionName: "San Isidro Labrador Parish Church",
      rating: 5,
      comment: "Peaceful town plaza!",
      visitTag: "Family",
      visibility: "public",
      createdAt: new Date(Date.now() - 1000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("User profile: owner create and partial update succeed, cross-user and ghost fields fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Reyes",
      preferredLanguage: "en",
      offlinePackEnabled: true,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "ceb",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user read/write blocked
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Ghost field & invalid language blocked
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      preferredLanguage: "fr",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Itineraries: owner CRUD & list succeed; cross-user and immutable mutations fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("itineraries")
      .doc("stop_1")
      .set({
        userId: ALICE_UID,
        attractionId: "danlugan_falls",
        attractionName: "Danlugan Highland Falls",
        plannedDateMillis: Date.now() + 86400000,
        notes: "Bring waterproof sandals",
        reminderEnabled: true,
        calendarSynced: false,
        createdAt: now,
        updatedAt: now,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("itineraries")
      .get()
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("itineraries")
      .doc("stop_1")
      .update({
        calendarSynced: true,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  // Cross-user denied
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("itineraries")
      .get()
  );

  // Immutable createdAt mutation denied
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("itineraries")
      .doc("stop_1")
      .update({
        createdAt: new Date(Date.now() - 500),
        updatedAt: new Date(Date.now() - 500),
      })
  );
});

test("Community reviews: public query works, owner create/update/delete work, unfiltered list & cross-user edit fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_alice_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      attractionId: "organic_village",
      attractionName: "Dumingag Organic Village",
      rating: 5,
      comment: "Inspiring heirloom rice terraces and warm hospitality!",
      visitTag: "Eco-Tour",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered public query (matches Kotlin repository) succeeds
  await assertSucceeds(
    bobDb.collection("reviews").where("visibility", "==", "public").get()
  );

  // Owner update succeeds
  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_alice_1").update({
      rating: 4,
      comment: "Updated: great morning views!",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user update and delete fail
  await assertFails(
    bobDb.collection("reviews").doc("rev_alice_1").update({
      comment: "Vandalized",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(bobDb.collection("reviews").doc("rev_alice_1").delete());

  // Unfiltered list on reviews fails (rules enforce visibility or userId)
  await assertFails(bobDb.collection("reviews").get());

  // Extra @DocumentId `id` field rejected
  await assertFails(
    bobDb.collection("reviews").doc("rev_bob_1").set({
      id: "rev_bob_1",
      userId: BOB_UID,
      authorName: "Bob",
      attractionId: "organic_village",
      attractionName: "Dumingag Organic Village",
      rating: 5,
      comment: "Invalid id field test",
      visitTag: "Solo",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Transit methods: unauthenticated denied, filtered public list works, owner create/update/delete work, cross-user & invalid payloads fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  // Unauthenticated reads & writes fail
  await assertFails(
    unauthDb.collection("transit_methods").where("visibility", "==", "public").get()
  );
  await assertFails(
    unauthDb.collection("transit_methods").doc("tricycle_poblacion").set({
      userId: ALICE_UID,
      name: "Poblacion Tricycle & Bao-Bao",
      modeCategory: "TRICYCLE",
      operatorName: "Dumingag TODA",
      routeSummary: "Poblacion town center to Lower Landing",
      scheduleText: "05:00 AM - 09:00 PM daily",
      fareRangePhp: "PHP 15 - 35",
      durationText: "5 - 12 mins",
      terminalLocation: "Dumingag Public Market Plaza",
      travelTip: "Standard fare PHP 15 within Poblacion.",
      serviceStatus: "OPERATIONAL",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Valid Kotlin-aligned create payload (no `id` field, optional travelTip included)
  await assertSucceeds(
    aliceDb.collection("transit_methods").doc("tricycle_poblacion").set({
      userId: ALICE_UID,
      name: "Poblacion Tricycle & Bao-Bao",
      modeCategory: "TRICYCLE",
      operatorName: "Dumingag TODA",
      routeSummary: "Poblacion town center to Lower Landing",
      scheduleText: "05:00 AM - 09:00 PM daily",
      fareRangePhp: "PHP 15 - 35",
      durationText: "5 - 12 mins",
      terminalLocation: "Dumingag Public Market Plaza",
      travelTip: "Standard fare PHP 15 within Poblacion.",
      serviceStatus: "OPERATIONAL",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Create without optional travelTip also succeeds
  await assertSucceeds(
    bobDb.collection("transit_methods").doc("habal_upland").set({
      userId: BOB_UID,
      name: "Upland Habal-Habal",
      modeCategory: "HABAL_HABAL",
      operatorName: "San Pablo Riders Assoc.",
      routeSummary: "Danlugan Falls, Mt. Timolan & Lipawan Cave",
      scheduleText: "05:30 AM - 04:30 PM",
      fareRangePhp: "PHP 80 - 150",
      durationText: "20 - 35 mins",
      terminalLocation: "Public Market & San Pablo Junction",
      serviceStatus: "OPERATIONAL",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Filtered public query (matches Kotlin repository) succeeds
  await assertSucceeds(
    bobDb.collection("transit_methods").where("visibility", "==", "public").get()
  );

  // Unfiltered list on transit_methods fails
  await assertFails(bobDb.collection("transit_methods").get());

  // Owner partial update succeeds
  await assertSucceeds(
    aliceDb.collection("transit_methods").doc("tricycle_poblacion").update({
      fareRangePhp: "PHP 15 - 40",
      serviceStatus: "ADVISORY",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user update and delete fail
  await assertFails(
    bobDb.collection("transit_methods").doc("tricycle_poblacion").update({
      fareRangePhp: "PHP 999",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    bobDb.collection("transit_methods").doc("tricycle_poblacion").delete()
  );

  // Invalid modeCategory, ghost field, and extra @DocumentId `id` rejected
  await assertFails(
    aliceDb.collection("transit_methods").doc("invalid_mode").set({
      userId: ALICE_UID,
      name: "Helicopter",
      modeCategory: "HELICOPTER",
      operatorName: "Test",
      routeSummary: "Test route",
      scheduleText: "Always",
      fareRangePhp: "PHP 5000",
      durationText: "5 mins",
      terminalLocation: "Plaza",
      serviceStatus: "OPERATIONAL",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("transit_methods").doc("with_id_field").set({
      id: "with_id_field",
      userId: ALICE_UID,
      name: "Pagadian Van",
      modeCategory: "BUS_VAN",
      operatorName: "UV Express",
      routeSummary: "Pagadian - Dumingag",
      scheduleText: "05:00 AM - 05:30 PM",
      fareRangePhp: "PHP 100",
      durationText: "1 hr 15 mins",
      terminalLocation: "Poblacion Terminal",
      serviceStatus: "OPERATIONAL",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Owner delete succeeds
  await assertSucceeds(
    aliceDb.collection("transit_methods").doc("tricycle_poblacion").delete()
  );
});

const ADMIN_UID = "admin_bren_001";
const ADMIN_EMAIL = "bigbren480@gmail.com";

test("User profile DP & displayName: owner update with photoData succeeds, oversized photoData and cross-user fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Reyes",
      photoData: "data:image/jpeg;base64,/9j/4AAQSkZJRg==",
      preferredLanguage: "en",
      offlinePackEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Alice R. Explorer",
      photoData: "aGVsbG8td29ybGQtanBlZw==",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Oversized photoData (> 65536 chars) rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      photoData: "x".repeat(70000),
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user DP update denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      photoData: "evil",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});

test("Admin attractions: verified admin CRUD works, unverified spoof & regular user fail, filtered public list works", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 5000);

  const validAttraction = (ownerUid) => ({
    userId: ownerUid,
    mapNodeId: "node_poblacion",
    nameEn: "Gintong Ani Eco Hub",
    subtitleEn: "Heirloom Rice Demo Plot",
    descriptionEn: "Community organic terrace learning center in Dumingag.",
    historyNoteEn: "Founded by local organic farmer cooperatives.",
    barangay: "Lower Landing",
    category: "ORGANIC_FARM",
    latitude: 8.1742,
    longitude: 123.362,
    openHours: "07:00 AM - 05:00 PM",
    entryFeeEn: "PHP 25",
    bestTimeEn: "Morning",
    transitHintEn: "Tricycle from Poblacion Plaza",
    baseRating: 4.8,
    imageKey: "organic",
    visibility: "public",
    createdAt: now,
    updatedAt: now,
  });

  // Regular user & spoofed unverified admin cannot create
  await assertFails(
    aliceDb.collection("attractions").doc("gintong_ani").set(validAttraction(ALICE_UID))
  );
  await assertFails(
    spoofAdminDb.collection("attractions").doc("gintong_ani").set(validAttraction("spoof_uid"))
  );

  // Verified admin creates attraction
  await assertSucceeds(
    adminDb.collection("attractions").doc("gintong_ani").set(validAttraction(ADMIN_UID))
  );

  // Unauthenticated query rejected; regular user filtered public query succeeds; unfiltered list fails
  await assertFails(
    unauthDb.collection("attractions").where("visibility", "==", "public").get()
  );
  await assertSucceeds(
    aliceDb.collection("attractions").where("visibility", "==", "public").get()
  );
  await assertFails(aliceDb.collection("attractions").get());

  // Verified admin update succeeds; regular user update fails
  await assertSucceeds(
    adminDb.collection("attractions").doc("gintong_ani").update({
      entryFeeEn: "Free entry",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("attractions").doc("gintong_ani").update({
      entryFeeEn: "PHP 9999",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Validation checks: invalid latitude, invalid category, extra id field fail
  await assertFails(
    adminDb.collection("attractions").doc("bad_lat").set({
      ...validAttraction(ADMIN_UID),
      latitude: 120.0,
    })
  );
  await assertFails(
    adminDb.collection("attractions").doc("bad_cat").set({
      ...validAttraction(ADMIN_UID),
      category: "SPACE_PORT",
    })
  );
  await assertFails(
    adminDb.collection("attractions").doc("with_id").set({
      ...validAttraction(ADMIN_UID),
      id: "with_id",
    })
  );

  // Regular user delete fails; admin delete succeeds
  await assertFails(aliceDb.collection("attractions").doc("gintong_ani").delete());
  await assertSucceeds(adminDb.collection("attractions").doc("gintong_ani").delete());
});

test("Admin emergency contacts & /admins grant: RBAC, email verification, filtered list & validation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const spoofAdminDb = testEnv.authenticatedContext("spoof_uid", {
    email: ADMIN_EMAIL,
    email_verified: false,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 5000);

  const validContact = (ownerUid) => ({
    userId: ownerUid,
    titleEn: "Dumingag Rescue 101",
    department: "MDRRMC Response Team",
    phoneNumber: "0917-555-0101",
    availability: "24/7 Dispatch",
    priorityLevel: "CRITICAL",
    email: "rescue@dumingag.com",
    websiteUrl: "https://dumingag.com",
    visibility: "public",
    createdAt: now,
    updatedAt: now,
  });

  await assertFails(
    aliceDb.collection("emergency_contacts").doc("rescue_1").set(validContact(ALICE_UID))
  );
  await assertFails(
    spoofAdminDb.collection("emergency_contacts").doc("rescue_1").set(validContact("spoof_uid"))
  );
  await assertSucceeds(
    adminDb.collection("emergency_contacts").doc("rescue_1").set(validContact(ADMIN_UID))
  );

  await assertSucceeds(
    aliceDb.collection("emergency_contacts").where("visibility", "==", "public").get()
  );
  await assertFails(aliceDb.collection("emergency_contacts").get());

  await assertSucceeds(
    adminDb.collection("emergency_contacts").doc("rescue_1").update({
      phoneNumber: "0917-555-0199",
      priorityLevel: "HIGH",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertFails(
    adminDb.collection("emergency_contacts").doc("bad_prio").set({
      ...validContact(ADMIN_UID),
      priorityLevel: "ASSISTANCE",
    })
  );

  // Admin grants Bob admin access via /admins/{BOB_UID}
  await assertFails(aliceDb.collection("admins").get());
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      grantedBy: ALICE_UID,
      createdAt: now,
    })
  );
  await assertSucceeds(
    adminDb.collection("admins").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      grantedBy: ADMIN_UID,
      createdAt: now,
    })
  );

  // Promoted Bob can now update/delete emergency contact
  await assertSucceeds(
    bobDb.collection("emergency_contacts").doc("rescue_1").update({
      availability: "24/7 Standby",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(bobDb.collection("emergency_contacts").doc("rescue_1").delete());
});


