# MyCycleCoach

## Plan 

# Low Level Design - Training Platform Backend

## System Overview
A REST API backend service that manages user training data, integrates with Strava, and generates personalized training plans through external AI services.

---

## Technology Stack Assumptions
- **Language**: Java/Spring Boot or Node.js/Express
- **Database**: PostgreSQL
- **Authentication**: JWT tokens
- **Message Queue**: RabbitMQ/Kafka (for async operations)
- **External APIs**: Strava API, Training Plan Generator API

---

## Architecture Components

### 1. Authentication Feature (`/auth`)

**Controller** (`AuthController`)
- `POST /api/auth/register` - Create new user account
- `POST /api/auth/login` - Authenticate and return JWT
- `POST /api/auth/refresh` - Refresh expired token
- `POST /api/auth/logout` - Invalidate token

**Service** (`AuthService`)
- `registerUser(email, password, profile)` - Hash password, create user
- `authenticateUser(email, password)` - Validate credentials, generate JWT
- `refreshToken(refreshToken)` - Issue new access token
- `validateToken(token)` - Verify JWT signature and expiration

**Domain**
- `User` - id, email, passwordHash, createdAt, updatedAt
- `RefreshToken` - id, userId, token, expiresAt

**Repository** (`UserRepository`)
- `create(user)` - Insert user record
- `findByEmail(email)` - Query user by email
- `findById(userId)` - Retrieve user by ID
- `updateLastLogin(userId)` - Update login timestamp

---

### 2. Training History Feature (`/training-history`)

**Controller** (`TrainingHistoryController`)
- `GET /api/training/history` - Get user's training history (paginated)
- `GET /api/training/history/{id}` - Get specific session details
- `GET /api/training/history/stats` - Get aggregated statistics

**Service** (`TrainingHistoryService`)
- `getUserTrainingHistory(userId, filters, pagination)` - Fetch filtered history
- `getSessionDetails(sessionId, userId)` - Retrieve single session
- `calculateStats(userId, dateRange)` - Compute metrics (total distance, avg pace, etc.)

**Domain**
- `TrainingSession` - id, userId, date, type, distance, duration, pace, heartRate, notes, source, createdAt

**Repository** (`TrainingHistoryRepository`)
- `findByUserId(userId, filters, pagination)` - Query sessions with filters
- `findById(sessionId)` - Get session by ID
- `getStatsByDateRange(userId, startDate, endDate)` - Aggregate data

---

### 3. Training Plan Feature (`/training-plan`)

**Controller** (`TrainingPlanController`)
- `GET /api/training/plan/current` - Get active training plan
- `GET /api/training/plan/upcoming` - Get scheduled sessions
- `GET /api/training/plan/past` - Get completed planned sessions
- `POST /api/training/plan/generate` - Trigger plan generation
- `PUT /api/training/plan/session/{id}` - Mark session complete/skip

**Service** (`TrainingPlanService`)
- `getCurrentPlan(userId)` - Fetch active plan
- `getUpcomingSessions(userId, days)` - Retrieve future sessions
- `getPastScheduledSessions(userId, pagination)` - Get historical plan sessions
- `markSessionComplete(sessionId, userId, actualData)` - Update session status
- `skipSession(sessionId, userId, reason)` - Mark session as skipped

**Domain**
- `TrainingPlan` - id, userId, startDate, endDate, goal, status, generatedAt
- `PlannedSession` - id, planId, scheduledDate, type, distance, duration, intensity, status, completedAt

**Repository** (`TrainingPlanRepository`)
- `findActivePlanByUserId(userId)` - Get current plan
- `savePlan(plan)` - Persist new plan
- `findById(planId)` - Retrieve plan by ID

(`PlannedSessionRepository`)
- `findByPlanId(planId, filters)` - Get sessions for plan
- `findUpcoming(userId, fromDate, toDate)` - Query future sessions
- `updateStatus(sessionId, status, data)` - Update session state

---

### 4. Strava Integration Feature (`/strava`)

**Controller** (`StravaController`)
- `GET /api/strava/connect` - Redirect to Strava OAuth
- `GET /api/strava/callback` - Handle OAuth callback
- `DELETE /api/strava/disconnect` - Remove Strava connection
- `POST /api/strava/sync` - Manually trigger sync

**Service** (`StravaService`)
- `initiateOAuth(userId)` - Generate OAuth URL
- `handleCallback(code, userId)` - Exchange code for tokens
- `syncActivities(userId)` - Pull recent activities
- `disconnectAccount(userId)` - Revoke tokens

**Domain**
- `StravaConnection` - id, userId, accessToken, refreshToken, expiresAt, athleteId, connected

**Client** (`StravaClient`)
- `exchangeToken(code)` - OAuth token exchange
- `refreshAccessToken(refreshToken)` - Refresh expired token
- `getActivities(accessToken, after, page)` - Fetch activity data
- `revokeAccess(accessToken)` - Disconnect application

**Consumer** (`StravaSyncConsumer`)
- Listens to: `strava.sync.requested`
- Processes: Periodic sync jobs
- Publishes: `training.session.created` for each new activity

**Publisher** (`StravaEventPublisher`)
- `publishSyncRequest(userId)` - Queue sync job
- `publishActivitiesImported(userId, count)` - Notify of import completion

**Repository** (`StravaConnectionRepository`)
- `save(connection)` - Store/update connection
- `findByUserId(userId)` - Get user's Strava connection
- `delete(userId)` - Remove connection

---

### 5. User Profile Feature (`/user-profile`)

**Controller** (`UserProfileController`)
- `GET /api/user/profile` - Get user profile and preferences
- `PUT /api/user/profile` - Update profile information
- `POST /api/user/background` - Submit training background
- `PUT /api/user/goals` - Update training goals

**Service** (`UserProfileService`)
- `getProfile(userId)` - Retrieve profile data
- `updateProfile(userId, profileData)` - Modify profile
- `saveTrainingBackground(userId, background)` - Store experience level, history
- `updateGoals(userId, goals)` - Set/update training objectives

**Domain**
- `UserProfile` - id, userId, age, weight, experienceLevel, updatedAt
- `TrainingBackground` - id, userId, yearsTraining, weeklyVolume, recentInjuries, priorEvents
- `TrainingGoals` - id, userId, goals
