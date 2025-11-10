# Sprint 3 Implementation Notes

## Dashboard Analytics
- Added MPAndroidChart dependency and embedded `PieChart` and `BarChart` widgets in `dashboard.xml` to visualize monthly spending and budget usage.
- Introduced `DashboardAnalyticsViewModel` backed by `AnalyticsRepository` to provide chart-ready `LiveData` with seeded fallbacks for empty datasets.
- Created `BudgetUsageSummary` model to surface allocated vs. spent amounts for each active budget.

## Savings Circles
- Implemented invitation workflow with `InvitationsActivity`, dialog UI, and `SavingCircleViewModel` helpers for send/accept/decline.
- Added Firebase-backed invitation storage under `invitations/{sanitizedEmail}` with lifecycle cleanup and empty-state support.
- Tuned cycle math by resetting allocations on rollover and syncing expense adjustments to both member balances and cycle history.

## Testing
- Added `AnalyticsRepositoryTest` unit coverage for category aggregation and per-budget usage calculations across date windows.
- Added `MemberCycleTest` to validate cycle rollovers and expense restoration logic.
- Gradle commands may require a local Android SDK; run `bash ./gradlew :app:testDebugUnitTest` after setting `local.properties` to execute the new tests.

1
CS 2340 Sprint 3
Dashboard Visualizations, Group Features, DCD, SD, SOLID, and Design Patterns
Due Date: See Canvas assignment
Background
This semester’s project is SpendWise: A Personal Finance & Budgeting App, designed to help users plan
budgets and track day-to-day spending with a clean, student-friendly workflow.
Purpose
We put you through all this with two goals in mind: one, to practice collaborating as a team to deliver a
product with concrete requirements. The second is to strengthen your grasp of software engineering
principles, documentation, and modern tooling. You will continue writing unit tests with each sprint to
verify functionality.
Tasks
In Sprint 3 you will submit a combined Design Deliverables PDF and demo implemented functionality.
Design focuses on modeling and justifying newly added features; implementation focuses on Dashboard
charts and Group Savings (Savings Circles) with real-time updates.
• Design Deliverables (PDF):
o Design Class Diagram (DCD)
o Sequence Diagram (SD)
o SOLID Principles Writeup
o Design Pattern Evidence (Factory and Strategy)
• Implementation:
o Dashboard analytics with MPAndroidChart (two or more charts)
o Group Savings (create/join groups, invitations, progress, linking to budgets/expenses)
o MVVM + Firestore integration; real-time updates
Pre-Sprint Agreement
Create an itemized list of tasks to be completed by the team and assign them to individuals. This list should
be submitted as a document to the Pre-Sprint Agreement Canvas assignment for your section. This list
can contain additional requirements such as a completion date for the task or subtasks, if you so wish.
The most important element to this assignment is agreeing as a team to the work to be done by everyone.
We will only be checking this assignment for completion but may refer to it in the future if there are any
issues with team members not pulling their weight. Ensure that the work distribution is fair and agreed
upon by everyone on the team. Please also attach a screenshot of the list of tasks in the project
management tool of your choice (Jira, Trello, Notion, etc.). Please refer to Canvas for the due date of this
assignment.
Design Deliverables
Four design deliverables are required in this Sprint: a Design Class Diagram (DCD), Sequence Diagram
(SD), GRASP Principles Write-Up, and Design Pattern Evidence. These should be submitted as a single
combined PDF on Canvas by the due date (see course page for official date). For diagramming, you
MUST use draw.io. If you don’t, it’s a zero on that portion of the assignment.
1) Design Class Diagram (DCD)
Model the extended SpendWise system incorporating Dashboard analytics and Savings Circles. Include:
• Classes, attributes (with types), operations where helpful, access modifiers.
• Associations with multiplicities; use Aggregation/Composition/Dependency where appropriate.
• New/updated classes for:
o Charts/Analytics data preparation (e.g., AnalyticsService, ChartData,
CategoryBreakdown, BudgetUsageSummary).
o Savings Circles (e.g., Group, Invitation, Challenge, Membership, Contribution).
o Storage/repository and ViewModels mapping to Firestore.
• Provide a typed list of classes & attributes used in your DCD. Clearly categorize newly
introduced nouns as classes vs attributes.
• Please submit a link to your draw.io diagram in the document.
2) Sequence Diagram (SD)
Create a new use case relevant to the Group Savings screen functionality and convert it to a SD. “User
accepts group invitation” would be one example (don’t use this one).
Requirements:
• Add a brief use-case description and a clear title for the SD.
• Include at least one ALT, LOOP, OPT, or Ref fragment (pick a scenario that merits
branching/iteration).
• Show dynamic interactions among user, ViewModels, repositories, and domain/storage classes.
• SD system calls must conceptually match the DCD.
• Disregard UI widgets; model calls as if the actor interacts directly with the system.
3) SOLID Principles Writeup
Provide distinct examples of SOLID applied in your implementation (screenshots + 4-5 sentence
explanations each). You only need ceil(team_size/2) examples.
4) Design Pattern Evidence (Factory and Strategy)
Demonstrate both of these in a context that improves modularity/usability:
• Factory: Centralize object creation for charts and repositories so Views don’t know which
concrete classes to build for a given user, date window, or data source.
• Strategy: pluggable sort/filter policies (e.g., sort expenses by date/amount/category; rank
groups by progress; choose chart aggregation strategies).
Submit screenshots of code implementing the pattern and a short paragraph (3–4 sentences) explaining
which classes participate and why the pattern improves your design.
Project Management
Please submit a screenshot of your project management tool when you have planned out the workload
for Sprint 3 in the pre-sprint agreement assignment on Canvas.
Implementation
In Sprint 3, you will deliver two new capabilities: (1) Dashboard Analytics with at least two
MPAndroidChart visualizations backed by Firestore and (2) Savings Circles (create/join, invitations,
realtime progress, and linkage to budgets/expenses). Maintain Java + MVVM separation (no Firestore
calls from Views), use observable streams (e.g., LiveData/Listeners) for realtime updates, and write unit
tests for the new business logic. Your demo should show charts updating live as budgets/expenses
change and group progress syncing across members.
*** The UI Images are there to help you showcase how your app can potentially look like. It is
not fully comprehensive, so make sure you read the document thoroughly. ***
U
A) Dashboard Analytics (MPAndroidChart)
Implement an analytics-driven Dashboard with at least two charts (examples below). Data must come
from Firestore under the logged in user. Handle null/empty datasets gracefully by showing
placeholder/dummy seeded data, so charts always render.
Required:
• Use MPAndroidChart for visuals.
• Maintain MVVM separation: all data prep in ViewModels/Repository; the View only
binds/observes.
• Charts must update in real time when the user adds/edits/deletes budgets or expenses.
Suggested charts (choose ≥2):
• Pie Chart – Expense breakdown by category (Food/Transport/...)
• Bar Chart – Budget usage (Spent vs Target) per budget
• Line Chart – Spending over time (last 30 days or current window)
• Stacked Bar – Category spend vs remaining in current budget window
Data Sources / Behavior:
• Expenses stored under users/{uid}/expenses
• Budgets stored under users/{uid}/budgets
• Respect the app’s current date selector (from Sprint 2) for weekly/monthly rollovers
• Validate/guard against empty collections; provide seeded records on first launch/login
B) Group Savings (Savings Circles)
Add full functionality to the Savings Circles module with Firestore backed data and realtime
synchronization.
1) Group Creation & Invitations
• Create new group with: group name, creator email, challenge title, goal amount, frequency
(weekly/monthly), optional notes.
• Only the group creator can send invitations (by email/username). Invitees can Accept (becomes
Active) or Decline (dismissed). The group creator should also be able to delete the challenge for
everyone.
• Validate inputs: no empty fields, non-negative amounts, valid frequency.
2) Challenge Duration & Calendar Integration
• When a user accepts an invite, their challenge period starts at acceptance time:
o Weekly: acceptance day = Day 1 of 7
o Monthly: acceptance day = Day 1 of the current month-long window
• Use the existing Dashboard date selector (testing current date) to drive period calculations.
3) Group Details & Progress Tracking
• Tapping a group shows: members, each member’s start/end dates, and contributions toward
the shared goal.
• Goals auto-mark complete if satisfied within the active window.
• Progress updates must sync in real time to all group members.
4) Integration with Budgets & Expenses
• Group-linked goals should appear alongside personal budgets (with a distinct visual indicator).
• In the Expense Creation form, allow attributing a spend to a group goal; this updates both the
group's progress and relevant charts.
5) Pattern Application
• Apply Factory(by centralizing object creation) and Strategy(by having ViewModel select behavior
at runtime).
C) Architecture & Data
• Continue Java + MVVM. Repository mediates all Firestore access. Avoid Firestore calls from
Views.
• Use LiveData/observable streams to propagate repository updates to Views.
• Keep code organized into model/, view/, viewmodel/, and repository/ (or equivalent) packages.
Testing Requirements
Your team will be required to create 2 unit tests per team member. Make sure you develop the project
with unit testing in mind, as it will help ensure robustness and catch issues early. Unit tests should focus
on the functionalities introduced in Sprint 3. For this Sprint, testing should cover the new Analytics and
Savings Circles features.
Provide 2 unit tests per team member targeting Sprint 3 functionality. Design for testability
(single-responsibility classes; logic in ViewModels/Repository). Examples:
• Given expenses linked to a budget/category, category totals and budget usage are computed
correctly for the current window.
• Charts’ data adapters omit future-dated expenses and correctly aggregate by category.
• Observer/Strategy behavior.
For instructions on setting up JUnit 4 and running test cases, please refer to the Sprint 2 document.
For further knowledge on Android testing, refer to the documentation.
Note: The UI of your team's game should be separate from the game's functionality/logic. These tests
should not be composed of UI mocking, but instead, your team's tests should be validating your game's
logic and event handling. If you create mock test cases, you will receive 0 credit for those tests.
Code Review
At the end of this sprint, your team will also be expected to complete the Code Review assignment. This
should not be any different than the Code Review submission for Sprint 1. Please ensure that you read
the directions for the Sprint 3 Code Review assignment carefully so that you don't run into any issues with
the grading.
Important: For pull requests created, the autograder will only count pull requests with at least 15 changes
(lines added + deleted) and at most 300 changes as valid PRs. Pull requests with fewer than 15 changes or
more than 300 changes will be marked as invalid and will not contribute to your score.
The quality of your pull requests is very important when you are a software engineer in industry, having
clear code and being able to explain why you made such changes with clear PR comments, commit
messages, and reviewing PRs properly. We will be reading these, and they need to show that you have a
proper understanding of what is being added to the codebase. This article goes into a little more detail.
CheckStyle
CheckStyle compliance is required for this Sprint. Refer to the setup details provided in Sprint 1 to ensure
proper code styling. Make sure your project code follows all specified style guidelines.
Sprint Tagging
Tags are a way of marking a specific commit and are typically used to mark new versions of software. To
do this, use "git tag" to list tags and "git tag –a tag name –m description" to create a tag on the current
commit. Important: To push tags, use "git push origin –tags". Pushing changes normally will not push
tags to GitHub. You will be asked to checkout this tagged commit during demo. This is done with "git fetch
--all --tags" and "git checkout tag name". You will be required to pull this tag during the demo. If you
forget to tag your commit or tag your commit late, you will be penalized with –5 points for each day
late.
Additional Resources
Under this section are some resources we think will help you and your team streamline the game creation
process. If you have any questions about these resources, contact your TAs through Ed or office hours.
1. TA website: https://github.gatech.edu/pages/gtobdes/obdes/
2. Ed: https://edstem.org/us/courses/81858/discussion/6876706
3. Android Studio Tutorial Videos (located on canvas --> media gallery).
Demos
Your implementation of sprint features will be graded through an in-person TA demo (Each team will
demo with their corresponding mentors). Demos will be held in CCB 267 the week after the Sprint is due.
Please sign up for a demo slot in Microsoft Bookings with your assigned mentor if possible. Note that not
all demo slots may be open at sprint release.
Summary
Grading for this Sprint will be based on the successful and correct completion of the design deliverables
and implementation requirements outlined above. A demo of implemented features is required for
teams to receive credit.
• Submission: Upload your design deliverables as a single combined PDF to Canvas. Include a link
to your GitHub repository in the submission, and ensure your repository is set to private.
• Demo Preparation: Add the shared TA GitHub account to your repository and tag the commit you
plan to demo. Points may be deducted if these guidelines are not followed
Academic Integrity
Note on Plagiarism and the Use of AI Tools
We understand that technology and tools, including Artificial Intelligence platforms, have become
readily accessible and can be valuable in various scenarios. However, students must be cautious about
their usage in academic settings. Please refer to the course policy regarding the use of AI tools for your
projects. Using AI to generate or copy content for your project is not considered collaboration.
Leveraging AI to produce work that you present as your own, without proper citation, is likely crossing
the line into academic misconduct. It's essential to maintain integrity in all academic undertakings. If in
doubt, always consult the course guidelines or reach out to the instructor on Ed Discussion for
clarification. If you are violating academic integrity policies, you WILL be caught and reported to OSI,
which could result in serious consequences.
