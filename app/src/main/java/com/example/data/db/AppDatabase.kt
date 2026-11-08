package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.BlockedNumberEntity
import com.example.data.model.CallLogEntity
import com.example.data.model.CallType
import com.example.data.model.ContactEntity
import com.example.data.model.SpamReportEntity
import com.example.data.model.SpamRiskLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        ContactEntity::class,
        CallLogEntity::class,
        SpamReportEntity::class,
        BlockedNumberEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "smart_dialer_db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            scope.launch(Dispatchers.IO) {
                                INSTANCE?.let { database ->
                                    seedDatabase(database.appDao())
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun seedDatabase(dao: AppDao) {
            val contacts = listOf(
                ContactEntity(
                    name = "Sheikh Farid",
                    phoneNumber = "+880 1712-345678",
                    email = "farid.dev@smartdialer.io",
                    company = "NoorAI Systems",
                    label = "Work",
                    isFavorite = true,
                    notes = "Lead Architect & AI Engineer",
                    avatarColorIndex = 0
                ),
                ContactEntity(
                    name = "Rubel Sheikh",
                    phoneNumber = "+880 1911-223344",
                    email = "rubelsheikhhero@gmail.com",
                    company = "Tech Hero BD",
                    label = "Mobile",
                    isFavorite = true,
                    notes = "Founder & Tech Pioneer",
                    avatarColorIndex = 1
                ),
                ContactEntity(
                    name = "Tanvir Ahmed",
                    phoneNumber = "+880 1819-876543",
                    email = "tanvir@dialerlabs.org",
                    company = "Robi Telecom Labs",
                    label = "Mobile",
                    isFavorite = true,
                    notes = "Telecom Protocol Lead",
                    avatarColorIndex = 2
                ),
                ContactEntity(
                    name = "Dr. Nusrat Jahan",
                    phoneNumber = "+880 1622-445566",
                    email = "nusrat.hospital@dhaka.med",
                    company = "Square Hospitals",
                    label = "Work",
                    isFavorite = false,
                    notes = "Consultant Physician",
                    avatarColorIndex = 3
                ),
                ContactEntity(
                    name = "Sadia Islam",
                    phoneNumber = "+880 1301-998877",
                    email = "sadia.design@studio.net",
                    company = "Creative Flow",
                    label = "Personal",
                    isFavorite = true,
                    notes = "UI/UX Designer",
                    avatarColorIndex = 4
                ),
                ContactEntity(
                    name = "Mehedi Hasan",
                    phoneNumber = "+880 1552-334455",
                    email = "mehedi@teletalk.gov.bd",
                    company = "National Gateway",
                    label = "Work",
                    isFavorite = false,
                    notes = "Infrastructure Engineer",
                    avatarColorIndex = 5
                ),
                ContactEntity(
                    name = "Ayesha Rahman",
                    phoneNumber = "+880 1745-667788",
                    email = "ayesha@lawchambers.com",
                    company = "Rahman Legal",
                    label = "Mobile",
                    isFavorite = false,
                    notes = "Legal Advisor",
                    avatarColorIndex = 6
                ),
                ContactEntity(
                    name = "Michael Chen",
                    phoneNumber = "+1 415-555-0199",
                    email = "m.chen@siliconvalley.io",
                    company = "Global Mobile Corp",
                    label = "International",
                    isFavorite = false,
                    notes = "San Francisco Partner",
                    avatarColorIndex = 7
                )
            )
            dao.insertContacts(contacts)

            val spamReports = listOf(
                SpamReportEntity(
                    number = "+880 1899-001122",
                    reportedName = "Fake Bank KYC Agent",
                    category = "Scammer",
                    reportsCount = 1420,
                    riskLevel = SpamRiskLevel.HIGH_RISK,
                    country = "Bangladesh",
                    isBlockedByUser = true,
                    notes = "Asks for OTP claiming bKash/Nagad account will be suspended."
                ),
                SpamReportEntity(
                    number = "+880 1600-443322",
                    reportedName = "Instant Loan Telemarketing",
                    category = "Telemarketer",
                    reportsCount = 890,
                    riskLevel = SpamRiskLevel.HIGH_RISK,
                    country = "Bangladesh",
                    isBlockedByUser = true,
                    notes = "Aggressive loan offers without consent."
                ),
                SpamReportEntity(
                    number = "+880 1700-112233",
                    reportedName = "Crypto Lottery Fraud",
                    category = "Loan Fraud",
                    reportsCount = 2150,
                    riskLevel = SpamRiskLevel.HIGH_RISK,
                    country = "Bangladesh",
                    isBlockedByUser = false,
                    notes = "Claims you won 50,000 taka prize."
                ),
                SpamReportEntity(
                    number = "+880 1900-556677",
                    reportedName = "Robocall Insurance Promo",
                    category = "Robocall",
                    reportsCount = 640,
                    riskLevel = SpamRiskLevel.SUSPICIOUS,
                    country = "Bangladesh",
                    isBlockedByUser = false,
                    notes = "Automated voice message selling health schemes."
                ),
                SpamReportEntity(
                    number = "+880 1300-889900",
                    reportedName = "Fake Courier Parcel Delivery",
                    category = "Fake Delivery",
                    reportsCount = 1830,
                    riskLevel = SpamRiskLevel.HIGH_RISK,
                    country = "Bangladesh",
                    isBlockedByUser = true,
                    notes = "Claims parcel held until advance delivery fee sent."
                )
            )
            dao.insertSpamReports(spamReports)

            // Blocked numbers initial seed
            dao.blockNumber(
                BlockedNumberEntity(
                    number = "+880 1899-001122",
                    name = "Fake Bank KYC Agent",
                    reason = "High-Risk Scammer"
                )
            )
            dao.blockNumber(
                BlockedNumberEntity(
                    number = "+880 1600-443322",
                    name = "Instant Loan Telemarketing",
                    reason = "Unsolicited Telemarketing"
                )
            )
            dao.blockNumber(
                BlockedNumberEntity(
                    number = "+880 1300-889900",
                    name = "Fake Courier Parcel Delivery",
                    reason = "Phishing Fraud"
                )
            )

            val now = System.currentTimeMillis()
            val logs = listOf(
                CallLogEntity(
                    number = "+880 1712-345678",
                    cachedName = "Sheikh Farid",
                    type = CallType.INCOMING,
                    timestamp = now - 1000 * 60 * 18, // 18 mins ago
                    durationSeconds = 145,
                    carrier = "Grameenphone",
                    location = "Dhaka, BD",
                    isSpam = false
                ),
                CallLogEntity(
                    number = "+880 1899-001122",
                    cachedName = "Fake Bank KYC Agent",
                    type = CallType.BLOCKED,
                    timestamp = now - 1000 * 60 * 45, // 45 mins ago
                    durationSeconds = 0,
                    carrier = "Robi Axiata",
                    location = "National, BD",
                    isSpam = true,
                    spamCategory = "Scammer"
                ),
                CallLogEntity(
                    number = "+880 1911-223344",
                    cachedName = "Rubel Sheikh",
                    type = CallType.OUTGOING,
                    timestamp = now - 1000 * 60 * 120, // 2 hours ago
                    durationSeconds = 320,
                    carrier = "Banglalink",
                    location = "Dhaka, BD",
                    isSpam = false
                ),
                CallLogEntity(
                    number = "+880 1622-445566",
                    cachedName = "Dr. Nusrat Jahan",
                    type = CallType.MISSED,
                    timestamp = now - 1000 * 60 * 300, // 5 hours ago
                    durationSeconds = 0,
                    carrier = "Airtel",
                    location = "Dhaka, BD",
                    isSpam = false
                ),
                CallLogEntity(
                    number = "+880 1301-998877",
                    cachedName = "Sadia Islam",
                    type = CallType.INCOMING,
                    timestamp = now - 1000 * 60 * 60 * 14, // 14 hours ago
                    durationSeconds = 480,
                    carrier = "Grameenphone",
                    location = "Chittagong, BD",
                    isSpam = false
                ),
                CallLogEntity(
                    number = "+880 1600-443322",
                    cachedName = "Instant Loan Telemarketing",
                    type = CallType.BLOCKED,
                    timestamp = now - 1000 * 60 * 60 * 26, // 1 day ago
                    durationSeconds = 0,
                    carrier = "Airtel",
                    location = "National, BD",
                    isSpam = true,
                    spamCategory = "Telemarketer"
                ),
                CallLogEntity(
                    number = "+1 415-555-0199",
                    cachedName = "Michael Chen",
                    type = CallType.OUTGOING,
                    timestamp = now - 1000 * 60 * 60 * 36, // 1.5 days ago
                    durationSeconds = 620,
                    carrier = "US/CA Cellular",
                    location = "San Francisco, USA",
                    isSpam = false
                )
            )
            dao.insertCallLogs(logs)
        }
    }
}
