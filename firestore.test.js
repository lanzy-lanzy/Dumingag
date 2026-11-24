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

async function setAdminEmail(email, enabled = true) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("admin_emails").doc(email.toLowerCase()).set({ enabled });
  });
}

test("Transit methods: unauthenticated denied, filtered public list works, owner create/update/delete work, cross-user & invalid payloads fail", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await setAdminEmail("alice@example.com");
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {email: "alice@example.com", email_verified: true}).firestore();
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

  // A regular user cannot publish a route.
  await assertFails(
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
  await setAdminEmail(ADMIN_EMAIL);
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
  // Custom managed category code succeeds; malformed category code fails
  await assertSucceeds(
    adminDb.collection("attractions").doc("custom_cat_spot").set({
      ...validAttraction(ADMIN_UID),
      category: "WATERFALLS",
    })
  );
  await assertFails(
    adminDb.collection("attractions").doc("bad_cat").set({
      ...validAttraction(ADMIN_UID),
      category: "bad category!",
    })
  );
  await assertFails(
    adminDb.collection("attractions").doc("all_cat").set({
      ...validAttraction(ADMIN_UID),
      category: "ALL",
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

test("Admin emergency contacts and manual email allowlist: RBAC, email verification, filtered list & validation", async () => {
  await setAdminEmail(ADMIN_EMAIL);
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

  await setAdminEmail("bob@example.com");
  // Promoted Bob can now update/delete emergency contact
  await assertSucceeds(
    bobDb.collection("emergency_contacts").doc("rescue_1").update({
      availability: "24/7 Standby",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertSucceeds(bobDb.collection("emergency_contacts").doc("rescue_1").delete());
});



test("Only operator-approved verified addresses have management access", async () => {
  const email = "chosen@example.com";
  const member = (uid, address, verified, provider) => testEnv.authenticatedContext(uid, {
    email: address, email_verified: verified, firebase: { sign_in_provider: provider }
  }).firestore();
  const google = member("google_uid", "Chosen@example.com", true, "google.com");
  const password = member("password_uid", email, true, "password");
  const unverified = member("new_uid", email, false, "password");
  const regular = member(ADMIN_UID, ADMIN_EMAIL, true, "google.com");
  const directory = db => db.collection("users").get();

  await testEnv.withSecurityRulesDisabled(async ctx => {
    await ctx.firestore().collection("admins").doc(ADMIN_UID).set({userId: ADMIN_UID});
  });
  await assertFails(directory(regular));
  await assertFails(directory(google));
  await assertFails(directory(password));
  await setAdminEmail(email);
  await assertSucceeds(directory(google));
  await assertSucceeds(directory(password));
  await assertSucceeds(google.collection("admin_emails").doc(email).get());
  await assertFails(directory(unverified));
  await assertFails(directory(regular));
  await assertFails(unverified.collection("admin_emails").doc(email).get());
  await assertFails(regular.collection("admin_emails").doc(email).get());
  for (const db of [google, regular, unverified]) {
    await assertFails(db.collection("admin_emails").get());
    await assertFails(db.collection("admin_emails").doc("other@example.com").set({enabled: true}));
    await assertFails(db.collection("admin_emails").doc(email).delete());
  }
  await setAdminEmail(email, false);
  await assertFails(directory(google));
  await assertFails(directory(password));
});

test("User favorites & review star ratings: owner bookmarking works, oversized/non-list/cross-user fail, and rating 1..5 enforced", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      favoriteAttractionIds: ["danlugan_falls"],
      preferredLanguage: "en",
      offlinePackEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Update favorites list
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteAttractionIds: ["danlugan_falls", "organic_village"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Oversized favorites (> 100 items) and non-list rejected
  const oversized = Array.from({ length: 101 }, (_, i) => `spot_${i}`);
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteAttractionIds: oversized,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      favoriteAttractionIds: "not_a_list",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-user favorite mutation denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      favoriteAttractionIds: ["hijack"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Review rating outside 1..5 rejected
  await assertFails(
    aliceDb.collection("reviews").doc("bad_rating_0").set({
      userId: ALICE_UID,
      authorName: "Alice",
      attractionId: "danlugan_falls",
      attractionName: "Danlugan Highland Falls",
      rating: 0,
      comment: "Invalid zero star rating",
      visitTag: "Solo",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );
  await assertFails(
    aliceDb.collection("reviews").doc("bad_rating_6").set({
      userId: ALICE_UID,
      authorName: "Alice",
      attractionId: "danlugan_falls",
      attractionName: "Danlugan Highland Falls",
      rating: 6,
      comment: "Invalid six star rating",
      visitTag: "Solo",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Manual role='admin' via verified email on users/{uid} or admin_emails/{email}: default regular, anti-escalation, and unverified denial", async () => {
  const now = new Date(Date.now() - 5000);
  const verifiedAliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const verifiedBobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const unverifiedAliceDb = testEnv.authenticatedContext("unverified_alice", {
    email: "unverified@example.com",
    email_verified: false,
  }).firestore();

  // Client cannot self-create with role='admin'
  await assertFails(
    verifiedAliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      role: "admin",
      displayName: "Alice",
      preferredLanguage: "en",
      offlinePackEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Client creates normal profile with default role='regular' and verified email
  await assertSucceeds(
    verifiedAliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      email: "alice@example.com",
      role: "regular",
      displayName: "Alice",
      preferredLanguage: "en",
      offlinePackEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob creates profile without explicit role (unset defaults to regular)
  await assertSucceeds(
    verifiedBobDb.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      email: "bob@example.com",
      displayName: "Bob",
      preferredLanguage: "en",
      offlinePackEnabled: false,
      createdAt: now,
      updatedAt: now,
    })
  );

  // Client cannot self-escalate via update
  await assertFails(
    verifiedAliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Regular / unset users cannot list users directory or create emergency contacts
  await assertFails(verifiedAliceDb.collection("users").get());
  await assertFails(verifiedBobDb.collection("users").get());

  // Unverified user with role='admin' set in Firestore STILL cannot act as admin
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("users").doc("unverified_alice").set({
      userId: "unverified_alice",
      email: "unverified@example.com",
      role: "admin",
      displayName: "Unverified",
      preferredLanguage: "en",
      offlinePackEnabled: false,
      createdAt: now,
      updatedAt: now,
    });
    // Operator sets role='admin' on Alice's user doc
    await ctx.firestore().collection("users").doc(ALICE_UID).update({
      role: "admin",
    });
  });

  await assertFails(unverifiedAliceDb.collection("users").get());
  await assertSucceeds(verifiedAliceDb.collection("users").get());

  // Alice (whose role='admin' was set in users/{uid}) can still edit her own displayName/favorites without changing role
  await assertSucceeds(
    verifiedAliceDb.collection("users").doc(ALICE_UID).update({
      displayName: "Admin Alice",
      favoriteAttractionIds: ["danlugan_falls"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Operator sets role='admin' in admin_emails/{email} for Bob (without enabled flag)
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admin_emails").doc("bob@example.com").set({
      role: "admin",
    });
  });
  await assertSucceeds(verifiedBobDb.collection("users").get());
});

test("Admin attraction categories: public filtered list works, verified admin CRUD works, non-admin/unauth/invalid payloads fail", async () => {
  await setAdminEmail(ADMIN_EMAIL);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 5000);

  const validCategory = (ownerUid) => ({
    userId: ownerUid,
    code: "WATERFALLS",
    name: "Waterfalls & Springs",
    iconKey: "water",
    sortOrder: 5,
    visibility: "public",
    createdAt: now,
    updatedAt: now,
  });

  // Unauthenticated and regular users cannot create or read unfiltered
  await assertFails(
    unauthDb.collection("attraction_categories").where("visibility", "==", "public").get()
  );
  await assertFails(
    aliceDb.collection("attraction_categories").doc("WATERFALLS").set(validCategory(ALICE_UID))
  );

  // Verified admin creates category
  await assertSucceeds(
    adminDb.collection("attraction_categories").doc("WATERFALLS").set(validCategory(ADMIN_UID))
  );

  // Signed-in traveler reads filtered public list; unfiltered list denied
  await assertSucceeds(
    aliceDb.collection("attraction_categories").where("visibility", "==", "public").get()
  );
  await assertFails(aliceDb.collection("attraction_categories").get());

  // Verified admin updates name/icon/sortOrder; immutable code change rejected
  await assertSucceeds(
    adminDb.collection("attraction_categories").doc("WATERFALLS").update({
      name: "Highland Waterfalls",
      iconKey: "forest",
      sortOrder: 2,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    adminDb.collection("attraction_categories").doc("WATERFALLS").update({
      code: "NEW_CODE",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Regular user cannot update or delete; invalid code 'ALL' or lowercase code rejected
  await assertFails(
    aliceDb.collection("attraction_categories").doc("WATERFALLS").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("attraction_categories").doc("WATERFALLS").delete());
  await assertFails(
    adminDb.collection("attraction_categories").doc("bad_code").set({
      ...validCategory(ADMIN_UID),
      code: "lowercase_bad",
    })
  );
  await assertFails(
    adminDb.collection("attraction_categories").doc("all_reserved").set({
      ...validCategory(ADMIN_UID),
      code: "ALL",
    })
  );

  // Verified admin delete succeeds
  await assertSucceeds(adminDb.collection("attraction_categories").doc("WATERFALLS").delete());
});

test("Enhanced reviews: photoData, verifiedVisit, helpful upvotes, official admin reply & moderation", async () => {
  await setAdminEmail(ADMIN_EMAIL);
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, {
    email: "bob@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 5000);

  // Regular traveler cannot forge adminReply on create
  await assertFails(
    aliceDb.collection("reviews").doc("rev_forged_reply").set({
      userId: ALICE_UID,
      authorName: "Alice",
      attractionId: "danlugan_falls",
      attractionName: "Danlugan Highland Falls",
      rating: 5,
      comment: "Trying to inject official reply",
      visitTag: "Nature",
      adminReply: "Forged official response",
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Traveler creates photo review with verifiedVisit
  await assertSucceeds(
    aliceDb.collection("reviews").doc("rev_photo_1").set({
      userId: ALICE_UID,
      authorName: "Alice",
      attractionId: "danlugan_falls",
      attractionName: "Danlugan Highland Falls",
      rating: 5,
      comment: "Crystal clear water after the morning hike!",
      visitTag: "Nature",
      photoData: "aGVsbG8tcGhvdG8=",
      verifiedVisit: true,
      helpfulUserIds: [],
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Bob can toggle his OWN UID into helpfulUserIds
  await assertSucceeds(
    bobDb.collection("reviews").doc("rev_photo_1").update({
      helpfulUserIds: [BOB_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob CANNOT spoof Alice's UID or remove someone else's UID
  await assertFails(
    bobDb.collection("reviews").doc("rev_photo_1").update({
      helpfulUserIds: [BOB_UID, ALICE_UID],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Bob can remove his own UID
  await assertSucceeds(
    bobDb.collection("reviews").doc("rev_photo_1").update({
      helpfulUserIds: [],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Regular user cannot post adminReply
  await assertFails(
    bobDb.collection("reviews").doc("rev_photo_1").update({
      adminReply: "Fake official reply",
      adminReplyBy: "Bob",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verified admin posts official reply
  await assertSucceeds(
    adminDb.collection("reviews").doc("rev_photo_1").update({
      adminReply: "Thank you for visiting Danlugan Falls! Trail railings were newly maintained.",
      adminReplyBy: "Dumingag Tourism Office",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Verified admin can moderate/delete a traveler review; non-owner Bob still cannot
  await assertFails(bobDb.collection("reviews").doc("rev_photo_1").delete());
  await assertSucceeds(adminDb.collection("reviews").doc("rev_photo_1").delete());
});

test("Attraction multi-photo galleryPhotos limit & Tourism Events CRUD/RBAC", async () => {
  await setAdminEmail(ADMIN_EMAIL);
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@example.com",
    email_verified: true,
  }).firestore();
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, {
    email: ADMIN_EMAIL,
    email_verified: true,
  }).firestore();
  const now = new Date(Date.now() - 5000);

  const baseSpot = {
    userId: ADMIN_UID,
    mapNodeId: "node_poblacion",
    nameEn: "Timolan View Deck",
    subtitleEn: "Panoramic ridge view",
    descriptionEn: "Highland sunrise deck overlooking Dumingag valley.",
    barangay: "Timolan",
    category: "NATURE",
    latitude: 8.19,
    longitude: 123.33,
    openHours: "05:00 AM - 06:00 PM",
    entryFeeEn: "PHP 20",
    bestTimeEn: "Sunrise",
    transitHintEn: "Habal-habal from Poblacion",
    baseRating: 4.9,
    imageKey: "highland",
    visibility: "public",
    createdAt: now,
    updatedAt: now,
  };
  await assertSucceeds(
    adminDb.collection("attractions").doc("timolan_deck").set({
      ...baseSpot,
      galleryPhotos: ["p1", "p2", "p3", "p4"],
    })
  );
  await assertFails(
    adminDb.collection("attractions").doc("timolan_deck_too_many").set({
      ...baseSpot,
      galleryPhotos: ["p1", "p2", "p3", "p4", "p5"],
    })
  );

  const validEvent = (ownerUid) => ({
    userId: ownerUid,
    title: "Dumingag Organic Harvest Festival",
    summary: "Heirloom rice showcase, farm-to-table tasting, and cultural dances at Poblacion Plaza.",
    eventType: "FESTIVAL",
    severity: "INFO",
    dateLabel: "Every 1st Saturday · 07:00 AM",
    locationName: "Dumingag Public Plaza",
    attractionId: "organic_village",
    active: true,
    visibility: "public",
    createdAt: now,
    updatedAt: now,
  });

  await assertFails(
    unauthDb.collection("tourism_events").where("visibility", "==", "public").get()
  );
  await assertFails(
    aliceDb.collection("tourism_events").doc("evt_1").set(validEvent(ALICE_UID))
  );
  await assertSucceeds(
    adminDb.collection("tourism_events").doc("evt_1").set(validEvent(ADMIN_UID))
  );
  await assertSucceeds(
    aliceDb.collection("tourism_events").where("visibility", "==", "public").get()
  );
  await assertFails(aliceDb.collection("tourism_events").get());

  await assertSucceeds(
    adminDb.collection("tourism_events").doc("evt_1").update({
      severity: "ADVISORY",
      active: false,
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(aliceDb.collection("tourism_events").doc("evt_1").delete());
  await assertSucceeds(adminDb.collection("tourism_events").doc("evt_1").delete());
});



