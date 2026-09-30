# PetCare Overhaul Implementation Plan (Updated)

Enhancing the PetCare application with a cohesive dark-green brand identity, expressive UI, multi-step pet management, analytics, and productivity features.

## User Review Required

> [!IMPORTANT]
> The application will be transitioned to a **dark-first visual system**. The current light cream background will be replaced with a deep green/near-black theme.
> Multi-step flows for Signup and Pet Creation will replace existing single-form layouts.
> Fully functional and persistent care entities (Reminders, Appointments, Activity Logs, Vaccinations) and Account Lifecycle actions (Edit Profile, Change Password, Delete Account) will be introduced.

## Proposed Changes

### Phase 1: Brand Identity, Colors, Themes & Typography
- **[MODIFY] [colors.xml](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/res/values/colors.xml)**: Define full PetCare dark-green brand palette.
- **[MODIFY] [themes.xml](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/res/values/themes.xml)**: Transition base theme to a premium dark theme. Set proper styles for text inputs, buttons, and backgrounds.
- **[NEW] [font_resources](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/res/font)**: Setup typography settings and custom font mappings if applicable.

### Phase 2: Persistence Layer Overhaul (Data Models & Room Migrations)
- **[NEW] [UserEntity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/data/model/UserEntity.kt)**: Table for user profiles (id, name, email, passwordHash, createdAt, updatedAt).
- **[MODIFY] [PetEntity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/data/model/PetEntity.kt)**: Expand with gender, birthday, sterilized, fur color, body size, health info, emergency and veterinarian contact information.
- **[NEW] [Care entities](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/data/model)**:
    - `ReminderEntity`: Tasks/alerts linked to `petId`.
    - `AppointmentEntity`: Veterinary visits linked to `petId`.
    - `ActivityLogEntity`: Tracking items (Walk, Feeding, etc.) linked to `petId`.
    - `VaccinationEntity`: Records linked to `petId`.
- **[MODIFY] [AppDatabase](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/data/local/AppDatabase.kt)**: Add new entities, DAOs (`UserDao`, `ReminderDao`, `AppointmentDao`, `ActivityDao`, `VaccinationDao`), and implement a clean migration or version increase.

### Phase 3: Authentication & Multi-Step Account Creation
- **[MODIFY] [SplashActivity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/ui/auth/SplashActivity.kt)**: Soft green gradient animation, redirecting based on session status.
- **[MODIFY] [EntryActivity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/res/layout/activity_entry.xml)**: High-quality expressive layout with hero branding, Sign In, Create Account, and isolated "Continue with Google" actions.
- **[MODIFY] [LoginActivity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/ui/auth/LoginActivity.kt)**: Overhaul with dark inputs, validation feedback, and password toggle.
- **[MODIFY] [SignUpActivity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/ui/auth/SignUpActivity.kt)**: Transform into a 2-step onboarding wizard. Step 1: Name + Email. Step 2: Password strength indicator check. No auto-login; forces navigating back to Login with Toast confirmation.

### Phase 4: Main Navigation & Expressive Dashboard
- **[MODIFY] [MainActivity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/ui/dashboard/MainActivity.kt)**: Bottom navigation implementation (Today, Pets, Plan, Explore, Profile tabs).
- **[MODIFY] [activity_main.xml](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/res/layout/activity_main.xml)**: Complete UI redesign incorporating Greeting, Active Pet Hero carousel, Circular/Arc Care Progress indicator, upcoming reminders, and floating panel quick actions.
- **[NEW] Post-login animation**: Smooth fade and entry scale for cards upon initialization.

### Phase 5: Pet Management & Creation Wizard
- **[NEW] Pet Wizard Activity**: Onboarding wizard consisting of structured steps (Type selection -> Photo Selection via modern Activity Result API for Camera/Gallery -> Basic info -> Physical metrics -> Health details -> Emergency/Vet data -> Summary Review -> Save Profile).
- **[MODIFY] [PetDetailActivity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/ui/profile/PetDetailActivity.kt)**: Refactor into an elegant personal medical record screen with tabs/sections for Overview, Health info, Vaccinations, and a button to view advanced metrics.

### Phase 6: Care Plan, Tasks, Reminders & Activity Logging
- **[MODIFY] Plan Tab/Section**: Filter tasks by selected pet, display upcoming events, vaccination reminders, and log activities (Walk, Feeding, Grooming, etc.) to the database.
- **[NEW] Action Sheet/Bottom Sheet**: Interactive + quick action selector sheet.

### Phase 7: Pet Analytics & Custom Charts
- **[NEW] PetAnalyticsActivity**: Dynamic reporting dashboard utilizing real data queries.
- **[NEW] Custom Canvas Charts**: Line/Area graph for activity trend, segmented bar chart for task completion, weight metrics history tracker. No external fat dependencies.

### Phase 8: PDF Report Generation & Document Sharing
- **[NEW] ReportUtils / PDF Generator**: Produce high-quality downloadable reports (`Complete Profile`, `Medical Report`, `Activity & Performance`) via native `PdfDocument`.
- **[MODIFY] Sharing**: Prompt file save via `ACTION_CREATE_DOCUMENT` or share across messaging apps via `Intent.ACTION_SEND` and `FileProvider`.

### Phase 9: Profile, Settings, & Full Account Lifecycle
- **[NEW] Profile Fragment/Activity View**: Edit profile details (Full Name updates), Change Password with validation strength checks, display "Member Since" timestamps, clear session + database drop on Delete Account, and clear activity stack on Logout.

### Phase 10: Explore / Map Placeholder & Demo Data Seeder
- **[MODIFY] [PetMapActivity](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/ui/map/PetMapActivity.kt)**: Build a polished dark-themed category exploration placeholder experience, ready for Google Maps SDK inclusion.
- **[NEW] [DemoDataSeeder](file:///C:/Users/dkshp/AndroidStudioProjects/PetCare/app/src/main/java/com/petcare/app/data/local/DemoDataSeeder.kt)**: Setup seeding for canonical demo scenario (Emily, Max, Luna, Daniel) along with historic activities and records for analytics visualization.

## Verification Plan

### Automated Tests
- Full project compilation check via `./gradlew assembleDebug`.
- Room foreign key constraint verification.

### Manual Verification
- Verify database cascading behavior (deleting pet cleans tasks/logs; deleting account cleans all local tables).
- Verify dark-theme colors and input layouts.
- Test PDF generation, local document downloads, and SMS sharing intent verification.
