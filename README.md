# PetCare

PetCare is an Android app (Kotlin, Room, MVVM) that helps pet owners plan and share their pets' daily care.

## Features and where to find them

| Requirement | In the app | Code |
|---|---|---|
| Home screen with Login / Signup | Splash → entry screen with **Create Account** and **Sign In** | `ui/auth/EntryActivity` |
| Registration / login | Single-screen sign-up (name, email, password + confirmation), SHA-256 hashed passwords, saved session, password reset (email + name) | `ui/auth/SignUpActivity`, `LoginActivity`, `util/SessionManager` |
| Create a pet care routine | **Plan → + New Routine** (or **+ → Care Routine**): name, repeat every day or on chosen weekdays, timed steps (feeding, walk, medication, …) | `ui/routine/RoutineEditorActivity` |
| Manage items – edit | Tap a routine, checklist step, reminder, appointment or vaccination to edit it | `ui/dashboard/CareItemDialogs` |
| Manage items – delete | Bin icon on every item, with **Undo**; routines and pets ask for confirmation | `ui/dashboard/MainActivity` |
| Manage items – mark as completed | Tick steps in **Today's Checklist** (resets each day), reminders and appointments | `TaskAdapter`, `PlanItemAdapter` |
| Item delegation by SMS | **Home → Share Plan**: pick a pet, choose *Care checklist*, *Feeding schedule*, *Medication reminder* or *Daily care routine*, type a number or pick a contact, edit the text, send via the SMS app | `ui/delegate/DelegateActivity` |
| Desirable: Geotagging | **Explore** map shows nearby vets, groomers, dog parks, pet stores and shelters (OpenStreetMap). Long-press the map or tap **Save to My Places** to tag a place. Link appointments and activities to a tagged place; its card lists them, and **View on map** opens it from an appointment | `ui/map/PetMapActivity`, `data/model/LocationEntity` |

Also: multi-step pet onboarding, pet profiles with photos, activity log, vaccinations, and a shareable PDF pet report.

## Demo account

On first launch the app seeds a demo user with two pets, routines, a reminder, an appointment linked to a tagged vet clinic, and a vaccination:

- Email: `emily@petcare.com`
- Password: `Password1!`

## Design system

All UI values live in resources, so screens stay consistent:

| File | Contents |
|---|---|
| `res/values/colors.xml` | Palette: warm pumpkin `primary` (4.7:1 contrast with white text), navy `secondary`, cream `background`, white `surface`, text colors, and status pairs (`success` / `warning` / `error` / `info` + their `_container` tints) |
| `res/values/dimens.xml` | 4dp spacing scale (`space_xs` to `space_4xl`), corner radii, elevation, component sizes (48dp touch targets), type scale |
| `res/values/styles.xml` | Text appearances (`Display`, `Headline`, `Title`, `Subtitle`, `Body`, `Caption`, `Label`), buttons (`Widget.PetCare.Button`, `.Tonal`, `.Outlined`, `.Text`, `.Danger`, `IconButton`), cards (`Widget.PetCare.Card`, `.Elevated`, `.Tinted`), inputs, chips, status badge, settings row, stat tile, progress bar, bottom navigation, dialogs |
| `res/values/themes.xml` | Material 3 theme wiring the palette, Poppins font and default component styles |
| `res/font/` | Poppins (SIL Open Font License, see `assets/licenses/poppins_OFL.txt`) |
| `res/drawable/` | Shapes (`bg_badge`, `bg_icon_tile`, `bg_circle`, `bg_pill`, `bg_sheet_top`, gradients), Material vector icons, PetCare logo (`ic_logo`, launcher icon) |

Status badges use one color pair per meaning: blue *Upcoming*, green *Completed / Done / Up to date*, amber *Pending / Due soon / Past date*, red *Overdue*.

## Project structure

- `data/model` – Room entities (users, pets, routines, tasks, reminders, appointments, activity logs, vaccinations, locations)
- `data/local` – DAOs, `AppDatabase`, demo data seeder
- `data/repository` – `PetRepository`
- `ui` – activities, adapters, `PetViewModel`
- `util` – session, hashing, time formatting, option lists, image helpers

## Building

Open the project in Android Studio and run the `app` configuration. From a terminal, Gradle needs JDK 17+ (for example Android Studio's bundled JBR):

```
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
gradlew assembleDebug
```
