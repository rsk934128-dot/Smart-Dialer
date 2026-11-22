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

test("Unauthenticated user: denied from reading or writing user profile, call logs, contacts, blocked numbers, and spam reports", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Unauthorized",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("call_logs").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("blocked_numbers").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("spam_reports").get()
  );
});

test("User profile: owner create, partial update, immutable guard, and cross-user denial", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      email: "alice@example.com",
      backedUpLogsCount: 2,
      backedUpContactsCount: 5,
      createdAt: created,
      updatedAt: created,
    })
  );

  // Partial update allowed
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      backedUpLogsCount: 4,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject ghost field & createdAt tampering
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user isolation
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      displayName: "Hacked",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("CloudCallLog: valid Kotlin-mirrored create, update, list query, and negative validation checks", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 10000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("call_logs")
      .doc("log_1")
      .set({
        userId: ALICE_UID,
        number: "+8801712345678",
        cachedName: "Sheikh Farid",
        callType: "INCOMING",
        callTimestampMs: Date.now() - 60000,
        durationSeconds: 45,
        carrier: "Grameenphone",
        location: "Dhaka, BD",
        spamFlag: false,
        spamCategory: "",
        simSlot: 1,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Filtered/sorted list query on owner subcollection
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("call_logs")
      .orderBy("callTimestampMs", "desc")
      .get()
  );

  // Reject invalid simSlot (outside 1..2)
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("call_logs")
      .doc("log_bad_sim")
      .set({
        userId: ALICE_UID,
        number: "+8801712345678",
        callType: "OUTGOING",
        callTimestampMs: Date.now() - 60000,
        durationSeconds: 10,
        spamFlag: false,
        simSlot: 3,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject @DocumentId "id" key in payload
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("call_logs")
      .doc("log_with_id")
      .set({
        id: "log_with_id",
        userId: ALICE_UID,
        number: "+8801712345678",
        callType: "INCOMING",
        callTimestampMs: Date.now() - 60000,
        durationSeconds: 10,
        spamFlag: false,
        simSlot: 1,
        createdAt: created,
        updatedAt: created,
      })
  );

  // Reject tampered future timestamp
  await assertFails(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("call_logs")
      .doc("log_future_ts")
      .set({
        userId: ALICE_UID,
        number: "+8801712345678",
        callType: "INCOMING",
        callTimestampMs: Date.now() - 60000,
        durationSeconds: 10,
        spamFlag: false,
        simSlot: 1,
        createdAt: new Date(Date.now() + 60000),
        updatedAt: created,
      })
  );

  // Bob cannot list or delete Alice's call logs
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("call_logs").get()
  );
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("call_logs")
      .doc("log_1")
      .delete()
  );
});

test("Contacts, BlockedNumbers, and SpamReports: owner CRUD succeeds and cross-user fails", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("contacts")
      .doc("c_1")
      .set({
        userId: ALICE_UID,
        name: "Rubel Sheikh",
        phoneNumber: "+8801911223344",
        email: "rubel@example.com",
        company: "Tech Hero BD",
        label: "Mobile",
        favorite: true,
        notes: "Founder",
        avatarColorIndex: 1,
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("contacts")
      .doc("c_1")
      .update({
        favorite: false,
        updatedAt: new Date(Date.now() - 1000),
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("blocked_numbers")
      .doc("b_1")
      .set({
        userId: ALICE_UID,
        number: "+8801899001122",
        name: "Scammer",
        reason: "Fraud",
        createdAt: created,
        updatedAt: created,
      })
  );

  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("spam_reports")
      .doc("s_1")
      .set({
        userId: ALICE_UID,
        number: "+8801899001122",
        reportedName: "Fake KYC Agent",
        category: "Scammer",
        reportsCount: 1,
        riskLevel: "HIGH_RISK",
        notes: "OTP scam",
        createdAt: created,
        updatedAt: created,
      })
  );

  // Cross-user access denied
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("contacts").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("blocked_numbers").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("spam_reports").get()
  );
});
